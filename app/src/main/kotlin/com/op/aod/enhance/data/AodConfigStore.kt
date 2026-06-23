package com.op.aod.enhance.data

import android.content.ContentResolver
import android.content.ContentValues
import java.util.concurrent.atomic.AtomicReference

object AodConfigStore {

    private val DEFAULT_CONFIG = AodUiConfig()

    /** Use AtomicReference instead of @Volatile var to keep CAS operations atomic. */
    private val cachedRef = AtomicReference<AodUiConfig?>()

    /**
     * Read the config, cache first.
     *
     * The first call performs one IPC query to populate the cache; later reads return the cached value directly.
     * The write side ([write]) updates the cache via CAS to stay thread-safe.
     */
    fun read(resolver: ContentResolver): AodUiConfig {
        val local = cachedRef.get()
        if (local != null) return local
        val fresh = queryOrNull(resolver)
        if (fresh != null) {
            // Try a CAS set; failure means another thread already set it, which is fine to ignore
            cachedRef.compareAndSet(null, fresh)
            return fresh
        }
        return DEFAULT_CONFIG
    }

    fun write(resolver: ContentResolver, cfg: AodUiConfig) {
        val values = ContentValues().apply {
            put(AodConfigContract.KEY_INIT_DARK, cfg.initDark)
            put(AodConfigContract.KEY_INIT_BRIGHT, cfg.initBright)
            put(AodConfigContract.KEY_RUNNING_MULTIPLIER, cfg.runningMultiplier)
            put(AodConfigContract.KEY_ENABLE_PANORAMIC, cfg.enablePanoramic)
            put(AodConfigContract.KEY_ENABLE_SETTINGS_SUPPORT, cfg.enableSettingsSupport)
            put(AodConfigContract.KEY_BLOCK_SINGLE_CLICK, cfg.blockSingleClick)
        }
        resolver.update(AodConfigProvider.CONTENT_URI, values, null, null)
        // Update the cache so concurrent threads don't lose this update
        cachedRef.set(cfg)
    }

    private fun queryOrNull(resolver: ContentResolver): AodUiConfig? {
        return runCatching {
            resolver.query(AodConfigProvider.CONTENT_URI, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val v = AodConfigContract.readRow(c)
                    AodUiConfig(
                        initDark = v.initDark,
                        initBright = v.initBright,
                        runningMultiplier = v.runningMultiplier,
                        enablePanoramic = v.enablePanoramic,
                        enableSettingsSupport = v.enableSettingsSupport,
                        blockSingleClick = v.blockSingleClick,
                    )
                } else null
            }
        }.getOrNull()
    }
}