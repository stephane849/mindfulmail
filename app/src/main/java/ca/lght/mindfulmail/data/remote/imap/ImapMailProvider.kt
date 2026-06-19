package ca.lght.mindfulmail.data.remote.imap

import ca.lght.mindfulmail.domain.model.Account
import ca.lght.mindfulmail.domain.model.AccountType
import ca.lght.mindfulmail.domain.model.Attachment
import ca.lght.mindfulmail.domain.model.Conversation
import ca.lght.mindfulmail.domain.model.EmailAddress
import ca.lght.mindfulmail.domain.model.Label
import ca.lght.mindfulmail.domain.model.LabelType
import ca.lght.mindfulmail.domain.model.Message
import ca.lght.mindfulmail.domain.provider.MailCredentials
import ca.lght.mindfulmail.domain.provider.MailProvider
import ca.lght.mindfulmail.domain.provider.MessageDraft
import ca.lght.mindfulmail.domain.provider.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.inject.Inject
import javax.mail.Authenticator
import javax.mail.FetchProfile
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Store
import javax.mail.Transport
import javax.mail.UIDFolder
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class ImapMailProvider @Inject constructor() : MailProvider {

    private val mutex = Mutex()
    private var store: Store? = null
    private var savedCredentials: MailCredentials.ImapCredentials? = null

    companion object {
        private const val PAGE_SIZE = 25

        private val SYSTEM_FOLDER_NAMES = setOf(
            "INBOX",
            "SENT", "SENT MESSAGES", "[GMAIL]/SENT MAIL",
            "TRASH", "[GMAIL]/TRASH",
            "DRAFTS", "[GMAIL]/DRAFTS",
            "SPAM", "JUNK", "[GMAIL]/SPAM",
            "ARCHIVE", "[GMAIL]/ALL MAIL",
        )
    }

    override suspend fun login(credentials: MailCredentials): Result<Account> {
        if (credentials !is MailCredentials.ImapCredentials) {
            return Result.failure(IllegalArgumentException("Expected ImapCredentials"))
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                val newStore = buildStore(credentials)
                mutex.withLock {
                    store?.runCatching { close() }
                    store = newStore
                    savedCredentials = credentials
                }
                Account(
                    id = credentials.email,
                    displayName = credentials.email,
                    email = credentials.email,
                    type = AccountType.IMAP,
                )
            }
        }
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        mutex.withLock {
            store?.runCatching { close() }
            store = null
            savedCredentials = null
        }
    }

    override suspend fun isAuthenticated(): Boolean =
        mutex.withLock { store?.isConnected == true }

    override suspend fun getLabels(): Result<List<Label>> = withContext(Dispatchers.IO) {
        runCatching {
            val s = requireStore()
            val folders = s.defaultFolder.list("*")
            folders
                .filter { it.type and Folder.HOLDS_MESSAGES != 0 }
                .map { folder ->
                    val unread = try { folder.unreadMessageCount } catch (_: MessagingException) { 0 }
                    Label(
                        id = folder.fullName,
                        name = folder.name,
                        type = if (folder.fullName.uppercase() in SYSTEM_FOLDER_NAMES ||
                            folder.name.uppercase() == "INBOX")
                            LabelType.SYSTEM else LabelType.FOLDER,
                        unreadCount = unread,
                    )
                }
        }
    }

    override suspend fun getConversations(labelId: String, page: Int): Result<List<Conversation>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val s = requireStore()
                val folder = s.getFolder(labelId)
                folder.open(Folder.READ_ONLY)
                try {
                    val total = folder.messageCount
                    if (total == 0) return@runCatching emptyList()
                    val end = total - page * PAGE_SIZE
                    val start = maxOf(1, end - PAGE_SIZE + 1)
                    if (start > end) return@runCatching emptyList()

                    val msgs = folder.getMessages(start, end)
                    val fp = FetchProfile().apply {
                        add(FetchProfile.Item.ENVELOPE)
                        add(FetchProfile.Item.FLAGS)
                        add(UIDFolder.FetchProfileItem.UID)
                    }
                    folder.fetch(msgs, fp)

                    msgs.reversed().map { msg ->
                        val uid = (folder as? UIDFolder)?.getUID(msg)
                            ?: msg.messageNumber.toLong()
                        val id = "${folder.fullName}:$uid"
                        val senders = msg.from
                            ?.filterIsInstance<InternetAddress>()
                            ?.map { EmailAddress(name = it.personal, address = it.address) }
                            ?: emptyList()
                        val isRead = msg.isSet(Flags.Flag.SEEN)
                        Conversation(
                            id = id,
                            subject = msg.subject ?: "(no subject)",
                            senders = senders,
                            numMessages = 1,
                            numUnread = if (isRead) 0 else 1,
                            latestTimestamp = msg.sentDate?.time ?: msg.receivedDate?.time ?: 0L,
                            labelIds = listOf(folder.fullName),
                            isStarred = msg.isSet(Flags.Flag.FLAGGED),
                        )
                    }
                } finally {
                    folder.close(false)
                }
            }
        }

    override suspend fun getMessage(id: String): Result<Message> = withContext(Dispatchers.IO) {
        runCatching {
            val (folderName, uid) = parseId(id)
            val s = requireStore()
            val folder = s.getFolder(folderName)
            folder.open(Folder.READ_ONLY)
            try {
                val uidFolder = folder as UIDFolder
                val msg = uidFolder.getMessageByUID(uid)
                    ?: throw NoSuchElementException("Message not found: $id")
                msg.toMessage(folder.fullName, uid)
            } finally {
                folder.close(false)
            }
        }
    }

    override suspend fun getMessagesInConversation(conversationId: String): Result<List<Message>> =
        getMessage(conversationId).map { listOf(it) }

    override suspend fun markAsRead(messageIds: List<String>): Result<Unit> =
        setFlag(messageIds, Flags.Flag.SEEN, true)

    override suspend fun markAsUnread(messageIds: List<String>): Result<Unit> =
        setFlag(messageIds, Flags.Flag.SEEN, false)

    override suspend fun moveToLabel(messageIds: List<String>, labelId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val s = requireStore()
                val dest = s.getFolder(labelId)
                messageIds.groupBy { parseId(it).first }.forEach { (folderName, ids) ->
                    val folder = s.getFolder(folderName)
                    folder.open(Folder.READ_WRITE)
                    try {
                        val uids = ids.map { parseId(it).second }.toLongArray()
                        val msgs = (folder as UIDFolder).getMessagesByUID(uids)
                        folder.copyMessages(msgs, dest)
                        folder.setFlags(msgs, Flags(Flags.Flag.DELETED), true)
                    } finally {
                        folder.close(true)
                    }
                }
            }
        }

    override suspend fun deleteMessages(messageIds: List<String>): Result<Unit> =
        setFlag(messageIds, Flags.Flag.DELETED, true)

    override suspend fun sendMessage(draft: MessageDraft): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val creds = savedCredentials ?: throw IllegalStateException("Not authenticated")
            val useSSL = creds.smtpPort == 465
            val props = Properties().apply {
                put("mail.smtp.host", creds.smtpHost)
                put("mail.smtp.port", creds.smtpPort.toString())
                put("mail.smtp.auth", "true")
                if (useSSL) {
                    put("mail.smtp.ssl.enable", "true")
                } else {
                    put("mail.smtp.starttls.enable", "true")
                }
                put("mail.smtp.connectiontimeout", "15000")
                put("mail.smtp.timeout", "30000")
            }
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(creds.email, creds.password)
            })
            val msg = MimeMessage(session).apply {
                setFrom(InternetAddress(creds.email))
                draft.to.forEach { addRecipient(javax.mail.Message.RecipientType.TO, InternetAddress(it.address, it.name ?: "")) }
                draft.cc.forEach { addRecipient(javax.mail.Message.RecipientType.CC, InternetAddress(it.address, it.name ?: "")) }
                draft.bcc.forEach { addRecipient(javax.mail.Message.RecipientType.BCC, InternetAddress(it.address, it.name ?: "")) }
                subject = draft.subject
                setText(draft.body, "utf-8")
            }
            Transport.send(msg)
        }
    }

    override suspend fun sync(): Result<SyncResult> =
        Result.success(SyncResult(newMessages = 0, updatedMessages = 0))

    // --- Helpers ---

    private fun buildStore(creds: MailCredentials.ImapCredentials): Store {
        val proto = if (creds.useSSL) "imaps" else "imap"
        val props = Properties().apply {
            put("mail.$proto.host", creds.imapHost)
            put("mail.$proto.port", creds.imapPort.toString())
            put("mail.$proto.ssl.enable", creds.useSSL.toString())
            put("mail.$proto.connectiontimeout", "15000")
            put("mail.$proto.timeout", "30000")
            // Trust all certs — fine for a dev build; replace with pinning for production
            put("mail.$proto.ssl.trust", "*")
        }
        val s = Session.getInstance(props)
        val store = s.getStore(proto)
        store.connect(creds.imapHost, creds.imapPort, creds.email, creds.password)
        return store
    }

    private suspend fun requireStore(): Store = mutex.withLock {
        store?.takeIf { it.isConnected }
            ?: throw IllegalStateException("Not connected — call login() first")
    }

    private fun parseId(id: String): Pair<String, Long> {
        val idx = id.lastIndexOf(':')
        require(idx > 0) { "Invalid message ID: $id" }
        return id.substring(0, idx) to id.substring(idx + 1).toLong()
    }

    private suspend fun setFlag(ids: List<String>, flag: Flags.Flag, value: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val s = requireStore()
                ids.groupBy { parseId(it).first }.forEach { (folderName, groupIds) ->
                    val folder = s.getFolder(folderName)
                    folder.open(Folder.READ_WRITE)
                    try {
                        val uids = groupIds.map { parseId(it).second }.toLongArray()
                        val msgs = (folder as UIDFolder).getMessagesByUID(uids)
                        folder.setFlags(msgs, Flags(flag), value)
                    } finally {
                        folder.close(false)
                    }
                }
            }
        }
}

