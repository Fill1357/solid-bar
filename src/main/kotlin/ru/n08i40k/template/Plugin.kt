package ru.n08i40k.template

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.widget.Toast
import java.io.File

object Plugin {
    private const val TAG = "SolidBarPlugin"
    private var appContext: android.content.Context? = null
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
                showToast("Plugin loaded! Disabling blur...")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup", e)
            showToast("Error: " + e.message)
        }
        
        disableBlur()
        
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

    private fun disableBlur() {
        val report = StringBuilder()
        report.appendLine("=== Disabling Blur (based on ChatActivity.java source) ===")
        var successCount = 0

        // === 1. Отключаем SharedConfig.chatBlurEnabled ===
        try {
            val sharedConfigClass = Class.forName("org.telegram.messenger.SharedConfig")
            
            for (field in sharedConfigClass.declaredFields) {
                val name = field.name.lowercase()
                if (name.contains("blur") || name.contains("chat_blur") || name.contains("glass")) {
                    field.isAccessible = true
                    if (field.type == Boolean::class.java || field.type == Boolean::class.javaPrimitiveType) {
                        try {
                            field.set(null, false)
                            successCount++
                            report.appendLine("✅ Disabled SharedConfig.${field.name}")
                        } catch (e: Exception) {
                            report.appendLine("❌ Failed ${field.name}: ${e.message?.take(40)}")
                        }
                    }
                }
            }
            
            try {
                for (method in sharedConfigClass.methods) {
                    if (method.name.lowercase().contains("setchatblur") || 
                        method.name.lowercase().contains("setblur")) {
                        if (method.parameterTypes.size == 1 && 
                            method.parameterTypes[0] == Boolean::class.javaPrimitiveType) {
                            method.invoke(null, false)
                            successCount++
                            report.appendLine("✅ Called ${method.name}(false)")
                        }
                    }
                }
            } catch (e: Exception) {
                report.appendLine("ℹ️ No setter method: ${e.message?.take(40)}")
            }
        } catch (e: Exception) {
            report.appendLine("❌ SharedConfig error: ${e.message}")
        }

        // === 2. Отключаем LiteMode.FLAG_LIQUID_GLASS ===
        try {
            val liteModeClass = Class.forName("org.telegram.messenger.LiteMode")
            
            var liquidGlassFlag = 0
            for (field in liteModeClass.declaredFields) {
                if (field.name.uppercase().contains("LIQUID") || 
                    field.name.uppercase().contains("GLASS")) {
                    field.isAccessible = true
                    if (field.type == Int::class.java) {
                        liquidGlassFlag = field.get(null) as Int
                        report.appendLine("Found ${field.name} = $liquidGlassFlag")
                    }
                }
            }
            
            if (liquidGlassFlag != 0) {
                for (field in liteModeClass.declaredFields) {
                    if (field.name.lowercase() == "flags" || 
                        field.name.lowercase() == "currentflags" ||
                        field.name.lowercase() == "s_flags") {
                        field.isAccessible = true
                        if (field.type == Int::class.java) {
                            val currentFlags = field.get(null) as Int
                            val newFlags = currentFlags and liquidGlassFlag.inv()
                            if (currentFlags != newFlags) {
                                field.set(null, newFlags)
                                successCount++
                                report.appendLine("✅ Removed FLAG_LIQUID_GLASS: $currentFlags -> $newFlags")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            report.appendLine("❌ LiteMode error: ${e.message}")
        }

        // === 3. Заменяем glass drawable factories на ColorDrawable ===
        try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
            val mActivitiesField = activityThreadClass.getDeclaredField("mActivities")
            mActivitiesField.isAccessible = true
            val mActivities = mActivitiesField.get(activityThread)
            
            var activitiesProcessed = 0
            
            if (mActivities is java.util.Map<*, *>) {
                for (record in mActivities.values()) {
                    if (record != null) {
                        try {
                            val activityField = record.javaClass.getDeclaredField("activity")
                            activityField.isAccessible = true
                            val activity = activityField.get(record) as? Activity
                            
                            if (activity != null && !activity.isFinishing) {
                                activitiesProcessed++
                                val activityName = activity.javaClass.simpleName
                                report.appendLine("\n📱 Activity #$activitiesProcessed: $activityName")
                                
                                // Заменяем glassBackgroundDrawableFactory и связанные поля
                                val glassFieldNames = listOf(
                                    "glassBackgroundDrawableFactory",
                                    "glassBackgroundDrawableFactoryFrosted",
                                    "navbarContentDrawableFactory",
                                    "scrimBlur3Factory"
                                )
                                
                                for (fieldName in glassFieldNames) {
                                    try {
                                        val field = activity.javaClass.getDeclaredField(fieldName)
                                        field.isAccessible = true
                                        val factory = field.get(activity)
                                        if (factory != null) {
                                            report.appendLine("  Found field: $fieldName = ${factory.javaClass.simpleName}")
                                            // Пытаемся заменить на null
                                            field.set(activity, null)
                                            successCount++
                                            report.appendLine("  ✅ Set $fieldName = null")
                                        }
                                    } catch (e: Exception) {
                                        // Поле не найдено
                                    }
                                }
                                
                                // Перерисовываем DecorView
                                try {
                                    activity.window.decorView.invalidate()
                                    activity.window.decorView.requestLayout()
                                    successCount++
                                    report.appendLine("  ✅ DecorView redrawn")
                                } catch (e: Exception) {
                                    report.appendLine("  ❌ Redraw failed: ${e.message?.take(30)}")
                                }
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
            
            if (activitiesProcessed == 0) {
                report.appendLine("\n⚠️ No activities found")
            }
        } catch (e: Exception) {
            report.appendLine("\n❌ Activity traversal error: ${e.message}")
        }

        // === 4. Проходим по всему дереву View и заменяем BlurredBackgroundDrawable на ColorDrawable ===
        try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
            val mActivitiesField = activityThreadClass.getDeclaredField("mActivities")
            mActivitiesField.isAccessible = true
            val mActivities = mActivitiesField.get(activityThread)
            
            var viewsReplaced = 0
            
            if (mActivities is java.util.Map<*, *>) {
                for (record in mActivities.values()) {
                    if (record != null) {
                        try {
                            val activityField = record.javaClass.getDeclaredField("activity")
                            activityField.isAccessible = true
                            val activity = activityField.get(record) as? Activity
                            
                            if (activity != null && !activity.isFinishing) {
                                val decorView = activity.window.decorView
                                viewsReplaced += replaceBlurDrawables(decorView)
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
            
            if (viewsReplaced > 0) {
                successCount++
                report.appendLine("\n✅ Replaced $viewsReplaced blur drawables with solid color")
            } else {
                report.appendLine("\nℹ️ No blur drawables found to replace")
            }
        } catch (e: Exception) {
            report.appendLine("\n❌ View traversal error: ${e.message}")
        }

        report.appendLine("\n=== Summary ===")
        report.appendLine("Total successes: $successCount")
        
        lastResult = report.toString()
        Log.d(TAG, lastResult)
    }

    private fun replaceBlurDrawables(view: View): Int {
        var count = 0
        try {
            val background = view.background
            if (background != null) {
                val className = background.javaClass.name
                if (className.contains("Blurred") || 
                    className.contains("Blur") ||
                    className.contains("blur")) {
                    view.setBackgroundColor(Color.parseColor("#FF1C1C1E"))
                    count++
                }
            }
            
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    count += replaceBlurDrawables(view.getChildAt(i))
                }
            }
        } catch (e: Exception) {}
        return count
    }

    private fun handleActivity(activity: Activity, trigger: String) {
        val simpleName = activity.javaClass.simpleName
        Log.d(TAG, "Activity $trigger: $simpleName")
        
        try {
            if (simpleName.contains("ChatActivity") || simpleName.contains("Launch")) {
                activity.window.decorView.invalidate()
                activity.window.decorView.requestLayout()
           
