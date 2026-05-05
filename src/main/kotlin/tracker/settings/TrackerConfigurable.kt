package tracker.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import tracker.api.ApiClient
import tracker.api.ApiSendResult
import tracker.api.EventQueueService
import tracker.api.TrackerEvent
import tracker.stats.StatsCalculator
import tracker.stats.StatsDialog
import tracker.status.GoitStatusBarWidget
import tracker.tools.LogOpener
//import tracker.tools.UpdateChecker
import tracker.tools.VersionUtil
import tracker.tools.VsixImporter
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.time.Instant
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingUtilities

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import tracker.Notifier

class TrackerConfigurable : Configurable {

    private val service: TrackerSettingsService = ApplicationManager.getApplication()
        .getService(TrackerSettingsService::class.java)
    private val queueService: EventQueueService = ApplicationManager.getApplication()
        .getService(EventQueueService::class.java)
    private val apiClient: ApiClient = ApplicationManager.getApplication()
        .getService(ApiClient::class.java)

    private val userTokenField = JTextField()
    //private val eventField = JTextField()
    private val endpointField = JTextField()
    private val fileExtensionsField = JTextField()
    private val updateInfoUrlField = JTextField()

    private val disableStatusBarButton = JCheckBox("Disable status bar button")
    private val activateOnStartupButton = JCheckBox("Activate on startup")
    private val tickSecondsField = JTextField()
    private val idleTimeoutSecondsField = JTextField()
    private val flushIntervalSecondsField = JTextField()

    private val activityTrackingModeBox = JComboBox(arrayOf("Hard key press activity", "Soft activity"))

    private val logModeBox = JComboBox(arrayOf("Normal", "Errors only", "Debug"))
    private val maxLogFileSizeMbField = JTextField()
    private val customDataDirField = JTextField()

    private val currentVersionField = JTextField()
    private val latestVersionField = JTextField()
    private val pendingEventsField = JTextField()
    private val totalTimeField = JTextField()
    private val todayTimeField = JTextField()

    //private val updateButton = JButton("Update plugin")
    //private val checkUpdatesButton = JButton("Check for updates")
    private val importButton = JButton("Import from VSCode plugin...")
    private val showLogsButton = JButton("Show logs")
    private val openSessionsButton = JButton("Open sessions log")
    private val clearLogsButton = JButton("Clear logs")
    private val clearQueueButton = JButton("Clear queue")
    private val testApiButton = JButton("Test API connection")
    private val chooseDataDirButton = JButton("Choose...")
    private val resetDefaultsButton = JButton("Reset settings to defaults")
    private val detailedStatsButton = JButton("Detailed statistics")

    private val updateNotes = JTextArea(3, 40)
    private var latestDownloadUrl = ""
    private var importedSettings: TrackerSettings? = null
    private var panel: JPanel? = null

    override fun getDisplayName(): String = "GoIT PyCharm Tracker"

