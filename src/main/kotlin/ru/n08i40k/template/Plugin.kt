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

        try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
            
            val mActivitiesField = activityThreadClass.getDeclaredField("mActivities")
            mActivitiesField.isAccessible = true
            val mActivities = mActivitiesField.get(activityThread)
            
            var activitiesFound = 0
            
            if (mActivities is java.util.Map<*, *>) {
                // ИСПРАВЛЕНИЕ: используем forEach вместо values()
                mActivities.forEach { _, record ->
                    if (record != null) {
                        try {
                            val activityField = record.javaClass.getDeclaredField("activity")
                            activityField.isAccessible = true
                            val activity = activityField.get(record) as? android.app.Activity
                            
                            if (activity != null && !activity.isFinishing) {
                                activitiesFound++
                                report.appendLine("✅ Found Activity: ${activity.javaClass.simpleName}")
                                
                                try {
                                    val actionBarField = activity.javaClass.getDeclaredField("actionBar")
                                    actionBarField.isAccessible = true
                                    val actionBar = actionBarField.get(activity)
                                    
                                    if (actionBar != null) {
                                        val actionBarClass = actionBar.javaClass
                                        val colorDrawable = ColorDrawable(Color.parseColor("#FF1C1C1E"))
                                        val setBackgroundMethod = actionBarClass.getMethod("setBackgroundDrawable", android.graphics.drawable.Drawable::class.java)
                                        setBackgroundMethod.invoke(actionBar, colorDrawable)
                                        redrawSuccess++
                                        report.appendLine("  ✅ ActionBar background set to solid")
                                    }
                                } catch (e: Exception) {
                                    report.appendLine("  ℹ️ ActionBar: ${e.message?.take(30)}")
                                }
                                
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
                            // игнорируем
                        }
                    }
                }
            }
            
            if (activitiesFound == 0) {
                report.appendLine("⚠️ No active activities found")
            }
            
        } catch (e: Exception) {
            report.appendLine("❌ Error: ${e.message?.take(50)}")
        }

        report.appendLine("\n=== SUMMARY ===")
        report.appendLine("Total redraw successes: $redrawSuccess")
        
        lastResult = report.toString()
        Log.d(TAG, lastResult)
    }
}
