package tracker.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import tracker.status.GoitIcons
import tracker.tracking.ProjectTrackingService

class ToggleTrackingAction : ToggleAction() {
    override fun isSelected(e: AnActionEvent): Boolean {
        return e.project?.getService(ProjectTrackingService::class.java)?.isEnabled() ?: false
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val service = e.project?.getService(ProjectTrackingService::class.java) ?: return
        if (state) {
            service.enable()
        } else {
            service.disableAndFlush()
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val service = e.project?.getService(ProjectTrackingService::class.java)
        val enabled = service?.isEnabled() == true
        e.presentation.isEnabled = service != null
        e.presentation.icon = if (enabled) GoitIcons.ON else GoitIcons.OFF
        e.presentation.text = if (enabled) "Disable GoIT Tracking" else "Enable GoIT Tracking"
        e.presentation.description = if (enabled) {
            "Disable GoIT tracking for this project"
        } else {
            "Enable GoIT tracking for this project"
        }
    }
}
