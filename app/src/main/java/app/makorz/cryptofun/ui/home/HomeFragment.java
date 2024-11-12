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

import app.makorz.cryptofun.data.ApprovedGroupedTokens;
import app.makorz.cryptofun.services.ApprovingService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFrag";

    private FragmentHomeBinding binding;
    CryptoApprovedAdapter myAdapter6, myAdapter2, myAdapter30;
    CryptoApprovedGroupedAdapter approvedAdapter;
    //SimpleListAdapter myAdapterLONG, myAdapterSHORT;
    CryptoGridAdapter gridAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);

//        binding.cryptoList30RecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
//        myAdapter2 = new CryptoListAdapter(initialList);
//        myAdapter30 = new CryptoListAdapter(initialList);
//        myAdapterLONG = new SimpleListAdapter(initialList);
//        myAdapterSHORT = new SimpleListAdapter(initialList);
//        binding.cryptoList30RecyclerView.setAdapter(myAdapter30);

        binding.cryptoRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 6, GridLayoutManager.HORIZONTAL, false));
        ArrayList<GridViewElement> initialGrid = new ArrayList<>();
        gridAdapter = new CryptoGridAdapter(initialGrid);
        binding.cryptoRecyclerView.setAdapter(gridAdapter);

        binding.cryptoList6RecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
//        ArrayList<ListViewElement> initialList = new ArrayList<>();
//        myAdapter6 = new CryptoApprovedAdapter(initialList);
//        binding.cryptoList6RecyclerView.setAdapter(myAdapter6);


        ArrayList<ListViewElement> items = new ArrayList<>();
        ArrayList<ApprovedGroupedTokens> groupedItems = groupItemsByStrategy(items);
        approvedAdapter = new CryptoApprovedGroupedAdapter(groupedItems);
        binding.cryptoList6RecyclerView.setAdapter(approvedAdapter);

        Log.e(TAG, "CreateView");

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMessageReceiver, new IntentFilter("ApprovedService"));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mMessageReceiver, new IntentFilter("DB_updated"));

        return binding.getRoot();
    }

    // Receive broadcast from ApprovingService
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("ApprovedService")) {
                Bundle bundle = intent.getBundleExtra("bundleApprovedCrypto");
                Log.e(TAG, "APRVServiceReceived " + Thread.currentThread());

                onReceiveNewGridList(gridAdapter, (ArrayList<GridViewElement>) bundle.getSerializable("cryptoGridViewList"));
                //onReceiveNewList(myAdapter6, (ArrayList<ListViewElement>) bundle.getSerializable("list3"));
                onReceiveNewGroupedList(approvedAdapter, (ArrayList<ListViewElement>) bundle.getSerializable("list3"));
                //onReceiveNewList(myAdapter30, (ArrayList<ListViewElement>) bundle.getSerializable("list1"));

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

    public static final HashMap<Integer, String> strategyNames = new HashMap<>();
    static {
        strategyNames.put(1, "EMA_4h_WT_ADX_EMA_15m");
        strategyNames.put(2, "EMA_KlineSize_WT_15m");
        strategyNames.put(3, "ICHIMOKU_15m");
        strategyNames.put(4, "Trend_GreenRed_15m_3m");
        strategyNames.put(5, "EMA_KlineSize_WT_4h");
        strategyNames.put(6, "KlinesCrossEMA_RSI_Appear_15m");
    }

    // Method to group items by strategyNr
    public ArrayList<ApprovedGroupedTokens> groupItemsByStrategy(ArrayList<ListViewElement> items) {
        HashMap<Integer, List<ListViewElement>> groupedMap = new HashMap<>();
        for (ListViewElement item : items) {
            int strategyNr = item.getStrategyNr();
            if (!groupedMap.containsKey(strategyNr)) {
                groupedMap.put(strategyNr, new ArrayList<>());
            }
            groupedMap.get(strategyNr).add(item);
        }

        ArrayList<ApprovedGroupedTokens> groupedItems = new ArrayList<>();
        for (Map.Entry<Integer, List<ListViewElement>> entry : groupedMap.entrySet()) {
            groupedItems.add(new ApprovedGroupedTokens(entry.getKey(), entry.getValue()));
        }
        return groupedItems;
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

    public void onReceiveNewList(CryptoApprovedAdapter adapter, ArrayList<ListViewElement> newList) {
        adapter.updateList(newList);
    }

    public void onReceiveNewGroupedList(CryptoApprovedGroupedAdapter adapter, ArrayList<ListViewElement> newList) {

        ArrayList<ApprovedGroupedTokens> groupedItems = groupItemsByStrategy(newList);

        adapter.updateList(groupedItems);
    }

    public void onReceiveNewSimpleList(SimpleListAdapter adapter, ArrayList<ListViewElement> newList) {
        adapter.updateList(newList);
    }

    public void onReceiveNewGridList(CryptoGridAdapter adapter, ArrayList<GridViewElement> newList) {
        adapter.updateList(newList);
    }
}