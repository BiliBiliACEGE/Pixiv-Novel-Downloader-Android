package net.ace.PixivNovelDownloader;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProgressFragment extends Fragment {
    @SuppressLint("StaticFieldLeak")
    private static ProgressBar progressBar;
    @SuppressLint("StaticFieldLeak")
    private static TextView tvProgress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_progress, container, false);
        progressBar = v.findViewById(R.id.progressBar);
        tvProgress = v.findViewById(R.id.tvProgress);
        return v;
    }

    public static void setProgress(int percent, String info) {
        if (progressBar != null && tvProgress != null) {
            progressBar.setProgress(percent);
            tvProgress.setText(info);
        }
    }
}
