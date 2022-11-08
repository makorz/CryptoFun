package com.example.cryptofun;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;

import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.databinding.ActivityMainBinding;
import com.example.cryptofun.services.AlarmReceiverLoopingService;
import com.example.cryptofun.services.CreatingDatabaseService;
import com.example.cryptofun.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavHostFragment navHostFragment;
    private NavController navController;
    private DBHandler databaseDB;
    private Fragment homeFragment = new HomeFragment();
    private boolean DBcreated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        TextView timeView = binding.dateMain;
        databaseDB = new DBHandler(this);

        Cursor data = databaseDB.retrieveParam(1);
        if (data.getCount() == 0) {
            Toast.makeText(this, "Table is empty...", Toast.LENGTH_LONG).show();
        } else {
            data.moveToFirst();
            timeView.setText(data.getString(2));
        }
        data.close();

//       BottomNavigationView navView = findViewById(R.id.nav_view);
////         Passing each menu ID as a set of Ids because each
////         menu should be considered as top level destinations.
//        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
//                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
//                .build();
        navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        assert navHostFragment != null;
        navController = navHostFragment.getNavController();
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
//        NavigationUI.setupWithNavController(binding.navView, navController);

        Intent serviceIntent = new Intent(this,
                CreatingDatabaseService.class);
        startService(serviceIntent);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("DB_created"));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("DB_updated"));

        AlarmReceiverLoopingService alarm = new AlarmReceiverLoopingService();
        alarm.setAlarm(this);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("DB_updated")) {
                Log.e("MainActivity", "SendUPDMessageReceived");
                Bundle bundle = intent.getBundleExtra("bundleApprovedCrypto");
                if (intent.hasExtra("updateDate")) {
                    TextView timeView = binding.dateMain;
                    timeView.setText(intent.getStringExtra("updateDate"));
                } else if (bundle != null && DBcreated){
                    homeFragment.setArguments(bundle);
                    navController.popBackStack();
                    navController.navigate(R.id.navigation_home, bundle);
                }
            } else if (intent.getAction().equals("DB_created")) {
                DBcreated = intent.getBooleanExtra("finishedCRTDB",false);
                Log.e("MainActivity", "SendCRTMessageReceived");
                navController.popBackStack();
                navController.navigate(R.id.navigation_home);
            }
        }
    };


}