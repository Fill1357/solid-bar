package ru.n08i40k.template

import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XC_MethodHook
import org.telegram.ui.ActionBar.ActionBar

class TemplatePlugin {
    
    val TAG: String = "SolidActionBar" 
    
    fun init() {
        XposedBridge.log("[$TAG] Initializing SolidActionBar Plugin...")

        try {
            val actionBarClass = ActionBar::class.java

            // 1. Хукаем конструкторы ActionBar для применения настроек сразу при создании
            XposedBridge.hookAllConstructors(actionBarClass, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val actionBar = param.thisObject as? ActionBar ?: return
                    applySolidStyle(actionBar)
                }
            })

            // 2. Хукаем методы установки размытия/прозрачности, если они вызываются динамически
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

    private fun applySolidStyle(actionBar: ActionBar) {
        try {
            actionBar.setDrawBlurBackground(false)
            actionBar.setOpaque(true)
            actionBar.alpha = 1.0f
        } catch (e: Throwable) {
            // Игнорируем возможные изменения в новых версиях TG
        }
    }
}
