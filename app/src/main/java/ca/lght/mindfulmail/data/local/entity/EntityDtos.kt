package ca.lght.mindfulmail.data.local.entity

import ca.lght.mindfulmail.domain.model.Attachment
import ca.lght.mindfulmail.domain.model.EmailAddress
import kotlinx.serialization.Serializable

/**
 * Serializable DTOs shared by the Room entities for JSON-encoded columns.
 * Declared once here to avoid per-file redeclarations within the package.
 */
@Serializable
internal data class EmailAddressDto(val name: String?, val address: String)

@Serializable
internal data class AttachmentDto(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val isEncrypted: Boolean,
)

internal fun EmailAddress.toDto() = EmailAddressDto(name, address)
internal fun EmailAddressDto.toDomain() = EmailAddress(name, address)
internal fun Attachment.toDto() = AttachmentDto(id, name, mimeType, size, isEncrypted)
internal fun AttachmentDto.toDomain() = Attachment(id, name, mimeType, size, isEncrypted)
