package com.op.aod.enhance.data

import android.database.Cursor

/**
 * Config contract: defines the ContentProvider's key names and constants.
 *
 * This file is the single source of truth between the Provider, the UI side (AodUiConfig)
 * and the Hook side (AodConfig). When adding/changing a field, all three must be updated:
 * - AodUiConfig.kt (UI side)
 * - AodConfig.kt (Hook side)
 * - AodConfigContract.kt (key definitions)
 *
 * Note: hardcoded column indices are deprecated; both sides look up columns by name via [getColumnIndex].
 */
object AodConfigContract {

    // SharedPreferences key names (also used as Cursor column names)
    const val KEY_INIT_DARK = "init_brightness_dark"
    const val KEY_INIT_BRIGHT = "init_brightness_bright"
    const val KEY_RUNNING_MULTIPLIER = "running_brightness_multiplier"
    const val KEY_ENABLE_PANORAMIC = "enable_panoramic"
    const val KEY_ENABLE_SETTINGS_SUPPORT = "enable_settings_support"
    const val KEY_BLOCK_SINGLE_CLICK = "block_single_click"

    // Default values
    const val DEFAULT_INIT_DARK = 80
    const val DEFAULT_INIT_BRIGHT = 160
    const val DEFAULT_RUNNING_MULTIPLIER = 1.6f
    const val DEFAULT_ENABLE_PANORAMIC = true
    const val DEFAULT_ENABLE_SETTINGS_SUPPORT = true
    const val DEFAULT_BLOCK_SINGLE_CLICK = true

    /** Column-index cache (lazily initialized, queried only once on the first readRow). */
    private var columnIndices: IntArray? = null

    /**
     * Return the cached column indices, querying and caching them if not yet initialized.
     * Avoids calling getColumnIndexOrThrow on every readRow.
     */
    private fun getCachedColumnIndices(c: Cursor): IntArray {
        columnIndices?.let { return it }
        val indices = intArrayOf(
            c.getColumnIndexOrThrow(KEY_INIT_DARK),
            c.getColumnIndexOrThrow(KEY_INIT_BRIGHT),
            c.getColumnIndexOrThrow(KEY_RUNNING_MULTIPLIER),
            c.getColumnIndexOrThrow(KEY_ENABLE_PANORAMIC),
            c.getColumnIndexOrThrow(KEY_ENABLE_SETTINGS_SUPPORT),
            c.getColumnIndexOrThrow(KEY_BLOCK_SINGLE_CLICK),
        )
        columnIndices = indices
        return indices
    }

    /**
     * Read the raw values of all config columns from the [Cursor]'s current row.
     *
     * Shared by the UI side ([AodConfigStore]) and the Hook side ([com.op.aod.enhance.hook.AodConfigReader]);
     * when adding/changing a field, only this method and the corresponding data class need to change.
     */
    fun readRow(c: Cursor): ConfigValues {
        val indices = getCachedColumnIndices(c)
        return ConfigValues(
            initDark = c.getInt(indices[0]),
            initBright = c.getInt(indices[1]),
            runningMultiplier = c.getFloat(indices[2]),
            enablePanoramic = c.getInt(indices[3]) == 1,
            enableSettingsSupport = c.getInt(indices[4]) == 1,
            blockSingleClick = c.getInt(indices[5]) == 1,
        )
    }

    /**
     * Snapshot of the raw Cursor values, so the same column parsing isn't reimplemented in two places.
     */
    data class ConfigValues(
        val initDark: Int,
        val initBright: Int,
        val runningMultiplier: Float,
        val enablePanoramic: Boolean,
        val enableSettingsSupport: Boolean,
        val blockSingleClick: Boolean,
    )
}
