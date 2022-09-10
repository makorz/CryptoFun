package com.example.cryptofun.ui.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.cryptofun.R;
import com.example.cryptofun.databinding.FragmentHomeBinding;
import com.example.cryptofun.data.CoinSymbol;
import com.example.cryptofun.data.CoinSymbols;
import com.example.cryptofun.data.KlineRequest;
import com.example.cryptofun.data.StatusOfKline;
import com.example.cryptofun.database.DBHandler;
import com.example.cryptofun.database.rawTable_Kline;
import com.example.cryptofun.retrofit.RetrofitClient;
import com.example.cryptofun.ui.view.GridViewAdapter;
import com.example.cryptofun.ui.view.GridViewElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ListView symbolList;
    private final List<String> listOfUSDTCryptos = new ArrayList<>();
    private List<String> listOfSymbols = new ArrayList<>();

    private DBHandler databaseDB;

    private static final String TABLE_NAME_KLINES_DATA = "klines_data";

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

        Button buttonCount = binding.countAlgorithm;
        buttonCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                for (int i = 0; i < listOfSymbols.size(); i++) {
                    Log.e("COUNTING", listOfSymbols.get(i));
                    countBestCryptoToBuy(listOfSymbols.get(i));
                }

            }
        });

    }

    public void setMainParametersOnView() {

        Bundle bundle = getArguments();
        Log.e("BUNDLELELEL", "SDASDASDASD" + listOfSymbols.toString() );
        if (bundle != null){
            GridView cryptoDataGrid = binding.cryptoCardView;
            symbolList = binding.symbolsList;


            listOfSymbols = bundle.getStringArrayList("listOfSymbols");
            Log.e("BUNDLELELEL", "23254646" + listOfSymbols.get(10) );
            ArrayList<GridViewElement> cryptoGridViewList = new ArrayList<>();
            cryptoGridViewList.add(get24hPercentChangeForCrypto("ETHUSDT"));
            cryptoGridViewList.add(get24hPercentChangeForCrypto("BTCUSDT"));
            cryptoGridViewList.add(get24hPercentChangeForCrypto("BNBUSDT"));
            cryptoGridViewList.add(get24hPercentChangeForCrypto("ADAUSDT"));
            cryptoGridViewList.add(get24hPercentChangeForCrypto("XRPUSDT"));
            cryptoGridViewList.add(get24hPercentChangeForCrypto("SOLUSDT"));
            ArrayList<GridViewElement> cryptoPercentageList = new ArrayList<>();

            for (int i = 0; i < listOfSymbols.size(); i++) {
                cryptoPercentageList.add(get24hPercentChangeForCrypto(listOfSymbols.get(i)));
                if (cryptoPercentageList.get(i) == null) {
                    cryptoPercentageList.remove(i);
                }
                Log.e("FRAGGGG:", String.valueOf(cryptoPercentageList.get(i).getPercent()));
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

            cryptoGridViewList.add(new GridViewElement("<1%", percentU2, under1));
            cryptoGridViewList.add(new GridViewElement("1% - 5%", percentB1and5, between1and5));
            cryptoGridViewList.add(new GridViewElement("5% - 15%", percentB5and15, between5and15));
            cryptoGridViewList.add(new GridViewElement(">15%", percentO15, over15));
            GridViewAdapter adapter = new GridViewAdapter(requireContext(), cryptoGridViewList);
            cryptoDataGrid.setAdapter(adapter);
        }
    }

    // Function checks what crypto is going to run in green
    private void countBestCryptoToBuy(String symbol) {

        List<StatusOfKline> status = new ArrayList<>();
        Cursor data = databaseDB.retrieveDataToFindBestCrypto(TABLE_NAME_KLINES_DATA, symbol);

        if (data.getCount() == 0) {
            Toast.makeText(getContext(), "Table is empty...", Toast.LENGTH_LONG).show();
        } else {
            while (data.moveToNext()) {
                float klinePlusMinus = data.getFloat(3) - data.getFloat(6);
                StatusOfKline temp;
                if (klinePlusMinus < 0) {
                    temp = new StatusOfKline(data.getString(10), 1);
                } else {
                    temp = new StatusOfKline(data.getString(10), 0);
                }
                status.add(temp);
                Log.e("DB KLINE 3:", String.valueOf(klinePlusMinus));
            }

            if (isKlineAcceptable(status)) {
                listOfUSDTCryptos.add(symbol + "  APPROVED  ");
            }

            status.clear();

        }
        List<String> sortedCryptoList = listOfUSDTCryptos.stream().sorted().collect(Collectors.toList());
        symbolList.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, sortedCryptoList));
    }

    // Function check if proviced result from function countBestCryptoToBuy matches required status
    public boolean isKlineAcceptable(List<StatusOfKline> status) {

        int nrOfGreenKlines3m = 3;
        int nrOfGreenKlines15m = 2;
        int nrOfGreenKlines1d = 2;
        int sumOf3m = 0;
        int sumOf15m = 0;
        int sumOf1d = 0;

        for (int i = 0; i < status.size(); i++) {

            if (status.get(i).getInterval().equals("3m")) {
                sumOf3m += status.get(i).getResult();
            } else if (status.get(i).getInterval().equals("15m")) {
                sumOf15m += status.get(i).getResult();
            } else {
                sumOf1d += status.get(i).getResult();
            }

        }

        if (sumOf3m == nrOfGreenKlines3m && sumOf15m == nrOfGreenKlines15m) {
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Calculate 24h change of price in %
    public GridViewElement get24hPercentChangeForCrypto(String symbol) {
        Cursor data = databaseDB.retrieveDataToFindBestCrypto(TABLE_NAME_KLINES_DATA, symbol);
        if (data.getCount() == 0) {
            Toast.makeText(getContext(), "Table is empty...", Toast.LENGTH_LONG).show();
            return null;
        } else {
            List<Long> openTime = new ArrayList<>();
            List<Float> openPrice = new ArrayList<>();
            List<Float> closePrice = new ArrayList<>();
            data.moveToFirst();
            Log.d("DATAFIRST", symbol + " " + data.getLong(2) + " " + data.getFloat(3) + " " + data.getFloat(6) + " " + data);


            do {
                if (data.getString(10).equals("1d")) {
                    openTime.add(data.getLong(2));
                    openPrice.add(data.getFloat(3));
                    closePrice.add(data.getFloat(6));
                }
            } while (data.moveToNext());
            if (openTime.size() > 1) {
                float percentOfChange = 0;
                Log.d("DATA LIST", symbol + " " + openTime.size() + " " + openPrice.size() + " " + closePrice.size());

                if (openTime.get(0) + 86400000 == openTime.get(1)) {
                    percentOfChange = ((closePrice.get(1) / openPrice.get(0)) * 100) - 100;
                }
                return new GridViewElement(symbol, percentOfChange, closePrice.get(1));
            }
            return new GridViewElement(symbol, 0, 0);
        }
    }

}