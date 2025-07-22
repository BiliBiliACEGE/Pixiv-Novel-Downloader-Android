package net.ace.PixivNovelDownloader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_CHOOSE_DIR = 1001;

    private EditText etSavePath;
    private SharedPreferences prefs;
    private ImageView btnBack;
    private Button btnChoosePath;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("settings", MODE_PRIVATE);
        String lang = prefs.getString("language", "zh");
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang));
    }

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        applyTheme(prefs);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Spinner spinner = findViewById(R.id.spinnerLanguage);
        Button btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switchTheme = findViewById(R.id.switchTheme);
        etSavePath = findViewById(R.id.etSavePath);
        btnChoosePath = findViewById(R.id.btnChoosePath);

        if (spinner == null || btnSave == null || switchTheme == null ||
                switchTheme == null || etSavePath == null || btnBack == null || btnChoosePath == null) {
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> finish());
        btnChoosePath.setOnClickListener(v -> openDirectoryChooser());

        // 语言选择器设置
        String[] langs = {"简体中文", "English", "日本語"};
        String[] values = {"zh", "en", "ja"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, langs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        String lang = prefs.getString("language", "zh");
        int idx = 0;
        if ("en".equals(lang)) idx = 1;
        else if ("ja".equals(lang)) idx = 2;
        spinner.setSelection(idx);

        // =============== 修复的路径显示逻辑 ===============
        String savedPath = prefs.getString("save_path", "");

        // 修复旧版本路径不一致问题
        if (TextUtils.isEmpty(savedPath) || savedPath.contains("PixivNovels")) {
            savedPath = getDefaultSavePath();
            // 更新存储的路径
            prefs.edit().putString("save_path", savedPath).apply();
        }
        etSavePath.setText(savedPath);

        // =============== 主题控制逻辑 ===============
        boolean isDark = prefs.getBoolean("dark_theme", false);
        switchTheme.setChecked(isDark);
        updateThemeText(switchTheme, isDark);

        switchTheme.setOnCheckedChangeListener((buttonView, checked) -> {
            prefs.edit().putBoolean("dark_theme", checked).apply();
            updateThemeText(switchTheme, checked);
            applyTheme(prefs);
            recreate();
        });

        // 保存按钮监听器
        btnSave.setOnClickListener(v -> {
            int sel = spinner.getSelectedItemPosition();
            String selectedLang = values[sel];
            String inputPath = etSavePath.getText().toString().trim();

            // 验证路径有效性
            if (!isPathValid(inputPath)) {
                Toast.makeText(this, R.string.invalid_path_message, Toast.LENGTH_LONG).show();
                inputPath = getDefaultSavePath();
                etSavePath.setText(inputPath);
            }

            prefs.edit()
                    .putString("language", selectedLang)
                    .putString("save_path", inputPath)
                    .apply();

            Toast.makeText(this,
                    getString(R.string.settings_saved) + ": " + inputPath,
                    Toast.LENGTH_LONG).show();

            restartMainActivity();
        });
    }

    // =============== 路径选择方法 ===============
    private void openDirectoryChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CODE_CHOOSE_DIR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE_DIR && resultCode == RESULT_OK) {
            if (data != null) {
                Uri treeUri = data.getData();
                if (treeUri != null) {
                    getContentResolver().takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    );

                    DocumentFile docFile = DocumentFile.fromTreeUri(this, treeUri);
                    if (docFile != null && docFile.exists()) {
                        String path = docFile.getUri().getPath();
                        if (path != null && path.startsWith("/tree/")) {
                            path = path.replace("/tree/", "");
                        }
                        etSavePath.setText(path);
                    }
                }
            }
        }
    }

    // =============== 路径验证方法 ===============
    private boolean isPathValid(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }

        try {
            File dir = new File(path);
            return dir.exists() ? dir.isDirectory() && dir.canWrite() : dir.mkdirs();
        } catch (SecurityException e) {
            return false;
        }
    }

    /**
     * 应用主题到整个应用
     */
    private void applyTheme(SharedPreferences prefs) {
        boolean isDark = prefs.getBoolean("dark_theme", false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    /**
     * 更新主题文本显示
     */
    private void updateThemeText(TextView switchTheme, boolean isDark) {
        switchTheme.setText(isDark ? getString(R.string.theme_switch) : getString(R.string.theme_light));
    }

    /**
     * 获取正确的默认保存路径（与实际下载路径一致）
     */
    private String getDefaultSavePath() {
        // 使用与下载器相同的路径
        File defaultDir = new File(getExternalFilesDir(null), "PixivDownloads");

        // 处理可能的null情况
        if (defaultDir == null) {
            defaultDir = new File(Environment.getExternalStorageDirectory(),
                    "Android/data/net.ace.PixivNovelDownloader/files/PixivDownloads");
        }

        if (!defaultDir.exists()) {
            defaultDir.mkdirs();
        }
        return defaultDir.getAbsolutePath();
    }

    /**
     * 重启主界面
     */
    private void restartMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}