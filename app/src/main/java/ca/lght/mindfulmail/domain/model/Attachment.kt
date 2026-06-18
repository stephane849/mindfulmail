package ca.lght.mindfulmail.domain.model

data class Attachment(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val isEncrypted: Boolean = false,
)
