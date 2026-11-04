package ru.n08i40k.template

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.ValueCallback
import android.widget.Toast

object Plugin {
    private const val TAG = "SolidBarPlugin"
    private var appContext: Context? = null
    private var lastResult: String = "No data yet"

    private var appliedActionBar: Any? = null
    private var appliedContentView: Any? = null
    private var appliedEnterView: Any? = null
    private var previousBlurredBackground: Boolean? = null
    private var previousActionBarColor: Int? = null
    private var previousAllowBlur: Boolean? = null

    @JvmStatic
    fun getBuildDate(): String = "2026-11-04"

    @JvmStatic
    fun inject(version: String, logger: ValueCallback<Any?>) {
        Log.d(TAG, "Solid Bar Plugin loaded! Version: $version")
        logger.onReceiveValue("Plugin loaded successfully")
        try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentApp = activityThread.getMethod("currentApplication").invoke(null)
            appContext = currentApp as? Context
            showToast("Solid Interface loaded")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get application context", e)
        }

        // If the plugin is loaded while a chat is already open, apply immediately.
        Handler(Looper.getMainLooper()).postDelayed({
            applyToCurrentChat()
        }, 700)
    }

    @JvmStatic
    fun finalizeInject() {
        Log.d(TAG, "Solid Bar Plugin initialization complete")
    }

    @JvmStatic
    fun eject() {
        Handler(Looper.getMainLooper()).post { restoreLastChat() }
        Log.d(TAG, "Solid Bar Plugin ejected")
    }

    @JvmStatic
    fun invokeChatContextMenuCallback(key: String, dialogId: Long) {
        if (key == "apply_classic") {
            Handler(Looper.getMainLooper()).post {
                val success = applyToCurrentChat()
                showToast(
                    if (success) "Classic chat bars applied"
                    else "Open a chat and try again"
                )
            }
        }
    }

    @JvmStatic
    fun invokeSettingsActionCallback(key: String) {
        if (key == "check_status") {
            copyToClipboard(lastResult)
            showToast("Debug report copied to clipboard")
        } else if (key == "apply_classic") {
            Handler(Looper.getMainLooper()).post {
                val success = applyToCurrentChat()
                showToast(if (success) "Classic chat bars applied" else "Open a chat and try again")
            }
        }
    }

    private fun showToast(message: String) {
        try {
            appContext?.let { ctx ->
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Toast error", e)
        }
    }

    private fun copyToClipboard(text: String) {
        try {
            val clipboard = appContext?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Solid Interface", text))
        } catch (e: Exception) {
            Log.e(TAG, "Clipboard error", e)
        }
    }

    private fun findField(type: Class<*>, name: String): java.lang.reflect.Field? {
        var current: Class<*>? = type
        while (current != null) {
            try {
                return current.getDeclaredField(name).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
                current = current.superclass
            }
        }
        return null
    }

    private fun findMethod(type: Class<*>, name: String): java.lang.reflect.Method? {
        try {
            return type.getMethod(name).apply { isAccessible = true }
        } catch (_: NoSuchMethodException) {
            // Fall back to non-public methods declared in the class hierarchy.
        }
        var current: Class<*>? = type
        while (current != null) {
            try {
                return current.getDeclaredMethod(name).apply { isAccessible = true }
            } catch (_: NoSuchMethodException) {
                current = current.superclass
            }
        }
        return null
    }

    private fun currentChatFragment(): Any? {
        val threadClass = Class.forName("android.app.ActivityThread")
        val thread = threadClass.getMethod("currentActivityThread").invoke(null)
        val activitiesField = findField(threadClass, "mActivities") ?: return null
        val activities = activitiesField.get(thread) as? Map<*, *> ?: return null

        for (record in activities.values) {
            if (record == null) continue
            val activityField = findField(record.javaClass, "activity") ?: continue
            val activity = activityField.get(record) as? Activity ?: continue
            if (activity.isFinishing || activity.javaClass.name != "org.telegram.ui.LaunchActivity") continue

            val layoutField = findField(activity.javaClass, "actionBarLayout") ?: continue
            val layout = layoutField.get(activity) ?: continue
            val getLastFragment = findMethod(layout.javaClass, "getLastFragment") ?: continue
            val fragment = getLastFragment.invoke(layout) ?: continue
            if (fragment.javaClass.name == "org.telegram.ui.ChatActivity") return fragment
        }
        return null
    }

    private fun applyToCurrentChat(): Boolean {
        val report = StringBuilder()
        report.appendLine("=== Solid Interface ===")
        report.appendLine("Target: current ChatActivity")
        try {
            val chat = currentChatFragment()
            if (chat == null) {
                report.appendLine("No active ChatActivity found")
                lastResult = report.toString()
                Log.i(TAG, lastResult)
                return false
            }

            val getActionBar = findMethod(chat.javaClass, "getActionBar")
            val actionBar = getActionBar?.invoke(chat)
            var topApplied = false
            var bottomApplied = false

            if (actionBar != null) {
                val blurredField = findField(actionBar.javaClass, "blurredBackground")
                val colorField = findField(actionBar.javaClass, "actionBarColor")
                val contentField = findField(actionBar.javaClass, "contentView")

                if (blurredField != null && colorField != null) {
                    // Keep the theme's own ActionBar color; only replace the blur layer.
                    val wasBlurred = blurredField.getBoolean(actionBar)
                    val color = colorField.getInt(actionBar)
                    val contentView = contentField?.get(actionBar)

                    if (appliedActionBar !== actionBar) {
                        appliedActionBar = actionBar
                        appliedContentView = contentView
                        previousBlurredBackground = wasBlurred
                        previousActionBarColor = color
                    }

                    blurredField.setBoolean(actionBar, false)
                    actionBar.javaClass.getMethod("setBackgroundColor", Int::class.javaPrimitiveType)
                        .invoke(actionBar, color)

                    // Remove the ActionBar from the blur-behind drawing list.
                    if (contentView != null) {
                        val viewsField = findField(contentView.javaClass, "blurBehindViews")
                        val views = viewsField?.get(contentView)
                        if (views is MutableCollection<*>) {
                            @Suppress("UNCHECKED_CAST")
                            (views as MutableCollection<Any?>).remove(actionBar)
                        }
                    }
                    (actionBar as? View)?.invalidate()
                    topApplied = true
                    report.appendLine("Top bar: blur disabled; theme color preserved")
                } else {
                    report.appendLine("Top bar: required fields not found")
                }
            } else {
                report.appendLine("Top bar: ActionBar unavailable")
            }

            val getEnterView = findMethod(chat.javaClass, "getChatActivityEnterView")
            val enterView = getEnterView?.invoke(chat)
            if (enterView != null) {
                val allowBlurField = findField(enterView.javaClass, "allowBlur")
                if (allowBlurField != null) {
                    val oldValue = allowBlurField.getBoolean(enterView)
                    if (appliedEnterView !== enterView) {
                        appliedEnterView = enterView
                        previousAllowBlur = oldValue
                    }
                    allowBlurField.setBoolean(enterView, false)
                    (enterView as? View)?.invalidate()
                    (enterView as? View)?.requestLayout()
                    bottomApplied = true
                    report.appendLine("Bottom bar: blur disabled; themed compose paint preserved")
                } else {
                    report.appendLine("Bottom bar: allowBlur field not found")
                }
            } else {
                report.appendLine("Bottom bar: ChatActivityEnterView unavailable")
            }

            val success = topApplied && bottomApplied
            report.appendLine("Result: " + if (success) "both bars updated" else "partial update")
            lastResult = report.toString()
            Log.i(TAG, lastResult)
            return success
        } catch (e: Exception) {
            report.appendLine("Error: ${e.javaClass.simpleName}: ${e.message}")
            lastResult = report.toString()
            Log.e(TAG, "Failed to apply classic chat bars", e)
            return false
        }
    }

    private fun restoreLastChat() {
        try {
            val actionBar = appliedActionBar
            val contentView = appliedContentView
            if (actionBar != null) {
                val wasBlurred = previousBlurredBackground ?: false
                val color = previousActionBarColor
                if (wasBlurred && contentView != null) {
                    actionBar.javaClass.getMethod(
                        "setDrawBlurBackground",
                        Class.forName("org.telegram.ui.Components.SizeNotifierFrameLayout")
                    ).invoke(actionBar, contentView)
                } else if (color != null) {
                    actionBar.javaClass.getMethod("setBackgroundColor", Int::class.javaPrimitiveType)
                        .invoke(actionBar, color)
                }
                (actionBar as? View)?.invalidate()
            }

            val enterView = appliedEnterView
            if (enterView != null) {
                val allowBlurField = findField(enterView.javaClass, "allowBlur")
                val oldValue = previousAllowBlur
                if (allowBlurField != null && oldValue != null) {
                    allowBlurField.setBoolean(enterView, oldValue)
                    (enterView as? View)?.invalidate()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore chat bars", e)
        } finally {
            appliedActionBar = null
            appliedContentView = null
            appliedEnterView = null
            previousBlurredBackground = null
            previousActionBarColor = null
            previousAllowBlur = null
        }
    }
}
