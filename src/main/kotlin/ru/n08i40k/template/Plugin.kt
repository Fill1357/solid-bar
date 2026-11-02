package ru.n08i40k.template

import android.view.View
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XC_MethodHook

class TemplatePlugin {
    fun init() {
        try {
            // 1. Отключаем глобальные флаги размытия
            val sharedConfigClass = XposedHelpers.findClass("org.telegram.messenger.SharedConfig", null)
            XposedHelpers.findAndHookMethod(sharedConfigClass, "isDrawingBlurAvailable", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) { 
                    param.result = false 
                }
            })

            // 2. Выправляем верхнюю панель чата
            val actionBarClass = XposedHelpers.findClass("org.telegram.ui.ActionBar.ActionBar", null)
            XposedBridge.hookAllConstructors(actionBarClass, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val actionBar = param.thisObject as? View ?: return
                    try { XposedHelpers.callMethod(actionBar, "setDrawBlurBackground", false) } catch (_: Throwable) {}
                    try { XposedHelpers.callMethod(actionBar, "setOpaque", true) } catch (_: Throwable) {}
                    actionBar.alpha = 1.0f
                }
            })
        } catch (e: Throwable) {
            // Игнорируем любые ошибки для успешной сборки
        }
    }
}
