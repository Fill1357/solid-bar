package ru.n08i40k.template

import android.util.Log
import android.webkit.ValueCallback

// Объект Plugin — это то, что ждёт Python-загрузчик шаблона n08i40k
object Plugin {
    private const val TAG = "SolidBarPlugin"

    // 1. Метод, который запрашивает загрузчик для проверки версии
    @JvmStatic
    fun getBuildDate(): String = "2026-11-04"

    // 2. Главный метод инициализации. Вызывается при загрузке плагина
    @JvmStatic
    fun inject(version: String, logger: ValueCallback<Any?>) {
        Log.d(TAG, "✅ Solid Bar Plugin загружен! Версия: $version")
        logger.onReceiveValue("Плагин успешно загружен")
        
        // Запускаем нашу магию по отключению размытия
        disableLiquidGlass()
    }

    // 3. Метод завершения инициализации (заглушка для загрузчика)
    @JvmStatic
    fun finalizeInject() {
        Log.d(TAG, "✅ Solid Bar Plugin инициализация завершена")
    }

    // 4. Метод выгрузки плагина (заглушка)
    @JvmStatic
    fun eject() {
        Log.d(TAG, "🗑️ Solid Bar Plugin выгружен")
    }

    // 5. Заглушки для меню (чтобы загрузчик не падал, если они есть в шаблоне)
    @JvmStatic
    fun invokeChatContextMenuCallback(key: String, dialogId: Long) {
        Log.d(TAG, "Menu click: $key, $dialogId")
    }

    @JvmStatic
    fun invokeSettingsActionCallback(key: String) {
        Log.d(TAG, "Settings click: $key")
    }

    // === НАША ОСНОВНАЯ ЛОГИКА ===
    private fun disableLiquidGlass() {
        try {
            val sharedConfigClass = Class.forName("org.telegram.messenger.SharedConfig")
            var found = false
            
            // Ищем все поля, в названии которых есть "blur", "glass" или "opaque"
            for (field in sharedConfigClass.declaredFields) {
                if (field.name.contains("blur", ignoreCase = true) || 
                    field.name.contains("glass", ignoreCase = true) ||
                    field.name.contains("opaque", ignoreCase = true)) {
                    
                    field.isAccessible = true
                    // Если это логическое поле (boolean), пробуем выставить false
                    if (field.type == Boolean::class.java || field.type == Boolean::class.javaPrimitiveType) {
                        field.set(null, false)
                        Log.d(TAG, "🎯 Отключено поле: ${field.name}")
                        found = true
                    }
                }
            }
            
            if (!found) {
                Log.w(TAG, "⚠️ Поля blur/glass не найдены в SharedConfig. Возможно, в этой версии AyuGram они называются иначе.")
            }
            
            // Дополнительная попытка: вызов метода отключения, если он существует
            try {
                val method = sharedConfigClass.getDeclaredMethod("setDrawingBlurAvailable", Boolean::class.javaPrimitiveType)
                method.isAccessible = true
                method.invoke(null, false)
                Log.d(TAG, "🎯 Вызван метод setDrawingBlurAvailable(false)")
            } catch (e: Exception) {
                // Это нормально, если такого метода нет
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка при отключении liquid glass", e)
        }
    }
}
