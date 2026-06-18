package ca.lght.mindfulmail.domain.model

data class Label(
    val id: String,
    val name: String,
    val type: LabelType,
    val unreadCount: Int = 0,
)

enum class LabelType { FOLDER, LABEL, SYSTEM }
