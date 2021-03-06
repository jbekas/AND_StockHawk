package com.redgeckotech.stockhawk.ui;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.redgeckotech.stockhawk.R;
import com.redgeckotech.stockhawk.StockHawkApplication;
import com.redgeckotech.stockhawk.data.QuoteColumns;
import com.redgeckotech.stockhawk.data.QuoteProvider;
import com.redgeckotech.stockhawk.events.RxBus;
import com.redgeckotech.stockhawk.events.SymbolNotFoundEvent;
import com.redgeckotech.stockhawk.rest.QuoteCursorAdapter;
import com.redgeckotech.stockhawk.rest.RecyclerViewItemClickListener;
import com.redgeckotech.stockhawk.rest.Utils;
import com.redgeckotech.stockhawk.service.StockIntentService;
import com.redgeckotech.stockhawk.service.StockTaskService;
import com.redgeckotech.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

import javax.inject.Inject;

import rx.Subscription;
import rx.functions.Action1;
import timber.log.Timber;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String SERVICE_INTENT_INITIALIZED = "SERVICE_INTENT_INITIALIZED";

    @Inject RxBus rxBus;

    private Subscription symbolNotFoundSubscription;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private ItemTouchHelper mItemTouchHelper;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;
    private Cursor mCursor;
    private boolean isConnected;
    private boolean serviceIntentInitialized;

    private MenuItem changeUnitsMenuItem;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fab;
    private TextView networkNotAvailable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        ((StockHawkApplication) getApplicationContext()).getApplicationComponent().inject(this);

        if (savedInstanceState != null) {
            serviceIntentInitialized = savedInstanceState.getBoolean(SERVICE_INTENT_INITIALIZED, false);
        }

        setContentView(R.layout.activity_my_stocks);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Timber.d("onRefresh");
                init();
            }
        });

        mCursorAdapter = new QuoteCursorAdapter(this, null);
        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        try {
                            Intent intent = new Intent(mContext, StockDetailsActivity.class);
                            mCursor.moveToPosition(position);
                            intent.putExtra("symbol", mCursor.getString(mCursor.getColumnIndex("symbol")));
                            mContext.startActivity(intent);
                        } catch (Exception e) {
                            errorkToast();
                        }
                    }
                }));
        recyclerView.setAdapter(mCursorAdapter);


        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected) {
                    new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                            .content(R.string.content_test)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    // On FAB click, receive user input. Make sure the stock doesn't already exist
                                    // in the DB and proceed accordingly
                                    Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                            new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                            new String[]{input.toString()}, null);
                                    if (c.getCount() != 0) {
                                        Toast toast =
                                                Toast.makeText(MyStocksActivity.this, R.string.stock_already_saved,
                                                        Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                        toast.show();
                                        return;
                                    } else {
                                        // Add the stock to DB
                                        Intent serviceIntent = new Intent(MyStocksActivity.this, StockIntentService.class);
                                        serviceIntent.putExtra("tag", "add");
                                        serviceIntent.putExtra("symbol", input.toString());
                                        startService(serviceIntent);
                                    }
                                }
                            })
                            .show();
                } else {
                    networkToast();
                }

            }
        });

        networkNotAvailable = (TextView) findViewById(R.id.network_not_available);

        // Accessibility, add the FAB to navigation after the toolbar.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            fab.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            fab.setAccessibilityTraversalBefore(R.id.recycler_view);
        }

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mTitle = getTitle();

        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
    }


    @Override
    public void onResume() {
        super.onResume();

        init();

        if (isConnected) {
            long period = 3600L;
            long flex = 10L;
            String periodicTag = "periodic";

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }

        symbolNotFoundSubscription = rxBus.register(SymbolNotFoundEvent.class, new Action1<SymbolNotFoundEvent>() {
            @Override
            public void call(final SymbolNotFoundEvent symbolNotFoundEvent) {
                Timber.d("Received: %s", symbolNotFoundEvent);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(MyStocksActivity.this, getString(R.string.symbol_not_found, symbolNotFoundEvent.getSymbol()), Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                        toast.show();
                    }
                });
            }
        });

        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (symbolNotFoundSubscription != null) {
            symbolNotFoundSubscription.unsubscribe();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SERVICE_INTENT_INITIALIZED, serviceIntentInitialized);
    }

    public void init() {
        checkNetworkStatus();
        initStockIntentService();
        updateUI();
    }

    public void checkNetworkStatus() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    public void initStockIntentService() {
        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately

        // Run the initialize task service so that some stocks appear upon an empty database
        if (isConnected) {
            if (!serviceIntentInitialized) {
                Timber.d("starting service intent.");

                Intent serviceIntent = new Intent(MyStocksActivity.this, StockIntentService.class);
                serviceIntent.putExtra("tag", "init");
                startService(serviceIntent);

                serviceIntentInitialized = true;
            }
        }
    }

    public void updateUI() {
        swipeRefreshLayout.setRefreshing(false);

        if (!isConnected) {
            fab.setVisibility(View.GONE);
            networkNotAvailable.setVisibility(View.VISIBLE);
            return;
        }

        fab.setVisibility(View.VISIBLE);
        networkNotAvailable.setVisibility(View.GONE);
    }

    public void networkToast() {
        Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }

    public void errorkToast() {
        Toast.makeText(mContext, getString(R.string.error), Toast.LENGTH_SHORT).show();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);

        // Change button title for accessibility.
        changeUnitsMenuItem = menu.findItem(R.id.action_change_units);
        changeUnitsMenuItem.setTitle(Utils.showPercent ? R.string.show_value_change : R.string.show_percent_change);

        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_change_units) {
            // this is for changing stock changes from percent value to dollar value
            Utils.showPercent = !Utils.showPercent;
            changeUnitsMenuItem.setTitle(Utils.showPercent ? R.string.show_value_change : R.string.show_percent_change);
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This narrows the return to only the stocks that are most current.
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
        mCursor = data;

        init();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }
}
