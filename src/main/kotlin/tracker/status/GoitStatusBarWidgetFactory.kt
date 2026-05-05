package tracker.status

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import tracker.settings.TrackerSettingsService

class GoitStatusBarWidgetFactory : StatusBarWidgetFactory {

    private val settingsService: TrackerSettingsService by lazy {
        ApplicationManager.getApplication()
            .getService(TrackerSettingsService::class.java)
    }

    override fun getId(): String = GoitStatusBarWidget.WIDGET_ID

    override fun getDisplayName(): String = "GoIT Tracker"

    override fun isAvailable(project: Project): Boolean = !settingsService.get().disableStatusBarButton

    override fun createWidget(project: Project): StatusBarWidget = GoitStatusBarWidget(project)

    override fun disposeWidget(widget: StatusBarWidget) {
        widget.dispose()
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}