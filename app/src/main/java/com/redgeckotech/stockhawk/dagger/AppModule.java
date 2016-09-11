package com.redgeckotech.stockhawk.dagger;

import android.content.Context;

import com.redgeckotech.stockhawk.StockHawkApplication;
import com.redgeckotech.stockhawk.events.RxBus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {
    private final StockHawkApplication application;

    public AppModule(StockHawkApplication application) {
        this.application = application;
    }

    @Provides
    @Singleton
    Context provideApplicationContext() {
        return this.application;
    }

    @Provides
    @Singleton
    public RxBus getRxBus() {
        return new RxBus();
    }
}
