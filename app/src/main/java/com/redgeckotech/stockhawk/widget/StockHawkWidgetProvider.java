package com.redgeckotech.stockhawk.widget;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.redgeckotech.stockhawk.R;
import com.redgeckotech.stockhawk.ui.MyStocksActivity;
import com.redgeckotech.stockhawk.ui.StockDetailsActivity;

public class StockHawkWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_list);

            // Create intent to launch MainActivity
            Intent titleClickIntent = new Intent(context, MyStocksActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, titleClickIntent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            views.setRemoteAdapter(R.id.stocks_listview,
                    new Intent(context, StockHawkRemoteViewsService.class));

            // Set up collection items
            Intent symbolClickIntent = new Intent(context, StockDetailsActivity.class);
            PendingIntent symbolClickPendingIntent = TaskStackBuilder.create(context)
                    .addParentStack(MyStocksActivity.class)
                    .addParentStack(StockDetailsActivity.class)
                    .addNextIntentWithParentStack(symbolClickIntent)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.stocks_listview, symbolClickPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
