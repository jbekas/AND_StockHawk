package com.redgeckotech.stockhawk;

import android.app.Application;

import com.redgeckotech.stockhawk.dagger.AppComponent;
import com.redgeckotech.stockhawk.dagger.AppModule;
import com.redgeckotech.stockhawk.dagger.DaggerAppComponent;

import timber.log.Timber;

public class StockHawkApplication extends Application {

    private AppComponent applicationComponent;

    @Override
    public void onCreate() {

        super.onCreate();

        initializeInjector();
        getApplicationComponent().inject(this);

        Timber.plant(new Timber.DebugTree());
    }

    public void initializeInjector() {
        this.applicationComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .build();
    }

    public AppComponent getApplicationComponent() {
        return this.applicationComponent;
    }
}
