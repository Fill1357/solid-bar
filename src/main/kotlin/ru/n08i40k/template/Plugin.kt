package ru.n08i40k.template

import android.view.View
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XC_MethodHook

class TemplatePlugin {

    companion object {
        private const val TAG = "SolidInterfacePlugin"
    }

    fun init() {
        XposedBridge.log("[$TAG] Initializing global solid interface hooks...")

        try {
            // 1. Отключаем глобальные флаги размытия
            try {
                val sharedConfigClass = XposedHelpers.findClass("org.telegram.messenger.SharedConfig", null)
                XposedBridge.hookAllMethods(sharedConfigClass, "isDrawingBlurAvailable", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = false 
                    }
                })
            } catch (e: Throwable) {
                XposedBridge.log("[$TAG] SharedConfig error: ${e.message}")
            }

            // 2. Делаем верхнюю панель чата квадратной и сплошной
            try {
                val actionBarClass = XposedHelpers.findClass("org.telegram.ui.ActionBar.ActionBar", null)
                XposedBridge.hookAllConstructors(actionBarClass, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val actionBar = param.thisObject as? View ?: return
                        try { XposedHelpers.callMethod(actionBar, "setDrawBlurBackground", false) } catch (_: Throwable) {}
                        try { XposedHelpers.callMethod(actionBar, "setOpaque", true) } catch (_: Throwable) {}
                        actionBar.alpha = 1.0f
                    }
                })
                
                XposedHelpers.findAndHookMethod(actionBarClass, "setDrawBlurBackground", Boolean::class.javaPrimitiveType, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.args[0] = false
                    }
                })
            } catch (e: Throwable) {
                XposedBridge.log("[$TAG] ActionBar error: ${e.message}")
            }

            // 3. Выправляем нижнюю панель ввода сообщений
            try {
                val chatActivityEnterViewClass = XposedHelpers.findClass("org.telegram.ui.Components.ChatActivityEnterView", null)
                XposedBridge.hookAllConstructors(chatActivityEnterViewClass, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val enterView = param.thisObject as? View ?: return
                        enterView.alpha = 1.0f
                    }
                })
            } catch (e: Throwable) {
                XposedBridge.log("[$TAG] ChatActivityEnterView error: ${e.message}")
            }

            // 4. Глушим слои размытия BlurDrawable
            try {
                val blurDrawableClass = XposedHelpers.findClass("org.telegram.ui.Components.BlurDrawable", null)
                XposedBridge.hookAllMethods(blurDrawableClass, "draw", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = null 
                    }
                })
            } catch (e: Throwable) {
                XposedBridge.log("[$TAG] BlurDrawable error: ${e.message}")
            }

            XposedBridge.log("[$TAG] All solid interface hooks applied successfully!")
        } catch (e: Throwable) {
            XposedBridge.log("[$TAG] Critical error during hooks: ${e.message}")
        }
    }
}
