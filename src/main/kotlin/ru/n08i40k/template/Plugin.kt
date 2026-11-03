package ru.n08i40k.template

import android.util.Log
import android.webkit.ValueCallback
import android.widget.Toast

object Plugin {
    private const val TAG = "SolidBarPlugin"

    @JvmStatic
    fun getBuildDate(): String = "2026-11-04"

    @JvmStatic
    fun inject(version: String, logger: ValueCallback<Any?>) {
        Log.d(TAG, "✅ Solid Bar Plugin loaded! Version: $version")
        logger.onReceiveValue("Plugin loaded successfully")
        disableLiquidGlass()
    }

    @JvmStatic
    fun finalizeInject() {
        Log.d(TAG, "✅ Solid Bar Plugin initialization complete")
    }

    @JvmStatic
    fun eject() {
        Log.d(TAG, "🗑️ Solid Bar Plugin ejected")
    }

    @JvmStatic
    fun invokeChatContextMenuCallback(key: String, dialogId: Long) {}

    @JvmStatic
    fun invokeSettingsActionCallback(key: String) {}

    private fun disableLiquidGlass() {
        try {
            // Получаем контекст через рефлексию (без импорта ApplicationLoader)
            val applicationClass = Class.forName("android.app.ActivityThread")
            val currentAppMethod = applicationClass.getMethod("currentApplication")
            val context = currentAppMethod.invoke(null) as? android.content.Context

            var disabledCount = 0
            val disabledFields = StringBuilder()

            // Ищем все поля в SharedConfig, связанные с размытием, стеклом и кругами
            try {
                val sharedConfigClass = Class.forName("org.telegram.messenger.SharedConfig")
                for (field in sharedConfigClass.declaredFields) {
                    val name = field.name.lowercase()
                    if (name.contains("blur") || name.contains("glass") || name.contains("liquid") || 
                        name.contains("round") || name.contains("circle") || name.contains("opaque")) {
                        
                        field.isAccessible = true
                        // Проверяем, что это boolean (логическое значение)
                        if (field.type == Boolean::class.java || field.type == Boolean::class.javaPrimitiveType) {
                            try {
                                field.set(null, false) // Отключаем (статическое поле)
                                disabledCount++
                                disabledFields.append(field.name).append(", ")
                                Log.d(TAG, "🎯 Disabled field: ${field.name}")
                            } catch (e: Exception) {
                                // Не статическое, пропускаем
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in SharedConfig", e)
            }

            // Показываем всплывающее сообщение, чтобы ты видел результат
            if (context != null) {
                val message = if (disabledCount > 0) {
                    "SolidBar: Disabled $disabledCount fields: $disabledFields"
                } else {
                    "SolidBar: No blur/glass fields found in SharedConfig!"
                }
                
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                Log.d(TAG, message)
            } else {
                Log.e(TAG, "Could not get application context")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error disabling liquid glass", e)
        }
    }
}
