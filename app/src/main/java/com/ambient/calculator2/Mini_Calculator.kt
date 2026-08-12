package com.ambient.calculator2

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class Mini_Calculator : AppWidgetProvider() {

    companion object {
        private var currentInput = ""
        private const val ACTION_BUTTON_CLICK = "com.ambient.calculator2.ACTION_BUTTON_CLICK"
        private const val EXTRA_KEY = "extra_key"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_BUTTON_CLICK) {
            val key = intent.getStringExtra(EXTRA_KEY) ?: return
            handleKey(key)
            
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, Mini_Calculator::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun handleKey(key: String) {
        when (key) {
            "C" -> currentInput = ""
            "⌫" -> if (currentInput.isNotEmpty()) currentInput = currentInput.dropLast(1)
            "=" -> {
                if (currentInput.isNotEmpty()) {
                    try {
                        val result = ExpressionEvaluator(currentInput).evaluate()
                        currentInput = formatValue(result)
                    } catch (e: Exception) {
                        currentInput = "Error"
                    }
                }
            }
            else -> {
                if (currentInput == "Error") currentInput = ""
                currentInput += key
            }
        }
    }

    private fun formatValue(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            "%.6f".format(value).trimEnd('0').trimEnd('.')
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.mini__calculator)
        views.setTextViewText(R.id.widget_display, if (currentInput.isEmpty()) "0" else currentInput)

        val keys = listOf(
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "+", "-", "*", "/", ".", "%", "C", "⌫", "="
        )
        val buttonIds = mapOf(
            "0" to R.id.btn_0, "1" to R.id.btn_1, "2" to R.id.btn_2, "3" to R.id.btn_3,
            "4" to R.id.btn_4, "5" to R.id.btn_5, "6" to R.id.btn_6, "7" to R.id.btn_7,
            "8" to R.id.btn_8, "9" to R.id.btn_9, "+" to R.id.btn_add, "-" to R.id.btn_sub,
            "*" to R.id.btn_mul, "/" to R.id.btn_div, "." to R.id.btn_dot, "%" to R.id.btn_percent,
            "C" to R.id.btn_c, "⌫" to R.id.btn_back, "=" to R.id.btn_eq
        )

        for ((key, id) in buttonIds) {
            val intent = Intent(context, Mini_Calculator::class.java).apply {
                action = ACTION_BUTTON_CLICK
                putExtra(EXTRA_KEY, key)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, id, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(id, pendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
