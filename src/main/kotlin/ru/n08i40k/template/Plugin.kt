
    
  

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
        return try {
            // AyuGram exposes the currently visible fragment through this static method.
            // Do not inspect ActivityThread.mActivities/actionBarLayout: that lookup
            // is unreliable in this AyuGram build and was the source of
            // "No active ChatActivity found".
            val launchActivityClass = Class.forName("org.telegram.ui.LaunchActivity")
            val getLastFragment = launchActivityClass.getMethod("getLastFragment")
            val fragment = getLastFragment.invoke(null) ?: return null

            val chatActivityClass = Class.forName("org.telegram.ui.ChatActivity")
            if (chatActivityClass.isInstance(fragment)) fragment else null
        } catch (e: Exception) {
            Log.e(TAG, "Cannot obtain LaunchActivity.getLastFragment()", e)
            null
        }
    }

    private fun applyToCurrentChat(reason: String): Boolean {
        val report = StringBuilder()
        report.appendLine("=== Solid Interface ===")
        report.appendLine("Mode: monitor + $reason")
        report.appendLine("Target: current ChatActivity")

        try {
            val chat = currentChatFragment()
            if (chat == null) {
                report.appendLine("No active ChatActivity found via LaunchActivity.getLastFragment()")
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
                    val itemsColorField = findField(actionBar.javaClass, "itemsBackgroundColor")
                    val oldItemsBackgroundColor = itemsColorField?.getInt(actionBar)

                    if (!actionBarStates.containsKey(actionBar)) {
                        actionBarStates[actionBar] = ActionBarState(
                            actionBar, contentView, oldBlur, color, oldItemsBackgroundColor
                        )
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

                    // Keep the buttons and their icons, but make their selector backgrounds
                    // exactly the same color as the ActionBar so the circular patches blend in.
                    val setItemsBackgroundColor = findMethod(
                        actionBar.javaClass,
                        "setItemsBackgroundColor",
                        Int::class.javaPrimitiveType!!,
                        Boolean::class.javaPrimitiveType!!
                    )
                    if (setItemsBackgroundColor != null) {
                        setItemsBackgroundColor.invoke(actionBar, color, false)
                        report.appendLine("Top buttons: selector background matched to ActionBar")
                    } else {
                        report.appendLine("Top buttons: setItemsBackgroundColor not found")
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

            report.appendLine("Bottom bar: intentionally untouched in this build")

            report.appendLine("Result: top=$topApplied; bottom=not modified")
            lastResult = report.toString()
            Log.i(TAG, lastResult)
            return topApplied
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
                if (state.oldItemsBackgroundColor != null) {
                    val setItemsBackgroundColor = findMethod(
                        actionBar.javaClass,
                        "setItemsBackgroundColor",
                        Int::class.javaPrimitiveType!!,
                        Boolean::class.javaPrimitiveType!!
                    )
                    setItemsBackgroundColor?.invoke(
                        actionBar, state.oldItemsBackgroundColor, false
                    )
                }
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