private fun javax.mail.Message.toMessage(folderName: String, uid: Long): Message {
    val id = "$folderName:$uid"

    fun addresses(type: javax.mail.Message.RecipientType): List<EmailAddress> = try {
        (getRecipients(type) ?: emptyArray())
            .filterIsInstance<InternetAddress>()
            .map { EmailAddress(name = it.personal, address = it.address) }
    } catch (_: MessagingException) { emptyList() }

    val sender = from
        ?.filterIsInstance<InternetAddress>()
        ?.map { EmailAddress(name = it.personal, address = it.address) }
        ?.firstOrNull()
        ?: EmailAddress(name = null, address = "unknown")

    return Message(
        id = id,
        conversationId = id,
        subject = subject ?: "(no subject)",
        sender = sender,
        recipients = addresses(javax.mail.Message.RecipientType.TO),
        ccRecipients = addresses(javax.mail.Message.RecipientType.CC),
        bccRecipients = addresses(javax.mail.Message.RecipientType.BCC),
        body = extractTextBody(this),
        bodyMimeType = "text/plain",
        timestamp = sentDate?.time ?: receivedDate?.time ?: 0L,
        isRead = isSet(Flags.Flag.SEEN),
        isStarred = isSet(Flags.Flag.FLAGGED),
        labelIds = listOf(folderName),
        attachments = extractAttachments(this),
        isEncrypted = false,
    )
}

