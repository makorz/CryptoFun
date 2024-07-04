package app.makorz.cryptofun.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import app.makorz.cryptofun.data.database.DBHandler;
import com.example.cryptofun.databinding.FragmentHomeBinding;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private FragmentHomeBinding binding;
    private final List<String> listOfUSDTCryptos = new ArrayList<>();
    private List<String> listOfSymbols = new ArrayList<>();

    private DBHandler databaseDB;

    private static final String TABLE_NAME_KLINES_DATA = "klines_data";

    public HomeViewModel(List<String> list) {
        listOfSymbols = list;
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}