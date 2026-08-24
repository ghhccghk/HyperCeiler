package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.island

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClassOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook

class UnlockFocus : BaseHook() {
    override fun init() {
        XposedLog.d(TAG, "UnlockFocus init")
        val a = loadClassOrNull("com.miui.systemui.notification.NotificationSettingsManager")

        a?.findMethod {
            name("canShowFocusState")
        }
            ?.createAfterHook {
                it.result = 1
            }

        a?.findMethod {
            name("canShowFocusStateApp")
        }
            ?.createAfterHook {
                it.result = 1
            }
    }
}
