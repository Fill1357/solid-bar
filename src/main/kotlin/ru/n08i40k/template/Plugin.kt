package ru.n08i40k.template

import android.util.Log
import android.webkit.ValueCallback
import android.widget.Toast
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.app.AlertDialog

object Plugin {
    private const val TAG = "SolidBarPlugin"
    private var appContext: Context? = null

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
            showToast("Plugin loaded! Making bars solid...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get context", e)
        }
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            makeBarsSolid()
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
    fun invokeSettingsActionCallback(key: String) {}

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

    private fun showDialog(title: String, message: String) {
        try {
            appContext?.let { ctx ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    AlertDialog.Builder(ctx)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Dialog error", e)
        }
    }

    private fun makeBarsSolid() {
        val context = appContext ?: run {
            Log.e(TAG, "No context available")
            return
        }

        var successCount = 0
        val results = StringBuilder()

        // Метод 1: ActionBar
        try {
            val actionBarClass = Class.forName("org.telegram.ui.ActionBar.ActionBar")
            val colorDrawable = ColorDrawable(Color.parseColor("#FF1C1C1E"))
            
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentActivityThread = activityThread.getMethod("currentActivityThread").invoke(null)
            val activitiesField = currentActivityThread.javaClass.getDeclaredField("mActivities")
            activitiesField.isAccessible = true
            val activities = activitiesField.get(currentActivityThread)
            
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
                                val actionBarField = activity.javaClass.getDeclaredField("actionBar")
                                actionBarField.isAccessible = true
                                val actionBar = actionBarField.get(activity)
                                
                                if (actionBar != null && actionBarClass.isInstance(actionBar)) {
                                    val setBackgroundMethod = actionBarClass.getMethod("setBackgroundDrawable", android.graphics.drawable.Drawable::class.java)
                                    setBackgroundMethod.invoke(actionBar, colorDrawable)
                                    successCount++
                                    results.append("ActionBar:OK ")
                                    Log.d(TAG, "Set solid background on ActionBar")
                                }
                            }
                        } catch (e: Exception) {
                            results.append("ActionBar:skip ")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            results.append("ActionBar:err(${e.message?.take(30)}) ")
            Log.e(TAG, "ActionBar error", e)
        }

        // Метод 2: ChatActivityEnterView
        try {
            val enterViewClass = Class.forName("org.telegram.ui.Components.ChatActivityEnterView")
            val colorDrawable = ColorDrawable(Color.parseColor("#FF1C1C1E"))
            
            val themeClass = Class.forName("org.telegram.messenger.Theme")
            val chatWallpaperField = themeClass.getDeclaredField("chat_wallpaper")
            chatWallpaperField.isAccessible = true
            chatWallpaperField.set(null, Color.parseColor("#FF1C1C1E"))
            successCount++
            results.append("Theme:chat_wallpaper:OK ")
            Log.d(TAG, "Set solid chat_wallpaper color")
        } catch (e: Exception) {
            results.append("EnterView:err(${e.message?.take(30)}) ")
            Log.e(TAG, "EnterView error", e)
        }

        // Метод 3: SharedConfig blur flags
        try {
            val sharedConfigClass = Class.forName("org.telegram.messenger.SharedConfig")
            for (field in sharedConfigClass.declaredFields) {
                val name = field.name.lowercase()
                if (name.contains("blur") || name.contains("glass")) {
                    field.isAccessible = true
                    if (field.type == Boolean::class.java || field.type == Boolean::class.javaPrimitiveType) {
                        try {
                            field.set(null, false)
                            successCount++
                            results.append("SC:${field.name}:OK ")
                        } catch (e: Exception) {
                            results.append("SC:${field.name}:err ")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            results.append("SC:err ")
        }

        // Метод 4: Принудительная перерисовка
        try {
            val activityThread
