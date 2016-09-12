package com.redgeckotech.stockhawk.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.redgeckotech.stockhawk.R;
import com.redgeckotech.stockhawk.data.HistoricalQuoteColumns;
import com.redgeckotech.stockhawk.data.QuoteColumns;
import com.redgeckotech.stockhawk.data.QuoteProvider;
import com.redgeckotech.stockhawk.service.StockIntentService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class StockDetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int CURSOR_LOADER_ID = 0;
    private Cursor cursor;
    private LineChartView lineChartView;
    private int maxRange;
    private int minRange;

    private Intent mServiceIntent;
    private boolean isConnected;

    private SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat sdf2 = new SimpleDateFormat("M/d", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_line_graph);

        lineChartView = (LineChartView) findViewById(R.id.linechart);

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        mServiceIntent = new Intent(this, StockIntentService.class);
        if (savedInstanceState == null) {
            // Run the initialize task service so that some stocks appear upon an empty database
            Calendar cal = Calendar.getInstance();
            String endDate = sdf1.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, -14);
            String startDate = sdf1.format(cal.getTime());

            mServiceIntent.putExtra("tag", "historical");
            mServiceIntent.putExtra("symbol", getIntent().getStringExtra("symbol"));
            mServiceIntent.putExtra("startDate", startDate);
            mServiceIntent.putExtra("endDate", endDate);
            if (isConnected) {
                startService(mServiceIntent);
            } else {
                networkToast();
            }
        }

        Intent intent = getIntent();
        String symbol = intent.getStringExtra("symbol");
        Bundle args = new Bundle();
        args.putString("symbol", symbol);
        getLoaderManager().initLoader(CURSOR_LOADER_ID, args, this);

        setTitle(symbol);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, QuoteProvider.HistoricalQuotes.CONTENT_URI,
                new String[]{HistoricalQuoteColumns.DATE, HistoricalQuoteColumns.CLOSE},
                QuoteColumns.SYMBOL + " = ?",
                new String[]{args.getString("symbol")},
                HistoricalQuoteColumns.DATE + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Timber.d("onLoadFinished.");

        cursor = data;
        refreshChart();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public void networkToast() {
        Toast.makeText(this, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }

    private void refreshChart() {
        try {
            if (cursor.getCount() == 0) {
                return;
            }

            ArrayList<Float> prices = new ArrayList<>();
            LineSet lineSet = new LineSet();

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {

                String dateStr = cursor.getString(cursor.getColumnIndex(HistoricalQuoteColumns.DATE));
                Date date = sdf1.parse(dateStr);

                float price = Float.parseFloat(cursor.getString(cursor.getColumnIndex(HistoricalQuoteColumns.CLOSE)));

                lineSet.addPoint(sdf2.format(date), price);

                prices.add(price);
            }

            minRange = Math.round(Collections.min(prices)) - 10;
            maxRange = Math.round(Collections.max(prices)) + 10;

            if (minRange < 0) {
                minRange = 0;
            }

            int steps = 5;
            int step = (maxRange - minRange) / steps;

            maxRange = minRange + (steps * step);

            Paint gridPaint = new Paint();
            gridPaint.setColor(getResources().getColor(R.color.grid_gray));
            gridPaint.setStyle(Paint.Style.STROKE);
            gridPaint.setAntiAlias(true);
            gridPaint.setStrokeWidth(Tools.fromDpToPx(1f));

            Timber.d("min range %d, max range %d, step %d", minRange, maxRange, step);

            lineChartView.setBorderSpacing(1)
                    .setAxisBorderValues(minRange, maxRange, steps)
                    .setXLabels(AxisController.LabelPosition.OUTSIDE)
                    .setYLabels(AxisController.LabelPosition.OUTSIDE)
                    .setLabelsColor(getResources().getColor(R.color.label_gray))
                    .setXAxis(false)
                    .setYAxis(false)
                    .setBorderSpacing(Tools.fromDpToPx(5))
                    .setGrid(ChartView.GridType.HORIZONTAL, gridPaint);


            lineSet.setColor(getResources().getColor(R.color.md_red_400))
                    .setDotsStrokeThickness(Tools.fromDpToPx(2))
                    .setDotsStrokeColor(getResources().getColor(R.color.md_red_900))
                    .setDotsColor(getResources().getColor(R.color.md_red_900));
            lineChartView.addData(lineSet);
            lineChartView.show();
        } catch (Exception e) {
            Timber.e(e, null);
        }
    }
}

