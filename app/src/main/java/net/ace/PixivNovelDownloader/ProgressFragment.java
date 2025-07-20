package net.ace.PixivNovelDownloader;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat; // Required for ContextCompat.getDrawable
import androidx.fragment.app.Fragment;

public class ProgressFragment extends Fragment {

    private ProgressBar progressBarInstance;
    private TextView tvProgressInstance;
    private static ProgressFragment currentInstance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_progress, container, false);

        progressBarInstance = v.findViewById(R.id.progressBar);
        tvProgressInstance = v.findViewById(R.id.tvProgress);

        currentInstance = this;

        if (tvProgressInstance != null) {
            tvProgressInstance.setText(R.string.progress_waiting);
        }
        if (progressBarInstance != null) {
            progressBarInstance.setProgress(0);
        }

        return v;
    }
    public static void setProgress(int percent, String info) {
        if (currentInstance != null && currentInstance.isAdded()) {
            currentInstance.getActivity().runOnUiThread(() -> {
                if (currentInstance.progressBarInstance != null) {
                    currentInstance.progressBarInstance.setProgress(percent);
                }
                if (currentInstance.tvProgressInstance != null) {
                    currentInstance.tvProgressInstance.setText(info);
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        progressBarInstance = null;
        tvProgressInstance = null;
        if (currentInstance == this) {
            currentInstance = null;
        }
    }

    public static void resetProgress() {
        if (currentInstance != null && currentInstance.isAdded()) {
            currentInstance.getActivity().runOnUiThread(() -> {
                if (currentInstance.progressBarInstance != null) {
                    currentInstance.progressBarInstance.setProgress(0);
                }
                if (currentInstance.tvProgressInstance != null && currentInstance.getContext() != null) {
                    currentInstance.tvProgressInstance.setText(currentInstance.getString(R.string.progress_waiting));
                }
            });
        }
    }
}
