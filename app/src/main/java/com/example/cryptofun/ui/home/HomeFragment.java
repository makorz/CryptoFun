package com.example.cryptofun.ui.home;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cryptofun.R;
import com.example.cryptofun.database.Kline;
import com.example.cryptofun.databinding.FragmentHomeBinding;
import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.ui.view.GridViewElement;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private List<String> listOfSymbols = new ArrayList<>();
    private DBHandler databaseDB;

    private static final String TABLE_NAME_KLINES_DATA = "klines_data";
    private static final String TABLE_SYMBOL_AVG = "crypto_avg_price";
    private static final String TABLE_NAME_CONFIG = "config";
    private static final String DB_NAME = "cryptodb.db";


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

//        HomeViewModel homeViewModel =
//                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        databaseDB = new DBHandler(getContext());

        setMainParametersOnView();
        buttonsJob();
        return root;
    }



    private void buttonsJob() {

        Button buttonDelete = binding.flush;
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireContext().deleteDatabase(DB_NAME);
                //RestartActivity
                Intent intent = getActivity().getIntent();
                getActivity().finish();
                startActivity(intent);
            }
        });

    }

    public void setMainParametersOnView() {

        Bundle bundle = getArguments();
        Log.e("HomeFragments", "setMain SIZE ON START -> " + listOfSymbols.size() );
        ListView symbolList = binding.symbolsList;

        Cursor data = databaseDB.retrieveCryptoSymbolsToListView();
        if (data.getCount() == 0) {
            //Toast.makeText(getContext(), "Table is empty...", Toast.LENGTH_LONG).show();
        } else {
            listOfSymbols.clear();
            while (data.moveToNext()) {
                listOfSymbols.add(data.getString(0));
            }
            int nrOfGridElements = 20;
            List<String> biggestNrOfTradesSymbols = getBiggestNrOfTradesSymbols("4h",nrOfGridElements);
            ArrayList<GridViewElement> cryptoPercentageList = new ArrayList<>();
            for (int i = 0; i < listOfSymbols.size(); i++) {
                cryptoPercentageList.add(get24hPercentChangeForCrypto(listOfSymbols.get(i)));
                if (cryptoPercentageList.get(i) == null) {
                    cryptoPercentageList.remove(i);
                }
            }

            int under1 = 0;
            int between5and15 = 0;
            int over15 = 0;
            int between1and5 = 0;
            for (int i = 0; i < cryptoPercentageList.size(); i++) {


                if (cryptoPercentageList.get(i).getPercent() < 1) {
                    under1++;
                } else if (cryptoPercentageList.get(i).getPercent() >= 1 && cryptoPercentageList.get(i).getPercent() < 5) {
                    between1and5++;
                } else if (cryptoPercentageList.get(i).getPercent() >= 5 && cryptoPercentageList.get(i).getPercent() <= 15) {
                    between5and15++;
                } else if (cryptoPercentageList.get(i).getPercent() > 15) {
                    over15++;
                }
            }

            float percentU2 = (float) under1 / cryptoPercentageList.size() * 100;
            float percentB5and15 = (float) between5and15 / cryptoPercentageList.size() * 100;
            float percentO15 = (float) over15 / cryptoPercentageList.size() * 100;
            float percentB1and5 = (float) between1and5 / cryptoPercentageList.size() * 100;

            ArrayList<GridViewElement> cryptoGridViewList = new ArrayList<>();
            if (biggestNrOfTradesSymbols.size() > 1) {
                cryptoGridViewList.add(new GridViewElement("<1%", percentU2, under1));
                cryptoGridViewList.add(new GridViewElement("1% - 5%", percentB1and5, between1and5));
                cryptoGridViewList.add(new GridViewElement("5% - 15%", percentB5and15, between5and15));
                cryptoGridViewList.add(new GridViewElement(">15%", percentO15, over15));
                for (int i = 0; i < nrOfGridElements; i++) {
                    cryptoGridViewList.add(get24hPercentChangeForCrypto(biggestNrOfTradesSymbols.get(i)));
                }
            }


            RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(),4, GridLayoutManager.HORIZONTAL,false);
            binding.cryptoRecyclerView.setLayoutManager(layoutManager);
            binding.cryptoRecyclerView.setAdapter(new CryptoGridAdapter(cryptoGridViewList));
        }

        if (bundle != null && bundle.containsKey("approvedCrypto")){
            symbolList.setAdapter(null);
            Log.e("HomeFragment", String.valueOf(bundle.getStringArrayList("approvedCrypto")));
            symbolList.setAdapter(new ArrayAdapter<String>(getContext(), R.layout.list_view_approved_coins , bundle.getStringArrayList("approvedCrypto")));
        }
        data.close();
    }


    // Calculate 24h change of price in %
    public GridViewElement get24hPercentChangeForCrypto(String symbol) {

        List<Kline> coinKlines1d = new ArrayList<>();
        Cursor data = databaseDB.retrieveDataToFindBestCrypto(TABLE_NAME_KLINES_DATA, symbol);

        if (data.getCount() == 0) {
            Log.e("HomeFragment", "24hPercentCount-Table " + TABLE_NAME_KLINES_DATA + " Empty");

        } else {
            while (data.moveToNext()) {

                if (data.getString(10).equals("4h")) {

                    coinKlines1d.add(new Kline(data.getInt(0), data.getString(1), data.getLong(2), data.getFloat(3), data.getFloat(4),
                            data.getFloat(5), data.getFloat(6), data.getFloat(7), data.getLong(8), data.getLong(9), data.getString(10)));

                }
            }
            long openTime = coinKlines1d.get(0).gettOpenTime();
            float closePriceYesterday = coinKlines1d.get(6).gettClosePrice();
            float percentOfChange = 0;
            long openTime2 = coinKlines1d.get(6).gettOpenTime();
            float closePriceToday = coinKlines1d.get(0).gettClosePrice();

            if (openTime2 + 86400000 == openTime) {
                percentOfChange = ((closePriceToday / closePriceYesterday) * 100) - 100;
            }
            return new GridViewElement(symbol, percentOfChange, closePriceToday);
        }
        data.close();
        return new GridViewElement(symbol, 0, 0);

    }

    public List<String> getBiggestNrOfTradesSymbols(String interval, int nrOfResults) {

        List<String> bigVolume = new ArrayList<>();
        List<Kline> klinesVolume = new ArrayList<>();
        Cursor data = databaseDB.checkVolumeOfKlineInterval(interval);

        if (data.getCount() == 0) {
            Log.e("HomeFragment", "BiggestNrOfTrades-Table " + TABLE_NAME_KLINES_DATA + " Empty");
        } else {
            data.moveToFirst();
            while (data.moveToNext()) {

                if (!data.getString(0).contains("BUSD")) {
                    klinesVolume.add(new Kline(data.getString(0),data.getFloat(1), data.getLong(2), data.getLong(3)));
                }
            }
            for (int i = 0; i < nrOfResults; i++) {
                bigVolume.add(klinesVolume.get(i).gettTokenSymbol());
            }
        }
        data.close();

        return bigVolume;
    }


    @Override
    public void onAttach(@NonNull Context context) {
        Log.e("ATTAACHH", "SASFSAFSFASF");
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        Log.e("dedededATTAACHH", "SASFSAFSFASF");
        super.onDetach();
    }

    @Override
    public void onDestroy() {

        Log.e("destroyyyy", "SASFSAFSFASF");
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}