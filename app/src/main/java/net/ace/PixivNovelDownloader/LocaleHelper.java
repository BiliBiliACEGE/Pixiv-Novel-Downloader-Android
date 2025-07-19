package net.ace.PixivNovelDownloader;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import java.util.Locale;

public class LocaleHelper {
    public static Context setLocale(Context context, String language) {
        Locale locale;
        switch (language) {
            case "en":
                locale = Locale.ENGLISH;
                break;
            case "ja":
                locale = Locale.JAPANESE;
                break;
            default:
                locale = Locale.SIMPLIFIED_CHINESE;
        }
        Locale.setDefault(locale);
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
            return context;
        }
    }

    public static String getCurrentLanguage(Context context) {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            locale = context.getResources().getConfiguration().locale;
        }
        String lang = locale.getLanguage();
        if (lang.equals("zh")) return "zh";
        if (lang.equals("ja")) return "ja";
        return "en";
    }
}
