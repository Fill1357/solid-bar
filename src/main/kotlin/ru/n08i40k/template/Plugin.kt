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
        
        disableBlurFlags()
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            forceRedraw()
            showToast(lastResult)
            saveToFile(lastResult)
        }, 2000)
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

    private fun disableBlurFlags() {
        val report = StringBuilder()
        report.appendLine("=== Solid Interface Debug ===")
        var successCount = 0

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
            report.appendLine("Error reading SharedConfig: ${e.message}")
        }
        
        lastResult = report.toString()
    }

    private fun forceRedraw() {
        val report = StringBuilder(lastResult)
        report.appendLine("\n=== REDRAW PHASE ===")
        var redrawSuccess = 0
        var activitiesProcessed = 0

        try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
            
            val mActivitiesField = activityThreadClass.getDeclaredField("mActivities")
            mActivitiesField.isAccessible = true
            val mActivities = mActivitiesField.get(activityThread)
            
            if (mActivities is java.util.Map<*, *>) {
                // Перебираем ВСЕ активности, не останавливаемся на первой
                mActivities.forEach { _, record ->
                    if (record != null) {
                        try {
                            val activityField = record.javaClass.getDeclaredField("activity")
                            activityField.isAccessible = true
                            val activity = activityField.get(record) as? android.app.Activity
                            
                            if (activity != null && !activity.isFinishing) {
                                activitiesProcessed++
                                val activityName = activity.javaClass.simpleName
                                report.appendLine("\n📱 Activity #$activitiesProcessed: $activityName")
                                
                                // Способ 1: Через поле actionBar
                                var actionBarFound = false
                                try {
                                    val actionBarField = activity.javaClass.getDeclaredField("actionBar")
                                    actionBarField.isAccessible = true
                                    val actionBar = actionBarField.get(activity)
                                    
                                    if (actionBar != null) {
                                        actionBarFound = true
                                        setActionBarSolid(actionBar, report)
                                        redrawSuccess++
                                    }
                                } catch (e: Exception) {
                                    // Поле не найдено, пробуем другие способы
                                }
                                
                                // Способ 2: Через метод getActionBar()
                                if (!actionBarFound) {
                                    try {
                                        val getActionBarMethod = activity.javaClass.getMethod("getActionBar")
                                        val actionBar = getActionBarMethod.invoke(activity)
                                        
                                        if (actionBar != null) {
                                            actionBarFound = true
                                            setActionBarSolid(actionBar, report)
                                            redrawSuccess++
                                        }
                                    } catch (e: Exception) {
                                        // Метод не найден
                                    }
                                }
                                
                                // Способ 3: Через findViewById
                                if (!actionBarFound) {
                                    try {
                                        val actionBarId = activity.resources.getIdentifier("action_bar", "id", activity.packageName)
                                        if (actionBarId != 0) {
                                            val actionBarView = activity.findViewById<View>(actionBarId)
                                            if (actionBarView != null) {
                                                actionBarFound = true
                                                val colorDrawable = ColorDrawable(Color.parseColor("#FF1C1C1E"))
                                                actionBarView.background = colorDrawable
                                                redrawSuccess++
                                                report.appendLine("  ✅ ActionBar found via findViewById and set solid")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // findViewById не сработал
                                    }
                                }
                                
                                if (!actionBarFound) {
                                    report.appendLine("  ℹ️ ActionBar not found")
                                }
                                
                                // Перерисовываем DecorView
                                try {
                                    activity.window.decorView.invalidate()
                                    activity.window.decorView.requestLayout()
                                    redrawSuccess++
                                    report.appendLine("  ✅ DecorView redrawn")
                                } catch (e: Exception) {
                                    report.appendLine("  ❌ Redraw failed: ${e.message?.take(30)}")
                                }
                            }
                        } catch (e: Exception) {
                            // Игнорируем ошибки парсинга отдельных записей
                        }
                    }
                }
            }
            
            if (activitiesProcessed == 0) {
                report.appendLine("\n⚠️ No active activities found")
            } else {
                report.appendLine("\n✅ Processed $activitiesProcessed activities")
            }
            
        } catch (e: Exception) {
            report.appendLine("\n❌ Error: ${e.message?.take(50)}")
        }

        report.appendLine("\n=== SUMMARY ===")
        report.appendLine("Total successes: $redrawSuccess")
        
        lastResult = report.toString()
        Log.d(TAG, lastResult)
    }
    
    private fun setActionBarSolid(actionBar: Any, report: StringBuilder) {
        try {
            val actionBarClass = actionBar.javaClass
            val colorDrawable = ColorDrawable(Color.parseColor("#FF1C1C1E"))
            val setBackgroundMethod = actionBarClass.getMethod("setBackgroundDrawable", android.graphics.drawable.Drawable::class.java)
            setBackgroundMethod.invoke(actionBar, colorDrawable)
            report.appendLine("  ✅ ActionBar background set to solid (via ${actionBarClass.simpleName})")
        } catch (e: Exception) {
            report.appendLine("  ❌ Failed to set ActionBar background: ${e.message?.take(30)}")
        }
    }
}
