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
            showToast("Plugin loaded! Starting in 5s...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get context", e)
            showToast("Error: " + e.message)
        }
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            makeBarsSilent()
        }, 5000)
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
            showToast("Debug saved to /sdcard/Download/solidbar_debug.txt")
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

    private fun makeBarsSilent() {
        val context = appContext ?: run {
            lastResult = "Error: No context available"
            Log.e(TAG, "No context available")
            saveToFile(lastResult)
            return
        }

        val report = StringBuilder()
        report.appendLine("=== Solid Interface Debug Report ===")
        report.appendLine("Time: ${System.currentTimeMillis()}")
        report.appendLine()

        var successCount = 0

        // === ДИАГНОСТИКА 1: Все поля SharedConfig ===
        report.appendLine("=== SHARED CONFIG FIELDS ===")
        try {
            val sharedConfigClass = Class.forName("org.telegram.messenger.SharedConfig")
            val fields = sharedConfigClass.declaredFields
            report.appendLine("Total fields: ${fields.size}")
            
            var blurFieldsFound = 0
            for (field in fields) {
                val name = field.name.lowercase()
                if (name.contains("blur") || name.contains("glass") || name.contains("liquid") || 
                    name.contains("round") || name.contains("circle") || name.contains("opaque") ||
                    name.contains("background") || name.contains("draw")) {
                    blurFieldsFound++
                    report.appendLine("  ${field.name} : ${field.type.simpleName}")
                    
                    // Пробуем отключить boolean поля
                    field.isAccessible = true
                    if (field.type == Boolean::class.java || field.type == Boolean::class.javaPrimitiveType) {
                        try {
                            field.set(null, false)
                            successCount++
                            report.appendLine("    -> Disabled (set to false)")
                        } catch (e: Exception) {
                            report.appendLine("    -> Failed: ${e.message?.take(50)}")
                        }
                    }
                }
            }
            report.appendLine("Blur-related fields found: $blurFieldsFound")
        } catch (e: Exception) {
            report.appendLine("Error: ${e.message}")
        }
        report.appendLine()

        // === ДИАГНОСТИКА 2: ActionBar ===
        report.appendLine("=== ACTION BAR ===")
        try {
            val actionBarClass = Class.forName("org.telegram.ui.ActionBar.ActionBar")
            report.appendLine("ActionBar class found: ${actionBarClass.name}")
            
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentActivityThread = activityThread.getMethod("currentActivityThread").invoke(null)
            val activitiesField = currentActivityThread.javaClass.getDeclaredField("mActivities")
            activitiesField.isAccessible = true
            val activities = activitiesField.get(currentActivityThread)
            
            var activityCount = 0
            var actionBarFound = 0
            
            if (activities != null) {
                val activitiesArray = if (activities is Array<*>) activities else (activities as? Collection<*>)?.toTypedArray()
                if (activitiesArray != null) {
                    report.appendLine("Activities found: ${activitiesArray.size}")
                    
                    for (activityRecord in activitiesArray) {
                        if (activityRecord == null) continue
                        activityCount++
                        
                        try {
                            val activityField = activityRecord.javaClass.getDeclaredField("activity")
                            activityField.isAccessible = true
                            val activity = activityField.get(activityRecord)
                            
                            if (activity != null) {
                                report.appendLine("  Activity: ${activity.javaClass.simpleName}")
                                
                                val actionBarField = activity.javaClass.getDeclaredField("actionBar")
                                actionBarField.isAccessible = true
                                val actionBar = actionBarField.get(activity)
                                
                                if (actionBar != null && actionBarClass.isInstance(actionBar)) {
                                    actionBarFound++
                                    report.appendLine("    ActionBar: FOUND")
                                    
                                    // Пробуем установить фон
                                    try {
                                        val colorDrawable = ColorDrawable(Color.parseColor("#FF1C1C1E"))
                                        val setBackgroundMethod = actionBarClass.getMethod("setBackgroundDrawable", android.graphics.drawable.Drawable::class.java)
                                        setBackgroundMethod.invoke(actionBar, colorDrawable)
                                        successCount++
                                        report.appendLine("    -> Background set to solid color")
                                    } catch (e: Exception) {
                                        report.appendLine("    -> Failed to set background: ${e.message?.take(50)}")
                                    }
                                } else {
                                    report.appendLine("    ActionBar: null or wrong type")
                                }
                            }
                        } catch (e: Exception) {
                            report.appendLine("  Error: ${e.message?.take(50)}")
                        }
                    }
                }
            }
            report.appendLine("Total activities: $activityCount, ActionBars found: $actionBarFound")
        } catch (e: Exception) {
            report.appendLine("Error: ${e.message}")
        }
        report.appendLine()

        // === ДИАГНОСТИКА 3: Theme ===
        report.appendLine("=== THEME ===")
        try {
            val themeClass = Class.forName("org.telegram.messenger.Theme")
            report.appendLine("Theme class found: ${themeClass.name}")
            
            var colorFieldsChanged = 0
            for (field in themeClass.declaredFields) {
                val name = field.name.lowercase()
                if (name.contains("wallpaper") || name.contains("background") || name.contains("chat")) {
                    field.isAccessible = true
                    if (field.type == Int::class.java || field.type == Int::class.javaPrimitiveType) {
                        try {
                            val oldValue = field.get(null) as Int
                            field.set(null, Color.parseColor("#FF1C1C1E"))
                            colorFieldsChanged++
                            report.appendLine("  ${field.name}: $oldValue -> #FF1C1C1E")
                        } catch (e: Exception) {}
                    }
                }
            }
            report.appendLine("Color fields changed: $colorFieldsChanged")
            if (colorFieldsChanged > 0) successCount++
        } catch (e: Exception) {
            report.appendLine("Error: ${e.message}")
        }
        report.appendLine()

        // === ДИАГНОСТИКА 4: Redraw ===
        report.appendLine("=== REDRAW ===")
        try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentActivityThread = activityThread.getMethod("currentActivityThread").invoke(null)
            val activitiesField = currentActivityThread.javaClass.getDeclaredField("mActivities")
            activitiesField.isAccessible = true
            val activities = activitiesField.get(currentActivityThread)
            
            var redrawCount = 0
            if (activities != null) {
                val activitiesArray = if (activities is Array<*>) activities else (activities as? Collection<*>)?.toTypedArray()
                if (activitiesArray != null) {
                    for (activityRecord in activitiesArray) {
                        if (activityRecord == null) continue
                        try {
                            val activityField = activityRecord.javaClass.getDeclaredField("activity")
                            activityField.isAccessible = true
                            val activity = activityField.get(activityRecord)
                            
                            if (activity != null) {
                                val contentView = (activity as android.app.Activity).findViewById<View>(android.R.id.content)
                                if (contentView != null) {
                                    contentView.invalidate()
                                    contentView.requestLayout()
                                    redrawCount++
                                }
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
            report.appendLine("Views invalidated: $redrawCount")
            if (redrawCount > 0) successCount++
        } catch (e: Exception) {
            report.appendLine("Error: ${e.message}")
        }
        report.appendLine()

        // === ИТОГ ===
        report.appendLine("=== SUMMARY ===")
        report.appendLine("Total successes: $successCount")
        
        lastResult = report.toString()
        Log.d(TAG, lastResult)
        saveToFile(lastResult)
        showToast("Debug saved! Check /sdcard/Download/solidbar_debug.txt")
    }
}
