/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.clock

import android.content.*
import android.graphics.*
import android.os.*
import android.util.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.api.LazyClass.mNewClockClass
import com.sevtinge.hyperceiler.utils.devicesdk.*
import com.sevtinge.hyperceiler.utils.devicesdk.DisplayUtils.*
import de.robv.android.xposed.*
import java.lang.reflect.*
import java.util.*
import kotlin.Pair

object StatusBarClockNew : BaseHook() {
    private val statusBarClass by lazy {
        loadClass("com.android.systemui.statusbar.views.MiuiClock")
    }

    private val clockBold by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_clock_bold")
    }
    private val isBold by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_clock_big_bold")
    }
    private val isSync by lazy {
        mPrefsMap.getBoolean("system_ui_disable_clock_synch")
    }
    private val clockSizeS by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_size_1", 12)
    }
    private val clockSizeB by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_size_2", 50)
    }
    private val clockSizeN by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_size_3", 12)
    }
    private val clockTextSpacing by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_double_spacing_margin_1", 16)
    }
    private val sClockLeftMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_left_margin_1", 0)
    }
    private val sClockRightMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_right_margin_1", 0)
    }
    private val sClockVerticalOffset by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_vertical_offset_1", 12)
    }
    private val fixedWidth by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_fixedcontent_width_1", 30)
    }
    private val bClockLeftMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_left_margin_2", 0)
    }
    private val bClockRightMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_right_margin_2", 0)
    }
    private val bClockVerticalOffset by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_vertical_offset_2", 12)
    }
    private val nClockLeftMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_left_margin_3", 0)
    }
    private val nClockRightMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_right_margin_3", 0)
    }
    private val nClockVerticalOffset by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_vertical_offset_3", 12)
    }
    private val clockAlign by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_clock_double_1", 0)
    }
    private val isShowSec by lazy {
        mPrefsMap.getBoolean("system_ui_clock_is_show_sec")
    }

    // 时钟格式
    private val getFormatS by lazy {
        mPrefsMap.getString("system_ui_statusbar_clock_editor_s", "HH:mm")
    }
    private val getFormatB by lazy {
        mPrefsMap.getString("system_ui_statusbar_clock_editor_b", "HH:mm")
    }
    private val getFormatN by lazy {
        mPrefsMap.getString("system_ui_statusbar_clock_editor_n", "")
    }
    private val getClockStyle by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_clock_style", 0)
    }

    private val safeFormatS by lazy {
        safeSplitFirst(getFormatS)
    }
    private val safeFormatB by lazy {
        safeSplitFirst(getFormatB)
    }
    private val safeFormatN by lazy {
        safeSplitFirst(getFormatN)
    }
    private val sClockName by lazy {
        if (getFormatN.isNullOrEmpty()) {
            when (getClockStyle) {
                0 -> safeFormatS
                1 -> "$safeFormatS\nM/d E"
                else -> "M/d E\n$safeFormatS"
            }
        } else {
            when (getClockStyle) {
                0 -> safeFormatS
                1 -> "$safeFormatS\n$safeFormatN"
                else -> "$safeFormatN\n$safeFormatS"
            }
        }
    }

    override fun init() {
        statusBarClass.constructorFinder()
            .filterByParamCount(3)
            .filterByParamTypes {
                it[0] == Context::class.java
            }.first().createAfterHook {
                runCatching {
                    val miuiClock = it.thisObject as TextView
                    val miuiClockName = miuiClock.resources.getResourceEntryName(miuiClock.id)
                        ?: return@createAfterHook

                    val isSec =
                        miuiClockName in setOf("clock", "big_time", "date_time")
                    // miuiClockName 内部标签分类如下
                    // clock 竖屏状态栏时钟
                    // big_time 通知中心时钟
                    // horizontal_time 横屏通知中心时钟
                    // date_time 通知中心日期时钟

                    if (getClockStyle != 0 && miuiClockName == "clock")
                        miuiClock.isSingleLine = false

                    if (isSec && isShowSec) {
                        val updateTimeMethod: Method = miuiClock.javaClass.getDeclaredMethod("updateTime")
                        val runnable = Runnable {
                            updateTimeMethod.isAccessible = true
                            updateTimeMethod.invoke(miuiClock)
                        }

                        val timerTask = object : TimerTask() {
                            override fun run() {
                                Handler(miuiClock.context.mainLooper).post(runnable)
                            }
                        }

                        Timer().schedule(timerTask, 1000 - System.currentTimeMillis() % 1000, 1000)
                    }
                }
            }

        if (isMoreHyperOSVersion(2f) && isBold) {
            loadClass("com.android.systemui.controlcenter.shade.NotificationHeaderExpandController\$notificationCallback\$1").methodFinder()
                .filterByName("onExpansionChanged").first().createAfterHook {
                    val notificationHeaderExpandController =
                        it.thisObject.getObjectField("this\$0")
                    notificationHeaderExpandController!!.callMethod("updateWeight", 0.3f)
                }
        } else if (isHyperOSVersion(1f)) {
            runCatching {
                loadClassOrNull("com.android.systemui.statusbar.policy.FakeStatusBarClockController")!!
                    .methodFinder().filterByName("initState")
                    .first().createHook {
                        replace { null }
                    }
            }
        }

        // 设置格式
        statusBarClass.methodFinder()
            .filterByName("updateTime")
            .single().createBeforeHook {
                runCatching {
                    applyMiuiClockStyleAndFormat(it)
                }
            }

        mNewClockClass.methodFinder()
            .filterByName("updateTime")
            .single().createBeforeHook {
                runCatching {
                    applyMiuiClockStyleAndFormat(it)
                }
            }
    }

    private fun applyMiuiClockStyleAndFormat(hook: XC_MethodHook.MethodHookParam) {
        val textV = hook.thisObject as TextView
        val context = textV.context
        val miuiClockName = textV.resources.getResourceEntryName(textV.id) ?: return

        if (miuiClockName in setOf("clock", "big_time", "date_time")) {
            setMiuiClockStyle(miuiClockName, textV)

            if (getFormatN.isEmpty() && miuiClockName == "date_time") return

            setMiuiClockFormat(context, miuiClockName, textV)
            hook.result = null
        }
    }

    private fun setMiuiClockStyle(name: String, text: TextView) {
        // 时钟加粗
        if (clockBold && (name == "clock" || (name == "big_time" && isBold && !isMoreHyperOSVersion(2f)))) {
            text.typeface = Typeface.DEFAULT_BOLD
        }

        // 设置时钟大小
        setStatusBarClock(name, text)

        if (getClockStyle != 0 && name == "clock") {
            // 状态栏时钟双排对齐
            text.textAlignment = when (clockAlign) {
                1 -> View.TEXT_ALIGNMENT_CENTER
                2 -> View.TEXT_ALIGNMENT_TEXT_END
                else -> View.TEXT_ALIGNMENT_TEXT_START
            }

            // 设置双排时钟行间距
            text.setLineSpacing(0f, clockTextSpacing * 0.05f)
        }

        // 设置时钟边距
        setClockMargin(name, text)
    }

    private fun setClockMargin(name: String, text: TextView) {
        when (name) {
            "clock" -> {
                setClockMargin(text, sClockLeftMargin, sClockRightMargin, sClockVerticalOffset)
                // 固定宽度
                if (fixedWidth > 30) {
                    text.width = (text.resources.displayMetrics.density * fixedWidth).toInt()
                }
            }
            "big_time" -> {
                setClockMargin(text, bClockLeftMargin, bClockRightMargin, bClockVerticalOffset)
            }
            else -> {
                setClockMargin(text, nClockLeftMargin, nClockRightMargin, nClockVerticalOffset)
            }
        }
    }

    private fun setStatusBarClock(name: String, text: TextView) {
        when {
            clockSizeS != 12 && name == "clock" -> {
                text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, clockSizeS.toFloat())
            }

            clockSizeB != 50 && name == "big_time" -> {
                text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, clockSizeB.toFloat())
            }

            clockSizeN != 12 && name in setOf("date_time", "horizontal_time") -> {
                text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, clockSizeN.toFloat())
            }
        }
    }

    private fun setClockMargin(
        id: TextView,
        leftMargin: Int,
        rightMargin: Int,
        verticalOffset: Int
    ) {
        val left = dp2px(leftMargin.toFloat())
        val right = dp2px(rightMargin.toFloat())
        var topMargin = 0
        if (verticalOffset != 12) {
            topMargin = dp2px((verticalOffset - 12) * 0.5f)
        }
        id.setPaddingRelative(left, topMargin, right, 0)
    }

    private fun setMiuiClockFormat(context: Context?, name: String, textV: TextView?) {
        if (context == null || textV == null) return

        val mCalendar =
            textV.getObjectField("mMiuiStatusBarClockController")
                ?.getObjectField("mCalendar") ?: return

        val (textSb, formatSb) = when (name) {
            "clock" -> Pair(StringBuilder(), StringBuilder(sClockName))

            "big_time" -> Pair(
                StringBuilder(),
                if (isSync) StringBuilder(safeFormatB)
                else StringBuilder(safeFormatS)
            )

            else -> Pair(StringBuilder(), StringBuilder(safeFormatN))
        }

        mCalendar.let {
            it.callMethod("setTimeInMillis", System.currentTimeMillis())
            it.callMethod("format", context, textSb, formatSb)
            textV.text = textSb.toString()
        }
    }

    private fun safeSplitFirst(str: String?): String {
        return str?.split("\n")?.firstOrNull() ?: ""
    }
}