    override fun createComponent(): JComponent {
        val result = JPanel(GridBagLayout())
        panel = result

        //eventField.isEditable = false
        currentVersionField.isEditable = false
        latestVersionField.isEditable = false
        pendingEventsField.isEditable = false
        totalTimeField.isEditable = false
        todayTimeField.isEditable = false
        updateNotes.isEditable = false
        updateNotes.lineWrap = true
        updateNotes.wrapStyleWord = true
        //updateButton.isVisible = false

        var row = 0

        addSection(result, row++, "Connection")
        addRow(result, row++, "User token / UID:", userTokenField)
        //addRow(result, row++, "Event:", eventField)
        addRow(result, row++, "Endpoint:", endpointField)
        addRow(result, row++, "File extensions:", fileExtensionsField)
        addConnectionButtons(result, row++)

        addSection(result, row++, "Tracking")
        addFullRow(result, row++, activateOnStartupButton)
        addFullRow(result, row++, disableStatusBarButton)
        addRow(result, row++, "Tick seconds:", tickSecondsField)
        addRow(result, row++, "Idle timeout seconds:", idleTimeoutSecondsField)
        addRow(result, row++, "Flush interval seconds:", flushIntervalSecondsField)
        addRow(result, row++, "Activity tracking mode:", activityTrackingModeBox)


        addSection(result, row++, "Logs")
        addRow(result, row++, "Logging mode:", logModeBox)
        addRow(result, row++, "Max log file size, MB:", maxLogFileSizeMbField)
        addCustomDataDirRow(result, row++)
        addLogButtons(result, row++)

        addSection(result, row++, "Statistics")
        addRow(result, row++, "Total tracked time:", totalTimeField)
        addRow(result, row++, "Today:", todayTimeField)
        addRow(result, row++, "Pending events:", pendingEventsField)
        addStatsButtons(result, row++)

        /*
        addSection(result, row++, "Updates")
        addRow(result, row++, "Update info URL:", updateInfoUrlField)
        addRow(result, row++, "Current version:", currentVersionField)
        addRow(result, row++, "Latest version:", latestVersionField)
        addRow(result, row++, "Update notes:", updateNotes)
        addUpdateButtons(result, row++)
        */

        importButton.addActionListener { importFromVsix() }
        //checkUpdatesButton.addActionListener { checkForUpdates() }
        //updateButton.addActionListener { openUpdatePage() }
        showLogsButton.addActionListener { LogOpener.openTrackerLog() }
        openSessionsButton.addActionListener { LogOpener.openSessionsLog() }
        clearLogsButton.addActionListener { clearLogs() }
        clearQueueButton.addActionListener { clearQueue() }
        testApiButton.addActionListener { testApiConnection() }
        chooseDataDirButton.addActionListener { chooseDataDir() }
        resetDefaultsButton.addActionListener { resetToDefaults() }
        detailedStatsButton.addActionListener { StatsDialog(null).show() }

        reset()
        return result
    }

    override fun isModified(): Boolean {
        val current = service.get()
        return importedSettings != null ||
            userTokenField.text != current.userToken ||
            //eventField.text != current.event ||
            endpointField.text != current.endpoint ||
            fileExtensionsField.text != current.fileExtensions ||
            updateInfoUrlField.text != current.updateInfoUrl ||
            disableStatusBarButton.isSelected != current.disableStatusBarButton ||
            activateOnStartupButton.isSelected != current.activateOnStartup ||
            tickSecondsField.text != current.tickSeconds.toString() ||
            idleTimeoutSecondsField.text != current.idleTimeoutSeconds.toString() ||
            flushIntervalSecondsField.text != current.flushIntervalSeconds.toString() ||
            activityTrackingModeBox.selectedItem?.toString() != activityModeLabel(current.activityTrackingMode) ||
            logModeBox.selectedItem?.toString() != current.logMode ||
            maxLogFileSizeMbField.text != current.maxLogFileSizeMb.toString() ||
            customDataDirField.text != current.customDataDir
    }

    override fun apply() {
        val updated = service.get().copy()
        updated.userToken = userTokenField.text.trim()
        //updated.event = eventField.text.trim()
        updated.endpoint = endpointField.text.trim()
        updated.fileExtensions = fileExtensionsField.text.trim()
        updated.updateInfoUrl = updateInfoUrlField.text.trim()
        updated.disableStatusBarButton = disableStatusBarButton.isSelected
        updated.activateOnStartup = activateOnStartupButton.isSelected
        updated.tickSeconds = tickSecondsField.text.trim().toIntOrNull() ?: updated.tickSeconds
        updated.idleTimeoutSeconds = idleTimeoutSecondsField.text.trim().toIntOrNull() ?: updated.idleTimeoutSeconds
        updated.flushIntervalSeconds = flushIntervalSecondsField.text.trim().toIntOrNull() ?: updated.flushIntervalSeconds
        updated.activityTrackingMode = activityModeValue(activityTrackingModeBox.selectedItem?.toString())
        updated.logMode = logModeBox.selectedItem?.toString() ?: "Normal"
        updated.maxLogFileSizeMb = maxLogFileSizeMbField.text.trim().toIntOrNull() ?: updated.maxLogFileSizeMb
        updated.customDataDir = customDataDirField.text.trim()

        val imported = importedSettings
        if (imported != null) {
            if (imported.eventType.isNotBlank()) updated.eventType = imported.eventType
            //if (imported.event.isNotBlank()) updated.event = imported.event
        }

        service.update(updated)
        importedSettings = null
        refreshRuntimeInfo()
        refreshStatusBarWidgets(updated)
    }

