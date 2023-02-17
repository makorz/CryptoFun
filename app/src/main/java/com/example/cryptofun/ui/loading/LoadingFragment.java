package com.example.cryptofun.ui.loading;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.cryptofun.databinding.FragmentLoadingBinding;


public class LoadingFragment extends Fragment {

    private FragmentLoadingBinding binding;
    private static final String TAG = "LoadingFrag";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        Log.e(TAG, "CreateView");
        LoadingViewModel loadingViewModel =
                new ViewModelProvider(this).get(LoadingViewModel.class);

        binding = FragmentLoadingBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        //final ProgressBar progress = binding.loader;
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}