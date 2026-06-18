package ca.lght.mindfulmail.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ca.lght.mindfulmail.domain.model.Label
import ca.lght.mindfulmail.domain.model.LabelType

@Entity(tableName = "labels")
data class LabelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val unreadCount: Int,
)

fun LabelEntity.toLabel() = Label(
    id = id,
    name = name,
    type = LabelType.valueOf(type),
    unreadCount = unreadCount,
)

fun Label.toEntity() = LabelEntity(
    id = id,
    name = name,
    type = type.name,
    unreadCount = unreadCount,
)
