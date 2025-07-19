package net.ace.PixivNovelDownloader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import net.ace.PixivNovelDownloader.R;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    @Override
    protected void attachBaseContext(Context newBase) {
        // 读取用户语言设置并切换
        SharedPreferences prefs = newBase.getSharedPreferences("settings", MODE_PRIVATE);
        String lang = prefs.getString("language", "zh");
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang));
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置状态栏颜色为主题色
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottomNavigation);

        // 设置BottomNavigationView背景和图标色
        bottomNav.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
        bottomNav.setItemIconTintList(ContextCompat.getColorStateList(this, R.color.bottom_nav_icon_selector));
        bottomNav.setItemTextColor(ContextCompat.getColorStateList(this, R.color.bottom_nav_icon_selector));
        bottomNav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);

        // 圆角阴影美化（API 21+，如需兼容更低版本可忽略）
        bottomNav.setElevation(8f);
        bottomNav.setOutlineProvider(null);

        // 修复：确保Fragment构造函数无参，且包名一致
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new net.ace.PixivNovelDownloader.HomeFragment());
        fragments.add(new net.ace.PixivNovelDownloader.ProgressFragment());
        fragments.add(new net.ace.PixivNovelDownloader.HistoryFragment());

        ViewPagerAdapter adapter = new ViewPagerAdapter(this, fragments);
        viewPager.setAdapter(adapter);

        // 禁止滑动切换（如需支持滑动可移除此行）
        viewPager.setUserInputEnabled(false);

        // BottomNavigation点击切换页面
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                viewPager.setCurrentItem(0, false);
                return true;
            } else if (id == R.id.menu_progress) {
                viewPager.setCurrentItem(1, false);
                return true;
            } else if (id == R.id.menu_history) {
                viewPager.setCurrentItem(2, false);
                return true;
            }
            return false;
        });

        // ViewPager页面切换时同步BottomNavigation
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNav.setSelectedItemId(R.id.menu_home);
                        break;
                    case 1:
                        bottomNav.setSelectedItemId(R.id.menu_progress);
                        break;
                    case 2:
                        bottomNav.setSelectedItemId(R.id.menu_history);
                        break;
                }
            }
        });
    }

    public void switchToProgress() {
        if (viewPager != null) {
            viewPager.setCurrentItem(1, false);
        }
    }
}