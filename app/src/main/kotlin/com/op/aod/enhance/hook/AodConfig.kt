package com.op.aod.enhance.hook

import android.content.Context
import android.net.Uri
import com.op.aod.enhance.data.AodConfigContract
import kotlin.concurrent.Volatile

internal data class AodConfig(
    val initDark: Int = AodConfigContract.DEFAULT_INIT_DARK,
    val initBright: Int = AodConfigContract.DEFAULT_INIT_BRIGHT,
    val runningMultiplier: Float = AodConfigContract.DEFAULT_RUNNING_MULTIPLIER,
    val enablePanoramic: Boolean = AodConfigContract.DEFAULT_ENABLE_PANORAMIC,
    val enableSettingsSupport: Boolean = AodConfigContract.DEFAULT_ENABLE_SETTINGS_SUPPORT,
    val blockSingleClick: Boolean = AodConfigContract.DEFAULT_BLOCK_SINGLE_CLICK,
)

internal object AodConfigReader {

    private val DEFAULT_CONFIG = AodConfig()

    private val uri: Uri = Uri.parse("content://com.op.aod.enhance.config/aod_config")

    /** Last successfully read value, used as a fallback when IPC fails. */
    @Volatile
    private var cached: AodConfig? = null

    /** Timestamp (ns) of the last successful read, used for TTL checks. */
    @Volatile
    private var lastReadTimeNs = 0L

    /** Cache validity: reuse the cache within 1 second to avoid frequent IPC (e.g. a touch event triggering 4 Hook calls). */
    private const val CACHE_TTL_NS = 1_000_000_000L

    /** First-read flag, to avoid going through IPC every time because lastReadTimeNs=0 on the first read. */
    @Volatile
    private var isFirstRead = true

    /**
     * Read the current config.
     *
     * - First read: query the Provider directly
     * - Within TTL: return the cached value (zero IPC)
     * - Past TTL: query the Provider directly for the latest value
     * - IPC failure: return the cached fallback / DEFAULT_CONFIG
     */
    fun read(context: Context?): AodConfig {
        if (context == null) return DEFAULT_CONFIG

        val cachedVal = cached
        if (!isFirstRead && cachedVal != null) {
            val now = System.nanoTime()
            // Fix: rely on the cached lastReadTimeNs, avoiding the first-read lastReadTimeNs=0 problem
            if (now - lastReadTimeNs < CACHE_TTL_NS) {
                return cachedVal
            }
        }

        return readFromProvider(context) ?: cachedVal ?: DEFAULT_CONFIG
    }

    /**
     * Query the Provider directly. Updates the cache and timestamp on success, returns null on failure.
     */
    private fun readFromProvider(context: Context): AodConfig? {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val v = AodConfigContract.readRow(c)
                    AodConfig(
                        initDark = v.initDark,
                        initBright = v.initBright,
                        runningMultiplier = v.runningMultiplier,
                        enablePanoramic = v.enablePanoramic,
                        enableSettingsSupport = v.enableSettingsSupport,
                        blockSingleClick = v.blockSingleClick,
                    )
                } else null
            }
        }.getOrNull()?.also { fresh ->
            cached = fresh
            lastReadTimeNs = System.nanoTime()
            isFirstRead = false
        }
    }
}
