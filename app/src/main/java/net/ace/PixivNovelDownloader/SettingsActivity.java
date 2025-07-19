package net.ace.PixivNovelDownloader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.view.View;
import android.widget.TextView;
import android.widget.Switch;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Spinner spinner = findViewById(R.id.spinnerLanguage);
        Button btnSave = findViewById(R.id.btnSave);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switchTheme = findViewById(R.id.switchTheme);
        TextView tvTheme = findViewById(R.id.tvTheme);

        // 防止空指针异常
        if (spinner == null || btnSave == null || switchTheme == null || tvTheme == null) {
            finish();
            return;
        }

        String[] langs = {"简体中文", "English", "日本語"};
        String[] values = {"zh", "en", "ja"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, langs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String lang = prefs.getString("language", "zh");
        int idx = 0;
        if ("en".equals(lang)) idx = 1;
        else if ("ja".equals(lang)) idx = 2;
        spinner.setSelection(idx);

        // 主题切换
        boolean isDark = prefs.getBoolean("dark_theme", false);
        switchTheme.setChecked(isDark);
        tvTheme.setText(isDark ? getString(R.string.theme_dark) : getString(R.string.theme_light));
        switchTheme.setOnCheckedChangeListener((buttonView, checked) -> {
            prefs.edit().putBoolean("dark_theme", checked).apply();
            tvTheme.setText(checked ? getString(R.string.theme_dark) : getString(R.string.theme_light));
            AppCompatDelegate.setDefaultNightMode(
                    checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            // 立即重启设置界面以生效
            recreate();
        });

        btnSave.setOnClickListener(v -> {
            int sel = spinner.getSelectedItemPosition();
            String selectedLang = values[sel];
            prefs.edit().putString("language", selectedLang).apply();
            // 立即切换语言并重启主界面
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
