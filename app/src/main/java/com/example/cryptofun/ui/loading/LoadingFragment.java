package com.example.cryptofun.ui.loading;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.cryptofun.databinding.FragmentLoadingBinding;
import com.example.cryptofun.databinding.FragmentNotificationsBinding;

public class LoadingFragment extends Fragment {

    private FragmentLoadingBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        LoadingViewModel loadingViewModel =
                new ViewModelProvider(this).get(LoadingViewModel.class);

        binding = FragmentLoadingBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        final ProgressBar progress = binding.loader;
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}