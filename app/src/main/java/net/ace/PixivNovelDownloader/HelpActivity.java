package net.ace.PixivNovelDownloader;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {

    private ImageView btnBack;


    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("settings", MODE_PRIVATE);
        String lang = prefs.getString("language", "zh");
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // 初始化多语言帮助文本
        initHelpContent();
    }

    private void initHelpContent() {
        // 从XML资源获取多语言文本
        String helpTitle = getString(R.string.help_title);
        String helpContent = getString(R.string.help_content);

        // 设置标题栏文字
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(helpTitle);
        }

        // 绑定布局组件并设置内容
        TextView contentView = findViewById(R.id.tv_help_content);
        contentView.setText(helpContent);
    }
}