package ru.n08i40k.template

import android.util.Log
import android.webkit.ValueCallback
import android.widget.Toast
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface

object Plugin {
    private const val TAG = "SolidBarPlugin"
    private var appContext: Context? = null
    private var lastResult: String = "No data yet. Click 'Check Plugin Status' in settings."

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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get context", e)
        }
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            makeBarsSilent()
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
            showStatusDialog()
        }
    }

    private fun showStatusDialog() {
        try {
            appContext?.let { ctx ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    val builder = AlertDialog.Builder(ctx)
                    builder.setTitle("Solid Interface - Status")
                    builder.setMessage(lastResult)
                    
                    builder.setPositiveButton("Copy", object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            try {
                                val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("SolidBar Debug", lastResult)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(ctx, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e(TAG, "Clipboard error", e)
                            }
                        }
                    })
                    
                    builder.setNegativeButton("Close", null)
                    
                    builder.show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Dialog error", e)
        }
    }

    private fun makeBarsSilent() {
        val context = appContext ?: run {
            lastResult = "Error: No context available"
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

        // Метод 2: Theme
        try {
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

        // Метод 3: SharedConfig
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

        // Метод 4: Redraw
        try {
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
                                val contentView = (activity as android.app.Activity).findViewById<View>(android.R.id.content)
                                contentView?.invalidate()
                                contentView?.requestLayout()
                                results.append("Redraw:OK ")
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            results.append("Redraw:err ")
        }

        lastResult = "Success: $successCount\n\n$results"
        Log.d(TAG, lastResult)
    }
}
