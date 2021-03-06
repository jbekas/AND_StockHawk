package com.redgeckotech.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.TaskParams;
import com.redgeckotech.stockhawk.StockHawkApplication;
import com.redgeckotech.stockhawk.events.RxBus;

import javax.inject.Inject;

/**
 * Created by sam_chordas on 10/1/15.
 */
public class StockIntentService extends IntentService {

    @Inject
    RxBus rxBus;

    public StockIntentService() {
        super(StockIntentService.class.getName());
    }

    public StockIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        ((StockHawkApplication) getApplicationContext()).getApplicationComponent().inject(this);

        String tag = intent.getStringExtra("tag");

        Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");

        if ("add".equals(tag) || "init".equals(tag) || "periodic".equals(tag)) {
            StockTaskService stockTaskService = new StockTaskService(this);
            Bundle args = new Bundle();
            if (intent.getStringExtra("tag").equals("add")) {
                args.putString("symbol", intent.getStringExtra("symbol"));
            }
            // We can call OnRunTask from the intent service to force it to run immediately instead of
            // scheduling a task.
            stockTaskService.onRunTask(new TaskParams(intent.getStringExtra("tag"), args));
        } else if ("historical".equals(tag)) {
            HistoricalStockTaskService stockTaskService = new HistoricalStockTaskService(this);
            Bundle args = new Bundle();
            args.putString("symbol", intent.getStringExtra("symbol"));
            args.putString("startDate", intent.getStringExtra("startDate"));
            args.putString("endDate", intent.getStringExtra("endDate"));
            // We can call OnRunTask from the intent service to force it to run immediately instead of
            // scheduling a task.
            stockTaskService.onRunTask(new TaskParams(intent.getStringExtra("tag"), args));
        }
    }
}