    override fun reset() {
        val current = service.get()
        userTokenField.text = current.userToken
        //eventField.text = current.event
        endpointField.text = current.endpoint
        fileExtensionsField.text = current.fileExtensions
        updateInfoUrlField.text = current.updateInfoUrl
        disableStatusBarButton.isSelected = current.disableStatusBarButton
        activateOnStartupButton.isSelected = current.activateOnStartup
        tickSecondsField.text = current.tickSeconds.toString()
        idleTimeoutSecondsField.text = current.idleTimeoutSeconds.toString()
        flushIntervalSecondsField.text = current.flushIntervalSeconds.toString()
        activityTrackingModeBox.selectedItem = activityModeLabel(current.activityTrackingMode)
        logModeBox.selectedItem = current.logMode.ifBlank { "Normal" }
        maxLogFileSizeMbField.text = current.maxLogFileSizeMb.toString()
        customDataDirField.text = current.customDataDir
        /*
        currentVersionField.text = VersionUtil.currentVersion()
        latestVersionField.text = "Not checked"
        updateNotes.text = ""
        latestDownloadUrl = ""
        updateButton.isVisible = false
         */
        importedSettings = null
        refreshRuntimeInfo()
    }

    private fun importFromVsix() {
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle("Select GoIT VSCode plugin (.vsix)")
            .withDescription("Select a VSCode extension package to extract userToken and compatible defaults")

        val selected = FileChooser.chooseFile(descriptor, null, null) ?: return

        val ext = selected.extension?.lowercase()

        if (ext != "vsix" && ext != "zip") {
            Notifier.error(
                null,
                "Invalid file type",
                "Please select a .vsix file."
            )
            return
        }

        val imported = VsixImporter.importFrom(java.io.File(selected.path))

        if (imported == null) {
            Messages.showErrorDialog("Cannot read package.json from selected VSCode plugin file.", "GoIT Tracker Import")
            return
        }

        importedSettings = imported
        //if (imported.event.isNotBlank()) eventField.text = imported.event
        if (imported.userToken.isNotBlank()) userTokenField.text = imported.userToken
        if (imported.endpoint.isNotBlank()) endpointField.text = imported.endpoint
        if (imported.fileExtensions.isNotBlank()) fileExtensionsField.text = imported.fileExtensions
        Messages.showInfoMessage("VSCode plugin values were imported into the form. Press Apply to save them.", "GoIT Tracker Import")
    }

    /*private fun checkForUpdates() {
        val updateInfoUrl = updateInfoUrlField.text.trim()
        if (updateInfoUrl.isBlank()) {
            Messages.showErrorDialog("Update info URL is empty.", "GoIT Tracker Updates")
            return
        }

        latestVersionField.text = "Checking..."
        updateButton.isVisible = false
        latestDownloadUrl = ""

        ApplicationManager.getApplication().executeOnPooledThread {
            val checker = UpdateChecker(service.get().requestTimeoutMs.coerceAtLeast(5000))
            val info = checker.check(updateInfoUrl)

            SwingUtilities.invokeLater {
                if (info == null || info.version.isBlank()) {
                    latestVersionField.text = "Unavailable"
                    updateNotes.text = "Cannot check latest version."
                } else {
                    latestVersionField.text = info.version
                    updateNotes.text = info.changelog
                    latestDownloadUrl = info.downloadUrl
                    updateButton.isVisible = VersionUtil.isNewer(info.version, VersionUtil.currentVersion()) && info.downloadUrl.isNotBlank()
                    if (!updateButton.isVisible) {
                        Messages.showInfoMessage("You are using the latest available version.", "GoIT Tracker Updates")
                    }
                }
            }
        }
    }

    private fun openUpdatePage() {
        val url = latestDownloadUrl
        if (url.isBlank()) return
        ApplicationManager.getApplication().executeOnPooledThread {
            UpdateChecker(service.get().requestTimeoutMs).openDownloadPage(url)
        }
    }*/

