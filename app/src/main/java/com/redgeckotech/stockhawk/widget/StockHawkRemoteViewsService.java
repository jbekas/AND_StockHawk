package com.redgeckotech.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.redgeckotech.stockhawk.R;
import com.redgeckotech.stockhawk.data.QuoteColumns;
import com.redgeckotech.stockhawk.data.QuoteProvider;
import com.redgeckotech.stockhawk.rest.Utils;

public class StockHawkRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor cursor = null;

            @Override
            public void onCreate() {
                onDataSetChanged();
            }

            @Override
            public void onDataSetChanged() {

                if (cursor != null) {
                    cursor.close();
                }

                final long identityToken = Binder.clearCallingIdentity();

                cursor = getContentResolver().query(
                        QuoteProvider.Quotes.CONTENT_URI,
                        new String[] {
                                QuoteColumns._ID,
                                QuoteColumns.SYMBOL,
                                QuoteColumns.BIDPRICE,
                                QuoteColumns.PERCENT_CHANGE,
                                QuoteColumns.CHANGE,
                                QuoteColumns.ISUP
                        },
                        QuoteColumns.ISCURRENT + " = ?",
                        new String[]{"1"},
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() { }

            @Override
            public int getCount() {
                return cursor == null ? 0 : cursor.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        cursor == null || !cursor.moveToPosition(position)) {
                    return null;
                }

                RemoteViews views = new RemoteViews(getPackageName(), R.layout.list_item_widget);

                views.setTextViewText(R.id.stock_symbol,
                        cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL)));

                // Update color indicator for positive or negative change
                if (cursor.getInt(cursor.getColumnIndex(QuoteColumns.ISUP)) == 1) {
                    views.setInt(R.id.change,
                            getResources().getString(R.string.set_background_resource),
                            R.drawable.percent_change_pill_green);
                } else {
                    views.setInt(R.id.change,
                            getResources().getString(R.string.set_background_resource),
                            R.drawable.percent_change_pill_red);
                }

                String percentChange = cursor.getString(cursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE));
                String change = cursor.getString(cursor.getColumnIndex(QuoteColumns.CHANGE));
                views.setTextViewText(R.id.change, Utils.showPercent ? percentChange : change);

                final Intent intent = new Intent();
                intent.putExtra("symbol", cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL)));
                views.setOnClickFillInIntent(R.id.widget_list_item, intent);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return null;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (cursor != null && cursor.moveToPosition(position)) {
                    return cursor.getLong(cursor.getColumnIndex(QuoteColumns._ID));
                }
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}