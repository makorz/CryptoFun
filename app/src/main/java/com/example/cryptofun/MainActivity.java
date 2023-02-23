package com.example.cryptofun;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Build;
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
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.databinding.ActivityMainBinding;
import com.example.cryptofun.services.AlarmReceiverLoopingService;
import com.example.cryptofun.services.CreatingDatabaseService;
import com.example.cryptofun.services.CreatingDatabaseWorker;
import com.example.cryptofun.ui.orders.OrdersFragment;
import com.example.cryptofun.ui.home.HomeFragment;
import com.example.cryptofun.ui.loading.LoadingFragment;
import com.example.cryptofun.ui.results.ResultsFragment;
import com.example.cryptofun.ui.settings.SettingsFragment;
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
        getWindow().setNavigationBarColor(ContextCompat.getColor(this,R.color.orange_700));

        TextView timeView = binding.textMain;
        bottomNavigationView = findViewById(R.id.nav_view2);

        Log.e(TAG, "CreateView");
        fm.beginTransaction().add(R.id.nav_host_fragment_activity_main,fragment0, "0").commit();
        fm.beginTransaction().add(R.id.nav_host_fragment_activity_main, fragment4, "4").hide(fragment4).commit();
        fm.beginTransaction().add(R.id.nav_host_fragment_activity_main, fragment3, "3").hide(fragment3).commit();
        fm.beginTransaction().add(R.id.nav_host_fragment_activity_main, fragment2, "2").hide(fragment2).commit();
        fm.beginTransaction().add(R.id.nav_host_fragment_activity_main,fragment1, "1").hide(fragment1).commit();

        databaseDB = new DBHandler(this);
        Cursor data = databaseDB.retrieveParam(1);
        if (data.getCount() == 0) {
            Toast.makeText(this, "Table is empty...", Toast.LENGTH_LONG).show();
        } else {
            data.moveToFirst();
            timeView.setText(data.getString(2));
        }
        data.close();

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        fm.beginTransaction().hide(active).show(fragment1).commit();
                        active = fragment1;
                        return true;
                    case R.id.navigation_dashboard:
                        fm.beginTransaction().hide(active).show(fragment2).commit();
                        active = fragment2;
                        return true;
                    case R.id.navigation_notifications:
                        fm.beginTransaction().hide(active).show(fragment3).commit();
                        active = fragment3;
                        return true;
                    case R.id.navigation_settings:
                        fm.beginTransaction().hide(active).show(fragment4).commit();
                        active = fragment4;
                        return true;
                }
                return false;
            }
        });

        if (!isMyServiceRunning(CreatingDatabaseService.class) && !isMyServiceRunning(CreatingDatabaseWorker.class)) {
            ProgressBar loadingBar = binding.loaderOfData;
            loadingBar.setVisibility(View.VISIBLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder ( CreatingDatabaseWorker.class ).addTag ( "WORKER_TAG" ).build ();
                WorkManager.getInstance ( this ).enqueue ( request );
            } else {
                Intent serviceIntent = new Intent(this,
                        CreatingDatabaseService.class);
                startForegroundService(serviceIntent);
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("DB_created"));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("DB_updated"));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("DB_update_start"));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("Approve_Fragment_Prepared"));

        Log.e(TAG, "CreateView2");

    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        databaseDB.close();
        Log.e(TAG, "Destroy");
        WorkManager workManager =  WorkManager.getInstance ( this );
        workManager.cancelAllWork();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("DB_updated") && DBCreated) {

                if (intent.getExtras().getBoolean("updateStarted")) {
                    Log.e(TAG, "SendUPDMessageReceivedA");
                    AlarmReceiverLoopingService alarm = new AlarmReceiverLoopingService();
                    alarm.stopAlarm(getApplicationContext());

                } else {
                    Log.e(TAG, "SendUPDMessageReceivedB");
                    TextView timeView = binding.textMain;
                    timeView.setText(intent.getStringExtra("updateDate"));
                    AlarmReceiverLoopingService alarm = new AlarmReceiverLoopingService();
                    alarm.setAlarm(getApplicationContext(),1);
                }
            } else if (intent.getAction().equals("DB_created")) {
                DBCreated = intent.getBooleanExtra("finishedCRTDB", false);
                Log.e(TAG, "SendCRTMessageReceived");
                AlarmReceiverLoopingService alarm = new AlarmReceiverLoopingService();
                alarm.setAlarm(getApplicationContext(), 0);
            }
            if (intent.getAction().equals("DB_update_start")) {
                Log.e(TAG, "InfoLoaderReceivedStarted");
                ProgressBar loadingBar = binding.loaderOfData;
                loadingBar.setVisibility(View.VISIBLE);
            }

            if (intent.getAction().equals("Approve_Fragment_Prepared")) {
                Log.e(TAG, "InfoLoaderReceivedFinished");
                ProgressBar loadingBar = binding.loaderOfData;
                loadingBar.setVisibility(View.GONE);
                if(active == fragment0) {
                    fm.beginTransaction().hide(active).show(fragment1).commit();
                    active = fragment1;
                }
            }
        }
    };

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager .getRunningServices(Integer.MAX_VALUE)) {
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