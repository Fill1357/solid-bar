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
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.IdentityHashMap

object Plugin {
    private const val TAG = "SolidBarPlugin"
    private const val POLL_INTERVAL_MS = 450L

    private var appContext: Context? = null
    private var lastResult: String = "No data yet"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var monitoring = false

    private data class ActionBarState(
        val actionBar: Any,
        val contentView: Any?,
        val wasBlurred: Boolean,
        val color: Int
    )

    private data class EnterViewState(
        val enterView: Any,
        val allowBlur: Boolean
    )

    // Keep original values for every chat screen touched while the plugin is loaded.
    private val actionBarStates = IdentityHashMap<Any, ActionBarState>()
    private val enterViewStates = IdentityHashMap<Any, EnterViewState>()

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (!monitoring) return
            try {
                applyToCurrentChat("automatic")
            } catch (e: Exception) {
                Log.e(TAG, "Automatic chat-bar update failed", e)
            }
            mainHandler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    @JvmStatic
    fun getBuildDate(): String = "2026-11-06"

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
        startMonitoring()
    }

    @JvmStatic
    fun finalizeInject() {
        Log.d(TAG, "Solid Bar Plugin initialization complete")
    }

    @JvmStatic
    fun eject() {
        monitoring = false
        mainHandler.removeCallbacks(monitorRunnable)
        mainHandler.post { restoreAllTouchedChats() }
        Log.d(TAG, "Solid Bar Plugin ejected")
    }

    @JvmStatic
    fun invokeChatContextMenuCallback(key: String, dialogId: Long) {
        if (key == "apply_classic") {
            mainHandler.post {
                val success = applyToCurrentChat("manual")
                showToast(if (success) "Classic chat bars applied" else "Open a chat and try again")
            }
        }
    }

    @JvmStatic
    fun invokeSettingsActionCallback(key: String) {
        when (key) {
            "check_status" -> {
                copyToClipboard(lastResult)
                showToast("Debug report copied to clipboard")
            }
            "apply_classic" -> mainHandler.post {
                val success = applyToCurrentChat("manual")
                showToast(if (success) "Classic chat bars applied" else "Open a chat and try again")
            }
        }
    }

    private fun startMonitoring() {
        if (monitoring) return
        monitoring = true
        mainHandler.removeCallbacks(monitorRunnable)
        mainHandler.post(monitorRunnable)
        Log.i(TAG, "ChatActivity monitor started (${POLL_INTERVAL_MS}ms)")
    }

    private fun showToast(message: String) {
        try {
            appContext?.let { ctx ->
                mainHandler.post {
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

    private fun findField(type: Class<*>, name: String): Field? {
        var current: Class<*>? = type
        while (current != null) {
            try {
                return current.getDeclaredField(name).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
                current = current.superclass
            } catch (e: Exception) {
                Log.w(TAG, "Cannot access field $name on ${current.name}", e)
                return null
            }
        }
        return null
    }

    private fun findMethod(type: Class<*>, name: String, vararg parameterTypes: Class<*>): Method? {
        try {
            return type.getMethod(name, *parameterTypes).apply { isAccessible = true }
        } catch (_: NoSuchMethodException) {
            var current: Class<*>? = type
            while (current != null) {
                try {
                    return current.getDeclaredMethod(name, *parameterTypes).apply { isAccessible = true }
                } catch (_: NoSuchMethodException) {
                    current = current.superclass
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot access method $name on ${current.name}", e)
                    return null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot access method $name", e)
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
            if (activity.isFinishing || activity.isDestroyed ||
                activity.javaClass.name != "org.telegram.ui.LaunchActivity") continue

            val layoutField = findField(activity.javaClass, "actionBarLayout") ?: continue
            val layout = layoutField.get(activity) ?: continue
            val getLastFragment = findMethod(layout.javaClass, "getLastFragment") ?: continue
            val fragment = getLastFragment.invoke(layout) ?: continue
            if (fragment.javaClass.name == "org.telegram.ui.ChatActivity") return fragment
        }
        return null
    }

    private fun applyToCurrentChat(reason: String): Boolean {
        val report = StringBuilder()
        report.appendLine("=== Solid Interface ===")
        report.appendLine("Mode: monitor + $reason")
        report.appendLine("Target: current ChatActivity")

        try {
            val chat = currentChatFragment()
            if (chat == null) {
                report.appendLine("No active ChatActivity found")
                lastResult = report.toString()
                return false
            }

            var topApplied = false
            var bottomApplied = false

            val actionBar = findMethod(chat.javaClass, "getActionBar")?.invoke(chat)
            if (actionBar != null) {
                val blurredField = findField(actionBar.javaClass, "blurredBackground")
                val colorField = findField(actionBar.javaClass, "actionBarColor")
                val contentField = findField(actionBar.javaClass, "contentView")

                if (blurredField != null && colorField != null) {
                    val oldBlur = blurredField.getBoolean(actionBar)
                    val color = colorField.getInt(actionBar)
                    val contentView = contentField?.get(actionBar)

                    if (!actionBarStates.containsKey(actionBar)) {
                        actionBarStates[actionBar] = ActionBarState(actionBar, contentView, oldBlur, color)
                    }

                    // ActionBar.setDrawBlurBackground() is called by ChatActivity during construction.
                    // Turn off its flag, remove it from the blur-behind list, and restore a real solid
                    // background using ActionBar.setBackgroundColor(), which also updates actionBarColor.
                    blurredField.setBoolean(actionBar, false)

                    val blurViewsField = contentView?.let { findField(it.javaClass, "blurBehindViews") }
                    val blurViews = blurViewsField?.get(contentView)
                    if (blurViews is MutableCollection<*>) {
                        @Suppress("UNCHECKED_CAST")
                        (blurViews as MutableCollection<Any?>).remove(actionBar)
                    }

                    val setColor = findMethod(actionBar.javaClass, "setBackgroundColor", Int::class.javaPrimitiveType!!)
                    if (setColor != null) {
                        setColor.invoke(actionBar, color)
                    } else {
                        (actionBar as? View)?.setBackgroundColor(color)
                    }
                    (actionBar as? View)?.invalidate()
                    (actionBar as? View)?.postInvalidateOnAnimation()

                    val blurStillEnabled = blurredField.getBoolean(actionBar)
                    val background = (actionBar as? View)?.background
                    topApplied = !blurStillEnabled && background != null
                    report.appendLine(
                        "Top bar: blurredBackground=${blurredField.getBoolean(actionBar)}, " +
                            "background=${background?.javaClass?.simpleName ?: "null"}, verified=$topApplied"
                    )
                } else {
                    report.appendLine("Top bar: required fields not found")
                }
            } else {
                report.appendLine("Top bar: getActionBar() unavailable")
            }

            val enterView = findMethod(chat.javaClass, "getChatActivityEnterView")?.invoke(chat)
            if (enterView != null) {
                val allowBlurField = findField(enterView.javaClass, "allowBlur")
                if (allowBlurField != null) {
                    val oldValue = allowBlurField.getBoolean(enterView)
                    if (!enterViewStates.containsKey(enterView)) {
                        enterViewStates[enterView] = EnterViewState(enterView, oldValue)
                    }
                    allowBlurField.setBoolean(enterView, false)
                    (enterView as? View)?.invalidate()
                    (enterView as? View)?.postInvalidateOnAnimation()
                    (enterView as? View)?.requestLayout()

                    val verified = !allowBlurField.getBoolean(enterView)
                    bottomApplied = verified
                    report.appendLine("Bottom bar: allowBlur=${allowBlurField.getBoolean(enterView)}, verified=$verified")
                } else {
                    report.appendLine("Bottom bar: allowBlur field not found")
                }
            } else {
                report.appendLine("Bottom bar: getChatActivityEnterView() unavailable")
            }

            report.appendLine("Result: top=$topApplied; bottom=$bottomApplied")
            lastResult = report.toString()
            Log.i(TAG, lastResult)
            return topApplied && bottomApplied
        } catch (e: Exception) {
            report.appendLine("Error: ${e.javaClass.simpleName}: ${e.message}")
            lastResult = report.toString()
            Log.e(TAG, "Failed to apply classic chat bars", e)
            return false
        }
    }

    private fun restoreAllTouchedChats() {
        try {
            for (state in actionBarStates.values) {
                val actionBar = state.actionBar
                val blurredField = findField(actionBar.javaClass, "blurredBackground")
                if (blurredField != null) blurredField.setBoolean(actionBar, state.wasBlurred)
                if (state.wasBlurred && state.contentView != null) {
                    val content = state.contentView
                    val method = findMethod(
                        actionBar.javaClass,
                        "setDrawBlurBackground",
                        Class.forName("org.telegram.ui.Components.SizeNotifierFrameLayout")
                    )
                    method?.invoke(actionBar, content)
                } else {
                    val setColor = findMethod(actionBar.javaClass, "setBackgroundColor", Int::class.javaPrimitiveType!!)
                    setColor?.invoke(actionBar, state.color)
                }
                (actionBar as? View)?.invalidate()
            }

            for (state in enterViewStates.values) {
                val field = findField(state.enterView.javaClass, "allowBlur")
                if (field != null) field.setBoolean(state.enterView, state.allowBlur)
                (state.enterView as? View)?.invalidate()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore chat bars", e)
        } finally {
            actionBarStates.clear()
            enterViewStates.clear()
        }
    }
}