private fun extractTextBody(part: Part): String {
    return try {
        when {
            part.isMimeType("text/plain") -> part.content as? String ?: ""
            part.isMimeType("multipart/*") -> {
                val mp = part.content as Multipart
                var plain = ""
                var html = ""
                for (i in 0 until mp.count) {
                    val bp = mp.getBodyPart(i)
                    when {
                        bp.isMimeType("text/plain") && plain.isEmpty() ->
                            plain = bp.content as? String ?: ""
                        bp.isMimeType("text/html") && html.isEmpty() ->
                            html = bp.content as? String ?: ""
                        bp.isMimeType("multipart/*") -> {
                            val nested = extractTextBody(bp)
                            if (nested.isNotEmpty() && plain.isEmpty()) plain = nested
                        }
                    }
                }
                plain.ifEmpty { html }
            }
            else -> ""
        }
    } catch (_: Exception) { "" }
}

private fun extractAttachments(part: Part): List<Attachment> {
    return try {
        if (!part.isMimeType("multipart/*")) return emptyList()
        val mp = part.content as Multipart
        val result = mutableListOf<Attachment>()
        for (i in 0 until mp.count) {
            val bp = mp.getBodyPart(i)
            if (Part.ATTACHMENT.equals(bp.disposition, ignoreCase = true) ||
                bp.fileName != null) {
                result += Attachment(
                    id = "$i",
                    name = bp.fileName ?: "attachment-$i",
                    mimeType = bp.contentType.substringBefore(';').trim(),
                    size = bp.size.toLong(),
                    isEncrypted = false,
                )
            }
        }
        result
    } catch (_: Exception) { emptyList() }
}
