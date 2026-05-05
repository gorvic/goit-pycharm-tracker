package tracker.stats

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JScrollPane
import javax.swing.JTextArea

class StatsDialog(project: Project?) : DialogWrapper(project) {
    init {
        title = "GoIT Tracker Detailed Statistics"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val stats = StatsCalculator().load()
        val text = buildString {
            appendLine("Total tracked time: ${StatsCalculator.formatDuration(stats.totalSeconds)}")
            appendLine("Today: ${StatsCalculator.formatDuration(stats.todaySeconds)}")
            appendLine()
            appendLine("By day:")
            if (stats.byDay.isEmpty()) {
                appendLine("  No sessions yet.")
            } else {
                stats.byDay.forEach { (day, seconds) ->
                    appendLine("  $day — ${StatsCalculator.formatDuration(seconds)}")
                }
            }
            appendLine()
            appendLine("By project:")
            if (stats.byProject.isEmpty()) {
                appendLine("  No sessions yet.")
            } else {
                stats.byProject.forEach { (project, seconds) ->
                    appendLine("  $project — ${StatsCalculator.formatDuration(seconds)}")
                }
            }
        }

        val area = JTextArea(text)
        area.isEditable = false
        area.lineWrap = false
        val scroll = JScrollPane(area)
        scroll.preferredSize = Dimension(560, 420)
        return scroll
    }
}
