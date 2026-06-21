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

    /** 上次成功读取的缓存值，IPC 失败时作为兜底。 */
    @Volatile
    private var cached: AodConfig? = null

    /** 上次成功读取的时间戳（ns），用于 TTL 判断。 */
    @Volatile
    private var lastReadTimeNs = 0L

    /** 缓存有效期：1 秒内复用缓存，避免高频 IPC（如触摸事件触发 4 次 Hook 调用）。 */
    private const val CACHE_TTL_NS = 1_000_000_000L

    /** 首次读取标记，避免首次读取时因 lastReadTimeNs=0 而每次都走 IPC。 */
    @Volatile
    private var isFirstRead = true

    /**
     * 读取当前配置。
     *
     * - 首次读取：直读 Provider
     * - TTL 内：返回缓存值（零 IPC）
     * - TTL 外：直读 Provider 获取最新值
     * - IPC 失败：返回缓存兜底 / DEFAULT_CONFIG
     */
    fun read(context: Context?): AodConfig {
        if (context == null) return DEFAULT_CONFIG

        val cachedVal = cached
        if (!isFirstRead && cachedVal != null) {
            val now = System.nanoTime()
            // 修复：使用已缓存的 lastReadTimeNs 判断，避免首次读取时 lastReadTimeNs=0 的问题
            if (now - lastReadTimeNs < CACHE_TTL_NS) {
                return cachedVal
            }
        }

        return readFromProvider(context) ?: cachedVal ?: DEFAULT_CONFIG
    }

    /**
     * 直读 Provider。成功时更新缓存和时间戳，失败时返回 null。
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
