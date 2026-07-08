package net.aspw.client.visual.notification

import java.awt.Color

enum class NotificationType {
    ENABLE, DISABLE, INFO, WARNING, ERROR
}

class Notification(
    val title: String,
    val content: String,
    val type: NotificationType,
    val duration: Int = 2000
) {
    var x = 0f
    var y = 0f
    var currentX = 0f
    var currentY = 0f
    var opacity = 0f
    var time = System.currentTimeMillis()

    fun getTypeColor(): Color = when (type) {
        NotificationType.ENABLE -> Color(59, 130, 246)
        NotificationType.DISABLE -> Color(239, 68, 68)
        NotificationType.INFO -> Color(99, 102, 241)
        NotificationType.WARNING -> Color(245, 158, 11)
        NotificationType.ERROR -> Color(239, 68, 68)
    }

    fun getTypeIcon(): String = when (type) {
        NotificationType.ENABLE -> "+"
        NotificationType.DISABLE -> "-"
        NotificationType.INFO -> "!"
        NotificationType.WARNING -> "!"
        NotificationType.ERROR -> "x"
    }
}
