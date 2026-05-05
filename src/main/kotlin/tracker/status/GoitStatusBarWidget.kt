package tracker.status

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import tracker.tracking.ProjectTrackingService
import java.awt.event.MouseEvent
import com.intellij.util.Consumer
import javax.swing.Icon

class GoitStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.IconPresentation {

    companion object {
        const val WIDGET_ID: String = "GoitTrackerWidget"
    }

    private var statusBar: StatusBar? = null

    override fun ID(): String = WIDGET_ID

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {
        statusBar = null
    }

    override fun getPresentation(type: StatusBarWidget.PlatformType): StatusBarWidget.WidgetPresentation = this

    override fun getIcon(): Icon {
        val service = project.getService(ProjectTrackingService::class.java)
        return if (service.isEnabled()) GoitIcons.ON else GoitIcons.OFF
    }

    override fun getTooltipText(): String {
        val service = project.getService(ProjectTrackingService::class.java)
        return if (service.isEnabled()) {
            "GoIT Tracker: enabled. Click to disable"
        } else {
            "GoIT Tracker: disabled. Click to enable"
        }
    }

    override fun getClickConsumer(): Consumer<MouseEvent>? {
        return Consumer {
            project.getService(ProjectTrackingService::class.java).toggle()
            statusBar?.updateWidget(WIDGET_ID)
        }
    }
}
