package ru.n08i40k.template

import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XC_MethodHook
import android.view.View

// Имитация структуры для обмана компилятора, чтобы не качать 60МБ JAR-файл
class TemplatePlugin {
    
    val TAG: String = "SolidActionBar" 
    
    fun init() {
        XposedBridge.log("[$TAG] Initializing SolidActionBar Plugin...")

        try {
            // Находим класс динамически в памяти самого AyuGram/exteraGram во время работы
            val actionBarClass = XposedHelpers.findClass("org.telegram.ui.ActionBar.ActionBar", null)

            // 1. Перехватываем создание панели
            XposedBridge.hookAllConstructors(actionBarClass, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val actionBar = param.thisObject as? View ?: return
                    applySolidStyle(actionBar)
                }
            })

            // 2. Блокируем включение размытия
            try {
                XposedHelpers.findAndHookMethod(
                    actionBarClass,
                    "setDrawBlurBackground",
                    Boolean::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.args[0] = false
                        }
                    }
                )
            } catch (e: Throwable) {
                XposedBridge.log("[$TAG] Method setDrawBlurBackground not found: ${e.message}")
            }

            // 3. Заставляем панель быть сплошной (Opaque)
            try {
                XposedHelpers.findAndHookMethod(
                    actionBarClass,
                    "setOpaque",
                    Boolean::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.args[0] = true
                        }
                    }
                )
            } catch (e: Throwable) {
                XposedBridge.log("[$TAG] Method setOpaque not found: ${e.message}")
            }

            XposedBridge.log("[$TAG] Successfully hooked ActionBar!")
        } catch (e: Throwable) {
            XposedBridge.log("[$TAG] Failed to hook ActionBar: ${e.message}")
        }
    }

    private fun applySolidStyle(actionBar: Any) {
        try {
            XposedHelpers.callMethod(actionBar, "setDrawBlurBackground", false)
            XposedHelpers.callMethod(actionBar, "setOpaque", true)
            if (actionBar is View) {
                actionBar.alpha = 1.0f
            }
        } catch (e: Throwable) {
            // Игнорируем возможные мелкие ошибки
        }
    }
}
