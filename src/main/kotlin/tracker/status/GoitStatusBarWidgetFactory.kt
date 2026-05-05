package tracker.status

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

class GoitStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = GoitStatusBarWidget.WIDGET_ID

    override fun getDisplayName(): String = "GoIT Tracker"

    override fun isAvailable(project: Project): Boolean {
        return !com.intellij.openapi.application.ApplicationManager.getApplication()
            .getService(tracker.settings.TrackerSettingsService::class.java)
            .get()
            .disableStatusBarButton
    }

    override fun createWidget(project: Project): StatusBarWidget = GoitStatusBarWidget(project)

    override fun disposeWidget(widget: StatusBarWidget) {
        widget.dispose()
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}
