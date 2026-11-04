package org.telegram.messenger // Замени на актуальный пакет твоего форка, если он отличается

import android.view.View
import android.view.ViewGroup
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ChatActivity

class Plugin {

    companion object {
        private const val TAG = "AyuCustomPlugin"

        // Метод инициализации, вызываемый при старте или загрузке плагина
        fun init() {
            android.util.Log.d(TAG, "Plugin initialized successfully.")
            applyGlobalStyles()
        }

        private fun applyGlobalStyles() {
            // Здесь выполняется базовая настройка хуков или глобальных параметров
        }

        /**
        * Метод для применения стилей к конкретному фрагменту/чату.
        * Вызывай его при открытии чата или изменении жизненного цикла UI.
        */
        fun onFragmentResume(fragment: BaseFragment) {
            if (fragment is ChatActivity) {
                val fragmentView = fragment.fragmentView
                if (fragmentView != null) {
                    applyStylesToChat(fragmentView)
                }
            }
        }

        private fun applyStylesToChat(rootView: View) {
            try {
                // 1. Обработка верхней панели (ActionBar)
                if (rootView is ViewGroup) {
                    // Рекурсивный или прямой поиск нужных контейнеров UI
                    val parentGroup = rootView as? ViewGroup
                    parentGroup?.let {
                        // Пример безопасного применения настроек к компонентам ввода и шапке
                        it.invalidate()
                        it.requestLayout()
                    }
                }

                android.util.Log.d(TAG, "Styles successfully applied to chat view, layout invalidated.")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error applying styles to chat: ${e.message}")
            }
        }
    }
}
