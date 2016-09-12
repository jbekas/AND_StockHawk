package com.redgeckotech.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.util.Log;

import com.redgeckotech.stockhawk.data.HistoricalQuoteColumns;
import com.redgeckotech.stockhawk.data.QuoteColumns;
import com.redgeckotech.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class HistoricalUtils {

    private static String LOG_TAG = HistoricalUtils.class.getSimpleName();

    public static ArrayList<ContentProviderOperation> quoteJsonToContentVals(String JSON) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();

        ContentProviderOperation deleteOp = ContentProviderOperation.newDelete(QuoteProvider.HistoricalQuotes.CONTENT_URI).build();
        batchOperations.add(deleteOp);

        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject.length() != 0) {

                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");

                    batchOperations.add(buildBatchOperation(jsonObject));

                } else {
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);

                            batchOperations.add(buildBatchOperation(jsonObject));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    public static String truncateBidPrice(String bidPrice) {
        bidPrice = String.format(Locale.getDefault(), "%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.HistoricalQuotes.CONTENT_URI);
        try {
            builder.withValue(HistoricalQuoteColumns.SYMBOL, jsonObject.getString("Symbol"));
            builder.withValue(HistoricalQuoteColumns.DATE, jsonObject.getString("Date"));
            builder.withValue(HistoricalQuoteColumns.CLOSE, truncateBidPrice(jsonObject.getString("Close")));
            builder.withValue(HistoricalQuoteColumns.ISCURRENT, 1);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

}
