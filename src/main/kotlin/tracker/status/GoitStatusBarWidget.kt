package tracker.status

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.util.Consumer
import tracker.tracking.ProjectTrackingService
import java.awt.event.MouseEvent
import javax.swing.Icon

class GoitStatusBarWidget(private val project: Project) :
    StatusBarWidget,
    StatusBarWidget.IconPresentation {

    companion object {
        const val WIDGET_ID: String = "GoitTrackerWidget"
    }

    private var statusBar: StatusBar? = null

    private val trackingService by lazy {
        project.getService(ProjectTrackingService::class.java)
    }

    private val isTrackingEnabled: Boolean
        get() = trackingService.isEnabled()

    override fun ID(): String = WIDGET_ID

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {
        statusBar = null
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun getPresentation(type: StatusBarWidget.PlatformType): StatusBarWidget.WidgetPresentation {
        return this
    }

    override fun getIcon(): Icon {
        return if (isTrackingEnabled) {
            GoitIcons.ON
        } else {
            GoitIcons.OFF
        }
    }

    override fun getTooltipText(): String {
        return if (isTrackingEnabled) {
            "GoIT Tracker: enabled. Click to disable"
        } else {
            "GoIT Tracker: disabled. Click to enable"
        }
    }

    override fun getClickConsumer(): Consumer<MouseEvent> {
        return Consumer {
            trackingService.toggle()
            statusBar?.updateWidget(WIDGET_ID)
        }
    }
}