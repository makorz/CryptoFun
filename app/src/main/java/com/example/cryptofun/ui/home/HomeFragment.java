package com.example.cryptofun.ui.home;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.cryptofun.databinding.FragmentHomeBinding;
import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.services.ApprovingService;
import com.example.cryptofun.services.ApprovingWorker;
import com.example.cryptofun.services.UpdatingDatabaseService;
import com.example.cryptofun.services.UpdatingDatabaseWorker;
import com.example.cryptofun.ui.view.CryptoGridAdapter;
import com.example.cryptofun.ui.view.CryptoListAdapter;
import com.example.cryptofun.ui.view.GridViewElement;
import com.example.cryptofun.ui.view.ListViewElement;
import com.example.cryptofun.ui.view.SimpleListAdapter;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFrag";

    private FragmentHomeBinding binding;
    private DBHandler databaseDB;
    CryptoListAdapter myAdapter6, myAdapter2, myAdapter30;
    SimpleListAdapter myAdapterLONG, myAdapterSHORT;
    CryptoGridAdapter gridAdapter;
    private int resumeControl;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

//        HomeViewModel homeViewModel =
//                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        binding.cryptoRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 6, GridLayoutManager.HORIZONTAL, false));
        binding.cryptoList6RecyclerView.setLayoutManager( new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL,false));
        binding.cryptoList2RecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL,false));
        binding.cryptoList30RecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL,false));
        binding.cryptoListLONGRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL,false));
        binding.cryptoListSHORTRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL,false));
        ArrayList<ListViewElement> initialList = new ArrayList<>();
        ArrayList<GridViewElement> initialGrid = new ArrayList<>();
        gridAdapter = new CryptoGridAdapter(initialGrid);
        myAdapter6 = new CryptoListAdapter(initialList);
        myAdapter2 = new CryptoListAdapter(initialList);
        myAdapter30 = new CryptoListAdapter(initialList);
        myAdapterLONG = new SimpleListAdapter(initialList);
        myAdapterSHORT = new SimpleListAdapter(initialList);
        binding.cryptoRecyclerView.setAdapter(gridAdapter);
        binding.cryptoList6RecyclerView.setAdapter(myAdapter6);
        binding.cryptoList2RecyclerView.setAdapter(myAdapter2);
        binding.cryptoList30RecyclerView.setAdapter(myAdapter30);
        binding.cryptoListLONGRecyclerView.setAdapter(myAdapterLONG);
        binding.cryptoListSHORTRecyclerView.setAdapter(myAdapterSHORT);

        View root = binding.getRoot();
        databaseDB = new DBHandler(getContext());
        Log.e(TAG, "CreateView");

        Intent serviceIntent = new Intent(getContext(),
                ApprovingService.class);

        Log.e(TAG, "isServiceRunning APRV: " + isMyServiceRunning(ApprovingService.class, getContext()) + " UPDT: "
                + isMyServiceRunning(UpdatingDatabaseService.class, getContext()));

        if ((!isMyServiceRunning(ApprovingService.class, getContext()) && !isMyServiceRunning(UpdatingDatabaseService.class, getContext()))) {
            Log.e(TAG, "APRVServiceBegin");
            resumeControl = 1;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ApprovingWorker.class).addTag("UPDATE_WORKER_TAG").build();
                WorkManager.getInstance(getContext()).enqueueUniqueWork("UPDATE_WORKER_TAG", ExistingWorkPolicy.KEEP,request);
            } else {
                getContext().startForegroundService(serviceIntent);
            }
        }

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMessageReceiver, new IntentFilter("ApprovedService"));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMessageReceiver, new IntentFilter("DB_updated"));

        //buttonsJob();
        return root;
    }


//    private void buttonsJob() {
//
//        Button buttonDelete = binding.flush;
//        buttonDelete.setOnClickListener(v -> {
//            requireContext().deleteDatabase(DB_NAME);
//            //RestartActivity
//            Intent intent = new Intent(getActivity(), MainActivity.class);
//            getActivity().finish();
//            startActivity(intent);
//        });
//
//    }

    // Receive broadcast from ApprovingService
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("ApprovedService")) {
                Bundle bundle = intent.getBundleExtra("bundleApprovedCrypto");
                Log.e(TAG, "APRVServiceReceived " + Thread.currentThread());

                onReceiveNewGridList(gridAdapter, (ArrayList<GridViewElement>) bundle.getSerializable("cryptoGridViewList"));
                onReceiveNewList(myAdapter2, (ArrayList<ListViewElement>) bundle.getSerializable("list2"));
                onReceiveNewList(myAdapter6, (ArrayList<ListViewElement>) bundle.getSerializable("list3"));
                onReceiveNewList(myAdapter30, (ArrayList<ListViewElement>) bundle.getSerializable("list1"));
                onReceiveNewSimpleList(myAdapterLONG, (ArrayList<ListViewElement>) bundle.getSerializable("list4"));
                onReceiveNewSimpleList(myAdapterSHORT, (ArrayList<ListViewElement>) bundle.getSerializable("list5"));

            } else if (intent.getAction().equals("DB_updated")) {
                Log.e(TAG, "SendUPDMessageReceived");
                if (!intent.getExtras().getBoolean("updateStarted")) {

                    if (!isMyServiceRunning(ApprovingService.class, getContext()) && !isMyServiceRunning(UpdatingDatabaseService.class, getContext())) {
                        Log.e(TAG, "APRVServiceBegin");

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Log.e(TAG, "APRVServiceBegin2");
                            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ApprovingWorker.class).addTag("WORKER_TAG").build();
                            WorkManager.getInstance(getContext()).enqueueUniqueWork("WORKER_TAG", ExistingWorkPolicy.KEEP,request);
                        } else {
                            Log.e(TAG, "APRVServiceBegin3");
                            Intent serviceIntent = new Intent(getActivity(),
                                    ApprovingService.class);
                            getContext().startForegroundService(serviceIntent);
                        }

                    }
                }
            }
        }
    };

    private boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        Log.e(TAG, "Attach");
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        Log.e(TAG, "Detach");
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "Destroy");
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mMessageReceiver);
        databaseDB.close();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        Log.e(TAG,"Resume");
//        Intent serviceIntent = new Intent(getActivity(),
//                ApprovingService.class);
//
//        if (!isMyServiceRunning(ApprovingService.class, getContext()) && !isMyServiceRunning(UpdatingDatabaseService.class, getContext())
//                && resumeControl != 1) {
//            Log.e(TAG, "APRVServiceBeginFromResume");
//            resumeControl = 1;
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ApprovingWorker.class).addTag("UPDATE_WORKER_TAG").build();
//                WorkManager.getInstance(getContext()).enqueueUniqueWork("UPDATE_WORKER_TAG", ExistingWorkPolicy.KEEP,request);
//            } else {
//                getContext().startForegroundService(serviceIntent);
//            }
//            resumeControl = 0;
//        }

        super.onResume();
    }

    public void onReceiveNewList(CryptoListAdapter adapter, ArrayList<ListViewElement> newList) {
        adapter.updateList(newList);
    }

    public void onReceiveNewSimpleList(SimpleListAdapter adapter, ArrayList<ListViewElement> newList) {
        adapter.updateList(newList);
    }

    public void onReceiveNewGridList(CryptoGridAdapter adapter, ArrayList<GridViewElement> newList) {
        adapter.updateList(newList);
    }
}