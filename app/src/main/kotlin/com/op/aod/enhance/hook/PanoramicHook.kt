package com.op.aod.enhance.hook

import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.op.aod.enhance.BuildConfig
import java.util.concurrent.ConcurrentHashMap

internal object PanoramicHook {

    /** Reflection method cache: Class -> Method */
    private val methodCache = ConcurrentHashMap<Class<*>, java.lang.reflect.Method?>()

    /** Reflection field cache: Class -> List<Field> */
    private val fieldCache = ConcurrentHashMap<Class<*>, List<java.lang.reflect.Field>>()

    private val FIELD_NAMES = listOf("isSupportPanoramicAllDay", "isSupportPanoramicAllDayByPanelFeature", "isSupportPanoramicByPanelFeature")
    private const val SMOOTH_TRANSITION_CONTROLLER = "com.oplus.systemui.aod.display.SmoothTransitionController"

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

                // Approach 1: call setPanoramicSupportedByRemote (with method cache)
                runCatching {
                    val method = methodCache.getOrPut(realClass) {
                        runCatching { realClass.getMethod("setPanoramicSupportedByRemote") }.getOrNull()
                    }
                    method?.invoke(instance)
                }

                // Approach 2: set fields via reflection (with field cache, as a fallback safeguard)
                val fields = fieldCache.getOrPut(realClass) {
                    FIELD_NAMES.mapNotNull { name ->
                        runCatching { realClass.getDeclaredField(name) }.getOrNull()
                    }
                }
                for (f in fields) {
                    runCatching {
                        f.isAccessible = true
                        f.setBoolean(instance, true)
                    }
                }
            }
        }
    }
}
