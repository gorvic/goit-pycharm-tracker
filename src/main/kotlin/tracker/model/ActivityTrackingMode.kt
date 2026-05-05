package tracker.model

enum class ActivityTrackingMode {
    HARD,
    SOFT;

    companion object {
        fun from(value: String?): ActivityTrackingMode {
            val normalized = value?.trim()?.uppercase() ?: ""
            return entries.firstOrNull { it.name == normalized } ?: HARD
        }
    }
}
