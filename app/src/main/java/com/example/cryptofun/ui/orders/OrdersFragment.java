package com.example.cryptofun.ui.orders;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptofun.R;
import com.example.cryptofun.databinding.FragmentOrdersBinding;

import com.example.cryptofun.ui.view.OrderListAdapter;
import com.example.cryptofun.ui.view.OrderListViewElement;

import java.util.ArrayList;
import java.util.List;

public class OrdersFragment extends Fragment {

    private FragmentOrdersBinding binding;
    private static final String TAG = "DashboardFrag";
    private ArrayList<OrderListViewElement> mItems = new ArrayList<>();



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        Log.e(TAG, "CreateView");
        View view = inflater.inflate(R.layout.fragment_orders, container, false);


        mItems.add(new OrderListViewElement("ETHUSDT",true, 50,2.566f, 2.575f, 2.32f,3.2f, 100,"29.01 15:22",5));
        mItems.add(new OrderListViewElement("BTCUSDT",false, 1000,2566f, 2300f, 2000f,3000f, 750,"29.02 03:22",10));
        mItems.add(new OrderListViewElement("ETHBUSD",true, 50,2.566f, 2.89f, 2.32f,3.2f, 3000,"29.02 15:22",15));


//        OrdersViewModel ordersViewModel =11
//                new ViewModelProvider(this).get(OrdersViewModel.class);
//
//        binding = FragmentOrdersBinding.inflate(inflater, container, false);
//        View root = binding.getRoot();

       // final TextView textView = binding.textDashboard;
       // dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        RecyclerView recyclerView = view.findViewById(R.id.rw_currentOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        OrderListAdapter adapter = new OrderListAdapter(mItems);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}