package ru.n08i40k.template

import android.app.Activity
import android.app.Application
import android.os.Bundle
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
    private var activitiesProcessed = 0

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
                // Регистрируем "слушатель" всех Activity
                application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                        handleActivity(activity, "created")
                    }
                    override fun onActivityStarted(activity: Activity) {}
                    override fun onActivityResumed(activity: Activity) {
                        // Применяем при каждом возврате к Activity
                        handleActivity(activity, "resumed")
                    }
                    override fun onActivityPaused(activity: Activity) {}
                    override fun onActivityStopped(activity: Activity) {}
                    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                    override fun onActivityDestroyed(activity: Activity) {}
                })
                showToast("Plugin loaded! Watching activities...")
                Log.d(TAG, "Registered ActivityLifecycleCallbacks")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup", e)
            showToast("Error: " + e.message)
        }
        
        // Отключаем blur сразу
        disableBlurFlags()
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
        val activityName = activity.javaClass.name
        val simpleName = activity.javaClass.simpleName
        
        // Логируем каждую Activity
        Log.d(TAG, "Activity $trigger: $activityName")
        
        // Применяем ко ВСЕМ Activity (или можно фильтровать по имени)
        try {
            var actionBarFound = false
            
            // Способ 1: Поле actionBar
            try {
                val actionBarField = activity.javaClass.getDeclaredField("actionBar")
                actionBarField.isAccessible = true
                val actionBar = actionBarField.get(activity)
                if (actionBar != null) {
                    actionBarFound = true
                    setActionBarSolid(actionBar)
                }
            } catch (e: Exception) {}
            
            // Способ 2: Метод getActionBar()
            if (!actionBarFound) {
                try {
                    val actionBar = activity.actionBar
                    if (actionBar != null) {
                        actionBarFound = true
                        setActionBarSolid(actionBar)
                    }
                } catch (e: Exception) {}
            }
            
            // Способ 3: findViewById
            if (!actionBarFound) {
                try {
                    val actionBarId = activity.resources.getIdentifier("action_bar", "id", activity.packageName)
                    if (actionBarId != 0) {
                        val actionBarView = activity.findViewById<View>(actionBarId)
                        if (actionBarView != null) {
                            actionBarFound = true
                            val colorDrawable = ColorDrawable(Color.parseColor("#FF1C1C1E"))
                            actionBarView.background = colorDrawable
                            Log.d(TAG, "  ✅ ActionBar set via findViewById in $simpleName")
                        }
                    }
                } catch (e: Exception) {}
            }
            
            // Перерисовка
            try {
                activity.window.decorView.invalidate()
                activity.window.decorView.requestLayout()
            } catch (e: Exception) {}
            
            if (actionBarFound) {
                activitiesProcessed++
                lastResult = "Applied to $simpleName (total: $activitiesProcessed)\nvia $trigger"
                Log.d(TAG, "✅ Applied to $simpleName via $trigger (total: $activitiesProcessed)")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling $simpleName", e)
        }
    }
    
    private fun setActionBarSolid(actionBar: Any) {
        try {
            val actionBarClass = actionBar.javaClass
            val colorDrawable = ColorDrawable(Color.parseColor("#FF1C1C1E"))
            val setBackgroundMethod = actionBarClass.getMethod("setBackgroundDrawable", android.graphics.drawable.Drawable::class.java)
            setBackgroundMethod.invoke(actionBar, colorDrawable)
            Log.d(TAG, "  ✅ ActionBar background set to solid")
        } catch (e: Exception) {
            Log.e(TAG, "  ❌ Failed to set background: ${e.message?.take(30)}")
        }
    }
}
