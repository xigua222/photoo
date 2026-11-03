package com.voicelike.app.widgetlikes

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.voicelike.app.R
import kotlin.math.roundToInt

class LikesPhotoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { appWidgetId ->
            LikesWidgetStore.clearSelection(context, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {
        private const val REQUEST_CODE_BASE = 5100

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, LikesPhotoWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            ids.forEach { appWidgetId ->
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_likes_photo)
            val clickPendingIntent = createPickerPendingIntent(context, appWidgetId)
            remoteViews.setOnClickPendingIntent(R.id.widget_likes_root, clickPendingIntent)
            remoteViews.setOnClickPendingIntent(R.id.widget_likes_image, clickPendingIntent)
            remoteViews.setOnClickPendingIntent(R.id.widget_likes_empty_text, clickPendingIntent)

            val selectedUri = LikesWidgetStore.getSelectedUri(context, appWidgetId)?.let(Uri::parse)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val (targetWidthPx, targetHeightPx) = resolveTargetSize(context, options)
            val bitmap = selectedUri?.let {
                LikesWidgetBitmapLoader.load(context, it, targetWidthPx, targetHeightPx)
            }

            if (bitmap != null) {
                remoteViews.setImageViewBitmap(R.id.widget_likes_image, bitmap)
                remoteViews.setViewVisibility(R.id.widget_likes_empty_text, View.GONE)
            } else {
                if (selectedUri != null) {
                    LikesWidgetStore.clearSelection(context, appWidgetId)
                }
                remoteViews.setImageViewResource(R.id.widget_likes_image, R.drawable.widget_likes_placeholder)
                remoteViews.setViewVisibility(R.id.widget_likes_empty_text, View.VISIBLE)
                remoteViews.setTextViewText(
                    R.id.widget_likes_empty_text,
                    context.getString(R.string.widget_likes_empty)
                )
            }

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }

        private fun createPickerPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, LikesWidgetPickerActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getActivity(
                context,
                REQUEST_CODE_BASE + appWidgetId,
                intent,
                flags
            )
        }

        private fun resolveTargetSize(
            context: Context,
            options: android.os.Bundle
        ): Pair<Int, Int> {
            val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160)
            val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160)
            val maxWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidthDp)
            val maxHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeightDp)
            val effectiveWidthDp = maxOf(minWidthDp, maxWidthDp)
            val effectiveHeightDp = maxOf(minHeightDp, maxHeightDp)
            val density = context.resources.displayMetrics.density
            // Ask decoder for a slightly larger bitmap to avoid launcher-side blur after scaling.
            val qualityMultiplier = 1.35f
            val widthPx = (effectiveWidthDp * density * qualityMultiplier).roundToInt().coerceAtLeast(480)
            val heightPx = (effectiveHeightDp * density * qualityMultiplier).roundToInt().coerceAtLeast(480)
            return widthPx to heightPx
        }
    }
}
