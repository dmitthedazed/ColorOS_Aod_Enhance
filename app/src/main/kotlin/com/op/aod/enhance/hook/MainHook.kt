package com.op.aod.enhance.hook

import android.content.Context
import android.util.Log
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.op.aod.enhance.BuildConfig
import com.op.aod.enhance.hook.AodSettingsHook.hookAodAllDaySupportSettings
import com.op.aod.enhance.hook.BrightnessHook.hookInitBrightnessFix
import com.op.aod.enhance.hook.BrightnessHook.hookRunningBrightnessBoost
import com.op.aod.enhance.hook.PanoramicHook.hookPanoramicAllDaySupport
import com.op.aod.enhance.hook.SingleClickBlockHook.hookSingleClickWakeUpBlock

/**
 * OP AOD Enhance - main entry point.
 *
 * Only responsible for dispatching to each feature Hook by package name,
 * and providing a unified [hostAppContext] cache for all Hooks.
 */
object MainHook : YukiBaseHooker() {

    /**
     * Application Context of the current host process, initialized only once.
     *
     * All Hooks should obtain their Context via [hostAppContext] rather than reflecting
     * it themselves, to avoid the object allocation and CPU cost of repeating the
     * reflective call on every frame.
     */
    val hostAppContext: Context? by lazy {
        runCatching {
            Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication").invoke(null) as? Context
        }.getOrNull() ?: runCatching {
            Class.forName("android.app.AppGlobals")
                .getMethod("getInitialApplication").invoke(null) as? Context
        }.getOrNull().also {
            if (it == null && BuildConfig.DEBUG) {
                Log.d("AOD_Enhance", "WARN: hostAppContext is null after both fallback paths")
            }
        }
    }

    override fun onHook() {
        loadApp(name = SYSTEM_UI) {
            runCatching { hookInitBrightnessFix() }
            runCatching { hookRunningBrightnessBoost() }
            runCatching { hookPanoramicAllDaySupport() }
            runCatching { hookSingleClickWakeUpBlock() }
        }
        loadApp(name = OPLUS_AOD) {
            runCatching { hookAodAllDaySupportSettings() }
        }
    }

    private const val SYSTEM_UI = "com.android.systemui"
    private const val OPLUS_AOD = "com.oplus.aod"

}