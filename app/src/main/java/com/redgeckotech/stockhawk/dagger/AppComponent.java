package com.redgeckotech.stockhawk.dagger;

import android.content.Context;

import com.redgeckotech.stockhawk.StockHawkApplication;
import com.redgeckotech.stockhawk.service.HistoricalStockTaskService;
import com.redgeckotech.stockhawk.service.StockIntentService;
import com.redgeckotech.stockhawk.service.StockTaskService;
import com.redgeckotech.stockhawk.ui.MyStocksActivity;
import com.redgeckotech.stockhawk.ui.StockDetailsActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton // Constraints this component to one-per-application or unscoped bindings.
@Component(modules = AppModule.class)
public interface AppComponent {
    void inject(StockHawkApplication application);

    void inject(MyStocksActivity activity);
    void inject(StockDetailsActivity activity);

    void inject(HistoricalStockTaskService service);
    void inject(StockIntentService service);
    void inject(StockTaskService service);

    //Exposed to sub-graphs.
    Context context();
}