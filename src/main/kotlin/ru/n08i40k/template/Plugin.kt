
package ru.n08i40k.template

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ValueCallback
import android.widget.Toast

object Plugin {
    private const val TAG = "SolidBarPlugin"

    private var appContext: Context? = null
    private var lastResult: String = "No data yet"

    @JvmStatic
    fun getBuildDate(): String = "2026-11-04"

    @JvmStatic
    fun inject(version: String, logger: ValueCallback<Any?>) {
        Log.d(TAG, "Solid Bar Plugin loaded! Version: $version")
        logger.onReceiveValue("Plugin loaded successfully")

        try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentApp = activityThread
                .getMethod("currentApplication")
                .invoke(null)

            appContext = currentApp as? Context

            showToast("Plugin loaded! Disabling blur...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get context", e)
            showToast("Error: " + e.message)
        }

        disableBlurFlags()

        Handler(Looper.getMainLooper()).postDelayed({
            forceRedraw()
            copyToClipboard(lastResult)
            showToast("Report copied! Paste it to see details")
        }, 3000)
    }

    @JvmStatic
    fun finalizeInject() {
        Log.d(TAG, "Solid Bar Plugin initialization complete")
    }

    @JvmStatic
    fun eject() {
        Log.d(TAG, "Solid Bar Plugin ejected")
    }

    @JvmStatic
    fun invokeChatContextMenuCallback(key: String, dialogId: Long) {
    }

    @JvmStatic
    fun invokeSettingsActionCallback(key: String) {
        if (key == "check_status") {
            copyToClipboard(lastResult)
            showToast("Debug report copied to clipboard!")
        }
    }

    private fun showToast(message: String) {
        try {
            appContext?.let { ctx ->
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        ctx,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Toast error", e)
        }
    }

    private fun copyToClipboard(text: String) {
        try {
            val clipboard = appContext?.getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as? ClipboardManager

            if (clipboard != null) {
                val clip = ClipData.newPlainText(
                    "SolidBar Debug",
                    text
                )

                clipboard.setPrimaryClip(clip)

                Log.d(TAG, "Copied to clipboard: ${text.length} chars")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Clipboard error", e)
        }
    }

    private fun disableBlurFlags() {
        val report = StringBuilder()

        report.appendLine("=== Solid Interface Debug ===")

        var successCount = 0

        try {
            val sharedConfigClass = Class.forName(
                "org.telegram.messenger.SharedConfig"
            )

            for (field in sharedConfigClass.declaredFields) {
                val name = field.name.lowercase()

                if (
                    name.contains("blur") ||
                    name.contains("glass") ||
                    name.contains("liquid")
                ) {
                    field.isAccessible = true

                    if (
                        field.type == Boolean::class.java ||
                        field.type == Boolean::class.javaPrimitiveType
                    ) {
                        try {
                            field.set(null, false)

                            successCount++

                            report.appendLine(
                                "Disabled: ${field.name}"
                            )

                            Log.d(TAG, "Disabled: ${field.name}")

                        } catch (e: Exception) {
                            report.appendLine(
                                "Failed: ${field.name} - ${e.message}"
                            )
                        }
                    }
                }
            }

        } catch (e: Exception) {
            report.appendLine(
                "Error reading SharedConfig: ${e.message}"
            )
        }

        report.appendLine(
            "Disabled fields: $successCount"
        )

        lastResult = report.toString()
    }

    private fun forceRedraw() {
        val report = StringBuilder(lastResult)

        report.appendLine("\n=== REDRAW PHASE ===")

        var redrawSuccess = 0
        var activitiesFound = 0

        try {
            val activityThreadClass = Class.forName(
                "android.app.ActivityThread"
            )

            val activityThread = activityThreadClass
                .getMethod("currentActivityThread")
                .invoke(null)

            val mActivitiesField = activityThreadClass
                .getDeclaredField("mActivities")

            mActivitiesField.isAccessible = true

            val mActivities = mActivitiesField.get(activityThread)

            // Исправленный перебор Java Map
            val activitiesMap = mActivities as? Map<*, *>

            if (activitiesMap != null) {

                for (entry in activitiesMap.entries) {

                    val record = entry.value

                    if (record != null) {
                        try {
                            val activityField = record.javaClass
                                .getDeclaredField("activity")

                            activityField.isAccessible = true

                            val activity = activityField.get(record)
                                as? Activity

                            if (
                                activity != null &&
                                !activity.isFinishing
                            ) {
                                activitiesFound++

                                val activityName =
                                    activity.javaClass.simpleName

                                report.appendLine(
                                    "\nActivity #$activitiesFound: $activityName"
                                )

                                var actionBarFound = false

                                // Поиск ActionBar через рефлексию
                                try {
                                    val actionBarField =
                                        activity.javaClass.getDeclaredField(
                                            "actionBar"
                                        )

                                    actionBarField.isAccessible = true

                                    val actionBar =
                                        actionBarField.get(activity)

                                    if (actionBar != null) {
                                        actionBarFound = true

                                        val colorDrawable =
                                            android.graphics.drawable.ColorDrawable(
                                                android.graphics.Color.parseColor(
                                                    "#FF1C1C1E"
                                                )
                                            )

                                        val setBackgroundMethod =
                                            actionBar.javaClass.getMethod(
                                                "setBackgroundDrawable",
                                                android.graphics.drawable.Drawable::class.java
                                            )

                                        setBackgroundMethod.invoke(
                                            actionBar,
                                            colorDrawable
                                        )

                                        redrawSuccess++

                                        report.appendLine(
                                            "ActionBar background set to solid"
                                        )
                                    }

                                } catch (e: Exception) {
                                    report.appendLine(
                                        "No actionBar field: ${e.message}"
                                    )
                                }

                                // Запасной способ через Android API
                                if (!actionBarFound) {
                                    try {
                                        val actionBar = activity.actionBar

                                        if (actionBar != null) {
                                            val colorDrawable =
                                                android.graphics.drawable.ColorDrawable(
                                                    android.graphics.Color.parseColor(
                                                        "#FF1C1C1E"
                                                    )
                                                )

                                            actionBar.setBackgroundDrawable(
                                                colorDrawable
                                            )

                                            redrawSuccess++

                                            report.appendLine(
                                                "ActionBar set via getActionBar()"
                                            )
                                        }

                                    } catch (e: Exception) {
                                        report.appendLine(
                                            "Android ActionBar unavailable"
                                        )
                                    }
                                }

                                // Перерисовка DecorView
                                try {
                                    activity.window.decorView.invalidate()
                                    activity.window.decorView.requestLayout()

                                    redrawSuccess++

                                    report.appendLine(
                                        "DecorView redrawn"
                                    )

                                } catch (e: Exception) {
                                    report.appendLine(
                                        "Redraw failed: ${e.message}"
                                    )
                                }
                            }

                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Activity processing error",
                                e
                            )
                        }
                    }
                }

            } else {
                report.appendLine(
                    "Activity map unavailable or has unexpected type"
                )
            }

            if (activitiesFound == 0) {
                report.appendLine(
                    "No active activities found"
                )
            } else {
                report.appendLine(
                    "Processed $activitiesFound activities"
                )
            }

        } catch (e: Exception) {
            report.appendLine(
                "Error: ${e.message?.take(100)}"
            )

            Log.e(TAG, "forceRedraw error", e)
        }

        report.appendLine("\n=== SUMMARY ===")

        report.appendLine(
            "Total redraw successes: $redrawSuccess"
        )

        lastResult = report.toString()

        Log.d(TAG, lastResult)
    }
}
