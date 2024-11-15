package app.makorz.cryptofun;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.cryptofun.R;
import app.makorz.cryptofun.data.database.DBHandler;
import com.example.cryptofun.databinding.ActivityMainBinding;
import app.makorz.cryptofun.services.AlarmReceiverLoopingService;
import app.makorz.cryptofun.services.ApprovingService;
import app.makorz.cryptofun.services.CreatingDatabaseService;
import app.makorz.cryptofun.services.OrdersService;
import app.makorz.cryptofun.services.UpdatingDatabaseService;
import app.makorz.cryptofun.ui.orders.OrdersFragment;
import app.makorz.cryptofun.ui.home.HomeFragment;
import app.makorz.cryptofun.ui.loading.LoadingFragment;
import app.makorz.cryptofun.ui.results.ResultsFragment;
import app.makorz.cryptofun.ui.settings.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private boolean DBCreated = false;
    private DBHandler databaseDB;

    private final Fragment fragment0 = new LoadingFragment();
    private final Fragment fragment1 = new HomeFragment();
    private final Fragment fragment2 = new OrdersFragment();
    private final Fragment fragment3 = new ResultsFragment();
    private final Fragment fragment4 = new SettingsFragment();
    Fragment active = fragment0;
    final FragmentManager fm = getSupportFragmentManager();
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        Objects.requireNonNull(this.getSupportActionBar()).hide();
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.orange_700));
        TextView timeView = binding.textMain;
        TextView strategyView = binding.textStrategyNumber;
        TextView stopLimitStrategyView = binding.textStopLimitStrategyNumber;
        bottomNavigationView = findViewById(R.id.nav_view2);
        bottomNavigationView.setVisibility(View.GONE);

        Log.e(TAG, "CreateView");
        fm.beginTransaction().add(R.id.nav_host_fragment_activity_main, fragment0, "0").commitAllowingStateLoss();
        fm.beginTransaction().add(R.id.nav_host_fragment_activity_main, fragment4, "4").hide(fragment4).commitAllowingStateLoss();
        fm.beginTransaction().add(R.id.nav_host_fragment_activity_main, fragment3, "3").hide(fragment3).commitAllowingStateLoss();
        fm.beginTransaction().add(R.id.nav_host_fragment_activity_main, fragment2, "2").hide(fragment2).commitAllowingStateLoss();
        fm.beginTransaction().add(R.id.nav_host_fragment_activity_main, fragment1, "1").hide(fragment1).commitAllowingStateLoss();

        databaseDB = DBHandler.getInstance(this);
        Cursor data = databaseDB.retrieveParam(1);
        if (data.getCount() == 0) {
            Toast.makeText(this, "Table is empty...", Toast.LENGTH_LONG).show();
        } else {
            data.moveToFirst();
            timeView.setText(data.getString(2));
        }
        data.close();

        String strategy = "STRATEGY: ";
        data = databaseDB.retrieveParam(17);
        if (data.getCount() == 0) {
            Toast.makeText(this, "Table is empty...", Toast.LENGTH_LONG).show();
        } else {
            data.moveToFirst();
            strategy += String.valueOf(data.getInt(3));
            strategyView.setText(strategy);
        }
        data.close();

        strategy = "SL STRATEGY: ";
        data = databaseDB.retrieveParam(18);
        if (data.getCount() == 0) {
            Toast.makeText(this, "Table is empty...", Toast.LENGTH_LONG).show();
        } else {
            data.moveToFirst();
            strategy += String.valueOf(data.getInt(3));
            stopLimitStrategyView.setText(strategy);
        }
        data.close();

        if (!isMyServiceRunning(CreatingDatabaseService.class)) {
            ProgressBar loadingBar = binding.loaderOfData;
            loadingBar.setVisibility(View.VISIBLE);

            Intent serviceIntent = new Intent(MainActivity.this,
                    CreatingDatabaseService.class);
            MainActivity.this.startForegroundService(serviceIntent);

        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("DB_created"));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("DB_updated"));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("DB_update_start"));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("ApprovedService"));

        Log.e(TAG, "CreateView2");

    }

    @Override
    protected void onDestroy() {
        databaseDB.close();
        Log.e(TAG, "Destroy");
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.e(TAG, "isServiceRunning APRV: " + isMyServiceRunning(ApprovingService.class) + " UPDT: "
                    + isMyServiceRunning(UpdatingDatabaseService.class) + " ORD: "
                    + isMyServiceRunning(OrdersService.class) + " RES: "
                     );

            if (intent.getAction().equals("DB_updated") && DBCreated) {

                if (!isMyServiceRunning(ApprovingService.class)) {
                    Intent serviceIntent = new Intent(MainActivity.this, ApprovingService.class);
                    Log.e(TAG, "APRVServiceBegin - UPD");
                    MainActivity.this.startForegroundService(serviceIntent);
                }

                TextView timeView = binding.textMain;
                TextView strategyView = binding.textStrategyNumber;
                TextView stopLimitStrategyView = binding.textStopLimitStrategyNumber;
                stopLimitStrategyView.setText(intent.getStringExtra("currentStopLimitStrategy"));
                strategyView.setText(intent.getStringExtra("currentStrategy"));
                timeView.setText(intent.getStringExtra("updateDate"));

                Cursor data = databaseDB.retrieveParam(3);
                int howManyMinutes = 1;
                if (data.getCount() == 0) {
                    Toast.makeText(getApplicationContext(), "Table is empty...", Toast.LENGTH_LONG).show();
                } else {
                    data.moveToFirst();
                    howManyMinutes = data.getInt(3);
                }
                data.close();
                Log.e(TAG, "ALARM START --> " + howManyMinutes + " minutes");
                AlarmReceiverLoopingService alarm = new AlarmReceiverLoopingService();
                alarm.setAlarm(getApplicationContext(),howManyMinutes);

            }

            if (intent.getAction().equals("DB_created")) {

                DBCreated = intent.getBooleanExtra("finishedCRTDB", false);
                Log.e(TAG, "Broadcast from CRTService received");

                if (!isMyServiceRunning(UpdatingDatabaseService.class)) {
                    Log.e(TAG, "UPDServiceBegin - CRT");
                    Intent serviceIntent = new Intent(MainActivity.this, UpdatingDatabaseService.class);
                    MainActivity.this.startForegroundService(serviceIntent);
                }

                if (!isMyServiceRunning(ApprovingService.class)) {
                    Log.e(TAG, "APRVServiceBegin - CRT");
                    Intent serviceIntent = new Intent(MainActivity.this, ApprovingService.class);
                    MainActivity.this.startForegroundService(serviceIntent);
                }

                Cursor data = databaseDB.retrieveParam(3);
                int howManyMinutes = 1;
                if (data.getCount() == 0) {
                    Toast.makeText(getApplicationContext(), "Table is empty...", Toast.LENGTH_LONG).show();
                } else {
                    data.moveToFirst();
                    howManyMinutes = data.getInt(3);
                }
                data.close();
                Log.e(TAG, "ALARM START --> " + howManyMinutes + " minutes");
                AlarmReceiverLoopingService alarm = new AlarmReceiverLoopingService();
                alarm.setAlarm(getApplicationContext(), howManyMinutes);
            }

            if (intent.getAction().equals("DB_update_start")) {
                Log.e(TAG, "Broadcast 2 from UPDService received");
                ProgressBar loadingBar = binding.loaderOfData;
                loadingBar.setVisibility(View.VISIBLE);
            }

            if (intent.getAction().equals("ApprovedService") && DBCreated) {
                Log.e(TAG, "Broadcast from APRVService received");
                ProgressBar loadingBar = binding.loaderOfData;
                loadingBar.setVisibility(View.GONE);

                if (!isMyServiceRunning(OrdersService.class)) {
                    Intent serviceIntent = new Intent(MainActivity.this,OrdersService.class);
                    MainActivity.this.startForegroundService(serviceIntent);
                }

                if (active == fragment0) {
                    fm.beginTransaction().hide(active).show(fragment1).commit();
                    active = fragment1;
                }

                //Enable bottom bottomNavigationView after populating fragments
                bottomNavigationView.setVisibility(View.VISIBLE);

                bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.navigation_home:
                                fm.beginTransaction().hide(active).show(fragment1).commitAllowingStateLoss();
                                active = fragment1;
                                return true;
                            case R.id.navigation_dashboard:
                                if (!isMyServiceRunning(OrdersService.class) && !isMyServiceRunning(ApprovingService.class) && !isMyServiceRunning(UpdatingDatabaseService.class)) {
                                    Intent serviceIntent = new Intent(MainActivity.this, OrdersService.class);
                                    MainActivity.this.startForegroundService(serviceIntent);

                                }
                                fm.beginTransaction().hide(active).show(fragment2).commitAllowingStateLoss();
                                active = fragment2;
                                return true;
                            case R.id.navigation_notifications:
                                if (!isMyServiceRunning(OrdersService.class) && !isMyServiceRunning(ApprovingService.class) && !isMyServiceRunning(UpdatingDatabaseService.class)) {
                                    Intent serviceIntent = new Intent(MainActivity.this, OrdersService.class);
                                    MainActivity.this.startForegroundService(serviceIntent);

                                }
                                fm.beginTransaction().hide(active).show(fragment3).commitAllowingStateLoss();
                                active = fragment3;
                                return true;
                            case R.id.navigation_settings:
                                fm.beginTransaction().hide(active).show(fragment4).commitAllowingStateLoss();
                                active = fragment4;
                                return true;
                        }
                        return false;
                    }
                });

            }

        }
    };

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPostResume() {
        Log.e(TAG, "PostResume");
        super.onPostResume();
    }

    @Override
    protected void onStart() {
        Log.e(TAG, "Start");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.e(TAG, "Stop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "Pause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.e(TAG, "Resume");
        super.onResume();
    }

    @Override
    protected void onRestart() {
        Log.e(TAG, "Restart");
        super.onRestart();
    }


}