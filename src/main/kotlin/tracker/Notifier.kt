package tracker

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object Notifier {
    private const val GROUP_ID = "GoIT Tracker"

    fun error(project: Project?, title: String, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(title, message, NotificationType.ERROR)
            .notify(project)
    }

    fun info(project: Project?, title: String, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(title, message, NotificationType.INFORMATION)
            .notify(project)
    }
}
