package net.ace.PixivNovelDownloader;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    private EditText etInput;
    private Button btnDownload;
    private Button btnSettings;

    private Button btnHelp;

    private DownloadManager downloadManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        etInput = v.findViewById(R.id.etInput);
        btnDownload = v.findViewById(R.id.btnDownload);
        btnSettings = v.findViewById(R.id.btnSettings);
        btnHelp = v.findViewById(R.id.btnHelp);
        btnHelp.setOnClickListener(view -> {
            // 跳转到帮助页面
            Intent intent = new Intent(getActivity(), HelpActivity.class);
            startActivity(intent);
        });
        // 传入Context参数
        downloadManager = new DownloadManager(requireContext());

        btnDownload.setOnClickListener(view -> {
            String input = etInput.getText().toString().trim();
            if (TextUtils.isEmpty(input)) {
                Toast.makeText(getContext(), R.string.input_error, Toast.LENGTH_SHORT).show();
                return;
            }
            DownloadManager.ContentIdResult result = downloadManager.extractContentId(input);
            if (result == null) {
                Toast.makeText(getContext(), R.string.id_error, Toast.LENGTH_SHORT).show();
                return;
            }
            // 跳转到进度页
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToProgress();
            }
            // 启动下载
            downloadManager.downloadSingleNovel(getContext(), result.id, "PixivDownloads", "TXT", new DownloadManager.DownloadCallback() {
                @Override
                public void onProgress(int percent, String info) {
                    ProgressFragment.setProgress(percent, info);
                }
                @Override
                public void onSuccess(String title, String filePath) {
                    ProgressFragment.setProgress(100, R.string.download_complete + title);
                    HistoryManager.addHistory(getContext(), title + " - " + filePath);
                }
                @Override
                public void onError(String errorMsg) {
                    ProgressFragment.setProgress(0, R.string.pro_download_failed + errorMsg);
                }
            });
        });

        btnSettings.setOnClickListener(view -> {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        });

        return v;
    }
}
