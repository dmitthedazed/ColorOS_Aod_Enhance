package com.op.aod.enhance.data

/**
 * Config data class for the UI side.
 * Mirror relationship: hook/AodConfig.kt holds the Hook-side mirror with the same fields.
 * When adding/changing a field, also update: AodConfig (hook side) and AodConfigContract.
 */
data class AodUiConfig(
    val initDark: Int = AodConfigContract.DEFAULT_INIT_DARK,
    val initBright: Int = AodConfigContract.DEFAULT_INIT_BRIGHT,
    val runningMultiplier: Float = AodConfigContract.DEFAULT_RUNNING_MULTIPLIER,
    val enablePanoramic: Boolean = AodConfigContract.DEFAULT_ENABLE_PANORAMIC,
    val enableSettingsSupport: Boolean = AodConfigContract.DEFAULT_ENABLE_SETTINGS_SUPPORT,
    val blockSingleClick: Boolean = AodConfigContract.DEFAULT_BLOCK_SINGLE_CLICK,
)
