package com.op.aod.enhance.hook

import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.op.aod.enhance.BuildConfig

internal object PanoramicHook {

    fun YukiBaseHooker.hookPanoramicAllDaySupport() {
        val clazz = runCatching {
            SMOOTH_TRANSITION_CONTROLLER.toClass(appClassLoader).resolve()
        }.getOrNull() ?: return

        val target = runCatching {
            clazz.firstMethod { name = "getInstance" }
        }.getOrNull() ?: return

        if (BuildConfig.DEBUG) {
            Log.d("AOD_Enhance", "AOD_PANORAMIC_HOOK: Registered")
        }

        target.hook {
            after {
                val instance = result<Any>() ?: return@after
                val cfg = AodConfigReader.read(MainHook.hostAppContext)
                if (!cfg.enablePanoramic) return@after

                val realClass = instance::class.java

                // 方法1：调用 setPanoramicSupportedByRemote
                runCatching {
                    realClass.getMethod("setPanoramicSupportedByRemote").invoke(instance)
                }

                // 方法2：反射设字段（双重保障）
                for (name in FIELD_NAMES) {
                    runCatching {
                        val f = realClass.getDeclaredField(name)
                        f.isAccessible = true
                        f.setBoolean(instance, true)
                    }
                }
            }
        }
    }

    private val FIELD_NAMES = listOf("isSupportPanoramicAllDay", "isSupportPanoramicAllDayByPanelFeature", "isSupportPanoramicByPanelFeature")
    private const val SMOOTH_TRANSITION_CONTROLLER = "com.oplus.systemui.aod.display.SmoothTransitionController"
}