    private fun clearLogs() {
        LogOpener.clearLogs()
        Messages.showInfoMessage("Log file was cleared.", "GoIT Tracker Logs")
    }

    private fun clearQueue() {
        if (Messages.showYesNoDialog("Clear all pending tracking events?", "GoIT Tracker Queue", Messages.getQuestionIcon()) == Messages.YES) {
            queueService.clearQueue()
            refreshRuntimeInfo()
        }
    }

    private fun testApiConnection() {
        val temporary = service.get().copy()
        temporary.userToken = userTokenField.text.trim()
        //temporary.event = eventField.text.trim()
        temporary.endpoint = endpointField.text.trim()
        service.update(temporary)

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = apiClient.send(TrackerEvent(Instant.now(), 1))
            SwingUtilities.invokeLater {
                when (result) {
                    ApiSendResult.Success -> Messages.showInfoMessage("API connection test succeeded.", "GoIT Tracker API")
                    ApiSendResult.InvalidCredentials -> Messages.showErrorDialog("Invalid user token. Update User token / UID in settings.", "GoIT Tracker API")
                    ApiSendResult.RetryLater -> Messages.showErrorDialog("API connection test failed. Check endpoint or internet connection.", "GoIT Tracker API")
                }
            }
        }
    }

    private fun chooseDataDir() {
        val descriptor = FileChooserDescriptor(
            false,
            true,
            false,
            false,
            false,
            false
        ).withTitle("Select GoIT Tracker data directory")
        val selected = FileChooser.chooseFile(descriptor, null, null) ?: return
        customDataDirField.text = selected.path
    }

    private fun resetToDefaults() {
        if (Messages.showYesNoDialog("Reset settings to defaults? User token will be kept.", "GoIT Tracker Settings", Messages.getQuestionIcon()) != Messages.YES) {
            return
        }
        val currentToken = userTokenField.text.trim()
        val defaults = TrackerConfigParser.parse(javaClass.classLoader.getResourceAsStream("default-config.json")?.bufferedReader()?.use { it.readText() } ?: "")
        userTokenField.text = currentToken
        //eventField.text = defaults.event
        endpointField.text = defaults.endpoint
        fileExtensionsField.text = defaults.fileExtensions
        updateInfoUrlField.text = defaults.updateInfoUrl
        disableStatusBarButton.isSelected = defaults.disableStatusBarButton
        activateOnStartupButton.isSelected = defaults.activateOnStartup
        tickSecondsField.text = defaults.tickSeconds.toString()
        idleTimeoutSecondsField.text = defaults.idleTimeoutSeconds.toString()
        flushIntervalSecondsField.text = defaults.flushIntervalSeconds.toString()
        activityTrackingModeBox.selectedItem = activityModeLabel(defaults.activityTrackingMode)
        logModeBox.selectedItem = defaults.logMode.ifBlank { "Normal" }
        maxLogFileSizeMbField.text = defaults.maxLogFileSizeMb.toString()
        customDataDirField.text = defaults.customDataDir
    }

    private fun activityModeLabel(value: String): String {
        return if (value.trim().equals("SOFT", ignoreCase = true)) {
            "Soft activity"
        } else {
            "Hard key press activity"
        }
    }

    private fun activityModeValue(label: String?): String {
        return if (label == "Soft activity") "SOFT" else "HARD"
    }

    private fun refreshRuntimeInfo() {
        val stats = StatsCalculator().load()
        pendingEventsField.text = queueService.pendingCount().toString()
        totalTimeField.text = StatsCalculator.formatDuration(stats.totalSeconds)
        todayTimeField.text = StatsCalculator.formatDuration(stats.todaySeconds)
    }

    private fun addSection(panel: JPanel, row: Int, title: String) {
        val constraints = GridBagConstraints()
        constraints.gridx = 0
        constraints.gridy = row
        constraints.gridwidth = 2
        constraints.weightx = 1.0
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.insets.set(10, 4, 4, 4)
        panel.add(JLabel(title), constraints)
    }

    private fun addRow(panel: JPanel, row: Int, label: String, component: JComponent) {
        val labelConstraints = GridBagConstraints()
        labelConstraints.gridx = 0
        labelConstraints.gridy = row
        labelConstraints.anchor = GridBagConstraints.WEST
        labelConstraints.insets.set(4, 4, 4, 8)
        panel.add(JLabel(label), labelConstraints)

        val fieldConstraints = GridBagConstraints()
        fieldConstraints.gridx = 1
        fieldConstraints.gridy = row
        fieldConstraints.weightx = 1.0
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL
        fieldConstraints.insets.set(4, 4, 4, 4)
        panel.add(component, fieldConstraints)
    }

    private fun addFullRow(panel: JPanel, row: Int, component: JComponent) {
        val constraints = GridBagConstraints()
        constraints.gridx = 0
        constraints.gridy = row
        constraints.gridwidth = 2
        constraints.anchor = GridBagConstraints.WEST
        constraints.insets.set(4, 4, 4, 4)
        panel.add(component, constraints)
    }

    private fun addCustomDataDirRow(panel: JPanel, row: Int) {
        val wrapper = JPanel(GridBagLayout())
        val fieldConstraints = GridBagConstraints()
        fieldConstraints.gridx = 0
        fieldConstraints.gridy = 0
        fieldConstraints.weightx = 1.0
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL
        wrapper.add(customDataDirField, fieldConstraints)
        val buttonConstraints = GridBagConstraints()
        buttonConstraints.gridx = 1
        buttonConstraints.gridy = 0
        buttonConstraints.insets.set(0, 4, 0, 0)
        wrapper.add(chooseDataDirButton, buttonConstraints)
        addRow(panel, row, "Custom data directory:", wrapper)
    }

    private fun addConnectionButtons(panel: JPanel, row: Int) {
        addButtonPanel(panel, row, listOf(importButton))
    }

    private fun addLogButtons(panel: JPanel, row: Int) {
        addButtonPanel(panel, row, listOf(showLogsButton, openSessionsButton, clearLogsButton))
    }

    private fun addStatsButtons(panel: JPanel, row: Int) {
        addButtonPanel(panel, row, listOf(detailedStatsButton, clearQueueButton, testApiButton, resetDefaultsButton))
    }

    /*
    private fun addUpdateButtons(panel: JPanel, row: Int) {
        addButtonPanel(panel, row, listOf(checkUpdatesButton, updateButton))
    }
    */

    private fun addButtonPanel(panel: JPanel, row: Int, buttons: List<JButton>) {
        val wrapper = JPanel(GridBagLayout())
        val constraints = GridBagConstraints()
        constraints.gridy = 0
        constraints.insets.set(2, 2, 2, 2)
        for ((index, button) in buttons.withIndex()) {
            constraints.gridx = index
            wrapper.add(button, constraints)
        }

        val panelConstraints = GridBagConstraints()
        panelConstraints.gridx = 0
        panelConstraints.gridy = row
        panelConstraints.gridwidth = 2
        panelConstraints.anchor = GridBagConstraints.WEST
        panelConstraints.insets.set(6, 4, 6, 4)
        panel.add(wrapper, panelConstraints)
    }

    private fun refreshStatusBarWidgets(settings: TrackerSettings) {
        ProjectManager.getInstance().openProjects.forEach { project ->
            val statusBar = WindowManager.getInstance().getStatusBar(project)

            statusBar?.removeWidget(GoitStatusBarWidget.WIDGET_ID)

            if (!settings.disableStatusBarButton && statusBar != null) {
                statusBar.addWidget(GoitStatusBarWidget(project), project)
            }
        }
    }
}
