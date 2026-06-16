package com.op.aod.enhance.hook

import android.util.Log
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.toClass
import com.op.aod.enhance.BuildConfig
import java.util.concurrent.atomic.AtomicLong

internal object SingleClickBlockHook {

    /** 上次被拦截的 onClick 时间戳（ms）。Atomic 保证多线程安全。 */
    private val lastBlockedTime = AtomicLong(0)

    /** 双击间隔阈值（ms）。 */
    private const val DOUBLE_CLICK_THRESHOLD = 350L

    fun YukiBaseHooker.hookSingleClickWakeUpBlock() {
        if (!AodConfigReader.read(MainHook.hostAppContext).blockSingleClick) {
            if (BuildConfig.DEBUG) Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: disabled by config")
            return
        }

        val targets = arrayOf(
            "com.oplus.systemui.aod.scene.AodViewSingleClickWakeUpHolder\$AodSingleClickWakeUpCallback" to "NormalAod",
            "com.oplus.systemui.aod.scene.PanoramicAodSingleClickWakeUpController\$PanoramicAodSingleClickWakeUpCallback" to "PanoramicAod",
            "com.oplus.systemui.aod.display.OplusWakeUpController\$AodSingleClickWakeUpCallback" to "WakeUpController",
        )

        for ((cls, label) in targets) {
            registerClickHook(cls, label)
        }
    }

    private fun YukiBaseHooker.registerClickHook(targetClass: String, label: String) {
        runCatching {
            targetClass
                .toClass(appClassLoader)
                .resolve()
                .firstMethod { name = "onClick" }
                .hook {
                    before {
                        val now = System.currentTimeMillis()
                        val prev = lastBlockedTime.get()

                        if (prev != 0L && now - prev < DOUBLE_CLICK_THRESHOLD) {
                            // 放行：快速第二次点击
                            lastBlockedTime.set(0L)
                            if (BuildConfig.DEBUG) {
                                Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $label allowed (double-click)")
                            }
                            return@before
                        }

                        // 拦截：首次单击或慢速重试
                        lastBlockedTime.set(now)
                        result = null
                        if (BuildConfig.DEBUG) {
                            Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $label blocked")
                        }
                    }
                }
        }.onFailure {
            if (BuildConfig.DEBUG) {
                Log.d("AOD_Enhance", "AOD_SINGLE_CLICK_BLOCK: $label onClick not available, ${it.message}")
            }
        }
    }
}
