package net.ace.PixivNovelDownloader;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class HistoryManager {
    private static final String PREFS_NAME = "HistoryPrefs";
    private static final String KEY_HISTORY = "history";

    // 保存历史记录 (保持不变)
    public static void addHistory(Context context, String item) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> historySet = new HashSet<>(prefs.getStringSet(KEY_HISTORY, new HashSet<>()));
        historySet.add(item); //  HashSet 自动处理重复项
        prefs.edit().putStringSet(KEY_HISTORY, historySet).apply();
    }

    // 获取历史记录 (保持不变)
    public static List<String> getHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> historySet = prefs.getStringSet(KEY_HISTORY, new HashSet<>());
        return new ArrayList<>(historySet); // 返回列表副本以允许修改
    }

    // 新增：清空所有历史记录
    public static void clearHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_HISTORY).apply();
    }

    // 新增：删除单条历史记录
    public static void removeHistoryItem(Context context, String item) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> historySet = new HashSet<>(prefs.getStringSet(KEY_HISTORY, new HashSet<>()));
        if (historySet.contains(item)) {
            historySet.remove(item);
            prefs.edit().putStringSet(KEY_HISTORY, historySet).apply();
        }
    }
}
