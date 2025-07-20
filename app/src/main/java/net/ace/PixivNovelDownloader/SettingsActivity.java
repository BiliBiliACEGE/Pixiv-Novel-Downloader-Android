package net.ace.PixivNovelDownloader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {
    private EditText etSavePath;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 在设置内容视图前应用主题
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        applyTheme(prefs);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 初始化UI组件
        Spinner spinner = findViewById(R.id.spinnerLanguage);
        Button btnSave = findViewById(R.id.btnSave);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switchTheme = findViewById(R.id.switchTheme);
        TextView tvTheme = findViewById(R.id.tvTheme);
        etSavePath = findViewById(R.id.etSavePath);

        // 防止空指针异常
        if (spinner == null || btnSave == null || switchTheme == null || tvTheme == null || etSavePath == null) {
            finish();
            return;
        }

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

        // =============== 路径显示逻辑 ===============
        // 获取默认路径
        String defaultPath = getDefaultSavePath();

        // 获取保存的路径（如果没有保存过，则使用默认路径）
        String savedPath = prefs.getString("save_path", "");

        // 如果保存的路径为空或者是默认路径的占位符，则显示默认路径
        if (TextUtils.isEmpty(savedPath)) {
            etSavePath.setText(defaultPath);
        } else {
            etSavePath.setText(savedPath);
        }

        // =============== 主题控制逻辑 ===============

        // 主题开关设置
        boolean isDark = prefs.getBoolean("dark_theme", false);
        switchTheme.setChecked(isDark);
        updateThemeText(tvTheme, isDark);

        // 主题切换监听器
        switchTheme.setOnCheckedChangeListener((buttonView, checked) -> {
            prefs.edit().putBoolean("dark_theme", checked).apply();
            updateThemeText(tvTheme, checked);
            applyTheme(prefs);
            recreate();
        });

        // 保存按钮监听器
        btnSave.setOnClickListener(v -> {
            int sel = spinner.getSelectedItemPosition();
            String selectedLang = values[sel];
            String inputPath = etSavePath.getText().toString().trim();

            // 如果用户清除了输入框，恢复为默认路径
            if (TextUtils.isEmpty(inputPath)) {
                inputPath = getDefaultSavePath();
                etSavePath.setText(inputPath); // 更新UI显示
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
    private void updateThemeText(TextView tvTheme, boolean isDark) {
        tvTheme.setText(isDark ? getString(R.string.theme_dark) : getString(R.string.theme_light));
    }

    /**
     * 获取默认保存路径
     */
    private String getDefaultSavePath() {
        // 创建默认目录（如果不存在）
        File defaultDir = getExternalFilesDir("Novels");
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