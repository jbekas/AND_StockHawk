package com.redgeckotech.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.redgeckotech.stockhawk.StockHawkApplication;
import com.redgeckotech.stockhawk.data.QuoteColumns;
import com.redgeckotech.stockhawk.data.QuoteProvider;
import com.redgeckotech.stockhawk.events.RxBus;
import com.redgeckotech.stockhawk.rest.HistoricalUtils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.URLEncoder;

import javax.inject.Inject;

import timber.log.Timber;

public class HistoricalStockTaskService extends GcmTaskService {

    @Inject RxBus rxBus;

    private String LOG_TAG = HistoricalStockTaskService.class.getSimpleName();

    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;

    public HistoricalStockTaskService() {
    }

    public HistoricalStockTaskService(Context context) {
        mContext = context;

        ((StockHawkApplication) context.getApplicationContext()).getApplicationComponent().inject(this);
    }

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams params) {
        int result = GcmNetworkManager.RESULT_FAILURE;

        try {
            if (mContext == null) {
                mContext = this;
            }
            StringBuilder urlStringBuilder = new StringBuilder();

            // Base URL for the Yahoo query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol "
                    + "in (", "UTF-8"));

            String stockInput = params.getExtras().getString("symbol");
            String startDate = params.getExtras().getString("startDate");
            String endDate = params.getExtras().getString("endDate");

            urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));

            // finalize the URL for the API query.
            urlStringBuilder.append(URLEncoder.encode(" and startDate=\"" + startDate + "\"", "UTF-8"));
            urlStringBuilder.append(URLEncoder.encode(" and endDate=\"" + endDate + "\"", "UTF-8"));
            urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=");

            String urlString = urlStringBuilder.toString();

            Timber.d("urlString: %s", urlString);

            String getResponse = fetchData(urlString);

            //Timber.d("getResponse: %s", getResponse);

            result = GcmNetworkManager.RESULT_SUCCESS;
            try {
                ContentValues contentValues = new ContentValues();
                // update ISCURRENT to 0 (false) so new data is current
                if (isUpdate) {
                    contentValues.put(QuoteColumns.ISCURRENT, 0);
                    mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues, null, null);
                }
                mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY, HistoricalUtils.quoteJsonToContentVals(getResponse));
            } catch (RemoteException | OperationApplicationException e) {
                Log.e(LOG_TAG, "Error applying batch insert", e);
            }
        } catch (IOException e) {
            Timber.e(e, null);
        }

        return result;
    }
}
