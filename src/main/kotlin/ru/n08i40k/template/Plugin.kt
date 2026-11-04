package ru.n08i40k.template

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import android.webkit.ValueCallback
import android.widget.Toast
import android.content.Context
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
            val application = currentApp as? Application
            appContext = application
            
            if (application != null) {
                application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                        handleActivity(activity, "created")
                    }
                    override fun onActivityStarted(activity: Activity) {}
                    override fun onActivityResumed(activity: Activity) {
                        handleActivity(activity, "resumed")
                    }
                    override fun onActivityPaused(activity: Activity) {}
                    override fun onActivityStopped(activity: Activity) {}
                    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                    override fun onActivityDestroyed(activity: Activity) {}
                })
                showToast("Plugin loaded! Watching activities...")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup", e)
            showToast("Error: " + e.message)
        }
        
        disableBlurFlags()
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
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

    private fun handleActivity(activity: Activity, trigger: String) {
        val simpleName = activity.javaClass.simpleName
        Log.d(TAG, "Activity $trigger: $simpleName")
        
        try {
            activity.window.decorView.invalidate()
            activity.window.decorView.requestLayout()
            Log.d(TAG, "✅ Redrew: $simpleName")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling $simpleName", e)
        }
    }
}
