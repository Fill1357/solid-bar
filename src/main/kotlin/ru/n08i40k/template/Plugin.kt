package ru.n08i40k.template

import android.util.Log
import android.webkit.ValueCallback
import android.widget.Toast
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import java.io.File

object Plugin {
    private const val TAG = "SolidBarPlugin"
    private var appContext: Context? = null
    private var lastResult: String = "No data yet"

    @JvmStatic
    fun getBuildDate(): String = "2026-11-04"

    @JvmStatic
    fun inject(version: String, logger: ValueCallback<Any?>) {
        Log.d(TAG, "✅ Solid Bar Plugin loaded! Version: $version")
        logger.onReceiveValue("Plugin loaded successfully")
        
        try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentApp = activityThread.getMethod("currentApplication").invoke(null)
            appContext = currentApp as? Context
            showToast("Plugin loaded! Disabling blur...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get context", e)
            showToast("Error: " + e.message)
        }
        
        // Отключаем blur СРАЗУ, без задержки
        disableBlur()
        
        // Через 3 секунды показываем результат
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            showToast(lastResult)
            saveToFile(lastResult)
        }, 3000)
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
    fun invokeSettingsActionCallback(key: String) {
        if (key == "check_status") {
            showToast(lastResult)
            saveToFile(lastResult)
        }
    }

    private fun showToast(message: String) {
        try {
            appContext?.let { ctx ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Toast error", e)
        }
    }

    private fun saveToFile(content: String) {
        try {
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadDir, "solidbar_debug.txt")
            file.writeText(content)
            Log.d(TAG, "Debug saved to: " + file.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save file", e)
        }
    }

    private fun disableBlur() {
        val context = appContext ?: run {
            lastResult = "Error: No context"
            return
        }

        val report = StringBuilder()
        report.appendLine("=== Solid Interface Debug ===")
        
        var successCount = 0

        // 1. Отключаем useNewBlur и другие поля
        try {
            val sharedConfigClass = Class.forName("org.telegram.messenger.SharedConfig")
            for (field in sharedConfigClass.declaredFields) {
                val name = field.name.lowercase()
                if (name.contains("blur") || name.contains("glass") || name.contains("liquid")) {
                    field.isAccessible = true
                    if (field.type == Boolean::class.java || field.type == Boolean::class.javaPrimitiveType) {
                        try {
                            field.set(null, false)
                            successCount++
                            report.appendLine("✅ Disabled: ${field.name}")
                            Log.d(TAG, "Disabled: ${field.name}")
                        } catch (e: Exception) {
                            report.appendLine("❌ Failed: ${field.name} - ${e.message?.take(30)}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            report.appendLine("Error: ${e.message}")
        }

        // 2. Пытаемся найти Activity через другой метод
        try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
            
            // Получаем все активности через getActivities()
            val getActivitiesMethod = activityThreadClass.getMethod("getActivities")
            val activities = getActivitiesMethod.invoke(activityThread) as? Array<*>
            
            if (activities != null && activities.isNotEmpty()) {
                report.appendLine("Found ${activities.size} activities via getActivities()")
                
                for (activity in activities) {
                    if (activity == null) continue
                    
                    try {
                        val activityObj = activity as android.app.Activity
                        
                        // Ищем ActionBar
                        try {
                            val actionBarField = activityObj.javaClass.getDeclaredField("actionBar")
                            actionBarField.isAccessible = true
                            val actionBar = actionBarField.get(activityObj)
                            
                            if (actionBar != null) {
                                report.appendLine("✅ Found ActionBar in ${activityObj.javaClass.simpleName}")
                                
                                // Устанавливаем непрозрачный фон
                                try {
                                    val actionBarClass = actionBar.javaClass
                                    val colorDrawable = ColorDrawable(Color.parseColor("#FF1C1C1E"))
                                    val setBackgroundMethod = actionBarClass.getMethod("setBackgroundDrawable", android.graphics.drawable.Drawable::class.java)
                                    setBackgroundMethod.invoke(actionBar, colorDrawable)
                                    successCount++
                                    report.appendLine("  ✅ Set solid background")
                                } catch (e: Exception) {
                                    report.appendLine("  ❌ Failed to set background: ${e.message?.take(30)}")
                                }
                            }
                        } catch (e: Exception) {
                            report.appendLine("No ActionBar in ${activityObj.javaClass.simpleName}")
                        }
                        
                        // Перерисовываем контент
                        try {
                            val contentView = activityObj.findViewById<View>(android.R.id.content)
                            if (contentView != null) {
                                contentView.invalidate()
                                contentView.requestLayout()
                                successCount++
                                report.appendLine("  ✅ Redraw content")
                            }
                        } catch (e: Exception) {
                            report.appendLine("  ❌ Failed to redraw: ${e.message?.take(30)}")
                        }
                        
                    } catch (e: Exception) {
                        report.appendLine("Error processing activity: ${e.message?.take(30)}")
                    }
                }
            } else {
                report.appendLine("No activities found via getActivities()")
            }
        } catch (e: Exception) {
            report.appendLine("Error finding activities: ${e.message?.take(50)}")
        }

        // 3. Принудительно перерисовываем все окна
        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
            val windowManagerClass = windowManager.javaClass
            
            // Получаем все окна
            val getWindowManager = Class.forName("android.view.WindowManagerImpl")
            val mGlobalField = windowManagerClass.getDeclaredField("mGlobal")
            mGlobalField.isAccessible = true
            val mGlobal = mGlobalField.get(windowManager)
            
            if (mGlobal != null) {
                val viewsField = mGlobal.javaClass.getDeclaredField("mViews")
                viewsField.isAccessible = true
                val views = viewsField.get(mGlobal) as? Array<*>
                
                if (views != null) {
                    report.appendLine("Found ${views.size} windows")
                    for (view in views) {
                        if (view is View) {
                            view.invalidate()
                            view.requestLayout()
                            successCount++
                        }
                    }
                    report.appendLine("✅ Redraw all windows")
                }
            }
        } catch (e: Exception) {
            report.appendLine("Error redrawing windows: ${e.message?.take(50)}")
        }

        report.appendLine()
        report.appendLine("Total successes: $successCount")
        
        lastResult = report.toString()
        Log.d(TAG, lastResult)
    }
}
