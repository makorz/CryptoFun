package app.makorz.cryptofun.ui.home;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cryptofun.databinding.FragmentHomeBinding;
import app.makorz.cryptofun.services.ApprovingService;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFrag";

    private FragmentHomeBinding binding;
    CryptoListAdapter myAdapter6, myAdapter2, myAdapter30;
    //SimpleListAdapter myAdapterLONG, myAdapterSHORT;
    CryptoGridAdapter gridAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        binding.cryptoRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 6, GridLayoutManager.HORIZONTAL, false));
        binding.cryptoList6RecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        //binding.cryptoList30RecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        ArrayList<ListViewElement> initialList = new ArrayList<>();
        ArrayList<GridViewElement> initialGrid = new ArrayList<>();
        gridAdapter = new CryptoGridAdapter(initialGrid);
        myAdapter6 = new CryptoListAdapter(initialList);
        myAdapter2 = new CryptoListAdapter(initialList);
        myAdapter30 = new CryptoListAdapter(initialList);
//        myAdapterLONG = new SimpleListAdapter(initialList);
//        myAdapterSHORT = new SimpleListAdapter(initialList);
        binding.cryptoRecyclerView.setAdapter(gridAdapter);
        binding.cryptoList6RecyclerView.setAdapter(myAdapter6);
       // binding.cryptoList30RecyclerView.setAdapter(myAdapter30);


        View root = binding.getRoot();
        Log.e(TAG, "CreateView");

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMessageReceiver, new IntentFilter("ApprovedService"));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMessageReceiver, new IntentFilter("DB_updated"));

        return root;
    }

    // Receive broadcast from ApprovingService
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("ApprovedService")) {
                Bundle bundle = intent.getBundleExtra("bundleApprovedCrypto");
                Log.e(TAG, "APRVServiceReceived " + Thread.currentThread());

                onReceiveNewGridList(gridAdapter, (ArrayList<GridViewElement>) bundle.getSerializable("cryptoGridViewList"));
                onReceiveNewList(myAdapter6, (ArrayList<ListViewElement>) bundle.getSerializable("list3"));
                onReceiveNewList(myAdapter30, (ArrayList<ListViewElement>) bundle.getSerializable("list1"));

            } else if (intent.getAction().equals("DB_updated")) {
                Log.e(TAG, "SendUPDMessageReceived");
                if (!intent.getExtras().getBoolean("updateStarted")) {

                    if (!isMyServiceRunning(ApprovingService.class, getContext())) {
                        Log.e(TAG, "APRVServiceBegin");

                        Intent serviceIntent = new Intent(getActivity(),
                                ApprovingService.class);
                        getContext().startForegroundService(serviceIntent);

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
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        Log.e(TAG, "Resume");
//        Intent serviceIntent = new Intent(getActivity(),
//                ApprovingService.class);
//
//        if (!isMyServiceRunning(ApprovingService.class, getContext())) {
//            Log.e(TAG, "APRVServiceBeginFromResume");
//            getContext().startForegroundService(serviceIntent);
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