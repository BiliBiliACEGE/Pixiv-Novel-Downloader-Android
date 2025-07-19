package net.ace.PixivNovelDownloader;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HistoryManager {

    private static final String PREFS = "history";
    private static final String KEY = "download_history";

    public static void addHistory(Context ctx, String item) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY, new HashSet<>()));
        set.add(item);
        prefs.edit().putStringSet(KEY, set).apply();
    }

    public static List<String> getHistory(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new ArrayList<>(prefs.getStringSet(KEY, new HashSet<>()));
    }

    public static void clearHistory(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY).apply();
    }
}
