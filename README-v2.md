# Solid Interface v2

## Changes
- Adds a main-thread monitor that checks the active ChatActivity every 450 ms.
- Reapplies the classic bar settings when AyuGram creates/replaces a chat screen.
- For the ActionBar, disables `blurredBackground`, removes the ActionBar from `contentView.blurBehindViews`, and calls the ActionBar's own `setBackgroundColor` method with its existing theme color.
- For ChatActivityEnterView, sets `allowBlur=false` and invalidates the view.
- Stores original values and attempts to restore them when the plugin is unloaded.
- Does not modify chat wallpaper, message bubbles, or chat content backgrounds.

## Important
The monitor is a compatibility workaround for the app calling `setDrawBlurBackground()` during ChatActivity creation. The plugin cannot guarantee removal of decorative circular button backgrounds; those are separate controls/drawables and must be identified from a screenshot before changing them.
