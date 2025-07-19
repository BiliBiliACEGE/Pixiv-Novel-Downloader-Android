package net.ace.PixivNovelDownloader;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import com.google.gson.Gson;
import okhttp3.*;
import org.json.JSONObject;
import timber.log.Timber;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class DownloadManager {
    private final Context context;
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public DownloadManager(Context ctx) {
        this.context = ctx;
    }

    public interface DownloadCallback {
        void onProgress(int percent, String info);
        void onSuccess(String title, String filePath);
        void onError(String errorMsg);
    }

    // 提取ID和类型
    public static class ContentIdResult {
        public final String type;
        public final String id;
        public ContentIdResult(String type, String id) {
            this.type = type;
            this.id = id;
        }
    }

    public ContentIdResult extractContentId(String input) {
        String[] patterns = {
                "novel/show\\.php\\?id=(\\d+)",
                "novel/.*?id=(\\d+)",
                "novel/(\\d+)",
                "n/(\\d+)",
                "series/(\\d+)",
                "works/(\\d+)",
                "id=(\\d+)",
                "^(\\d+)$"
        };
        for (String pattern : patterns) {
            Matcher m = Pattern.compile(pattern).matcher(input.trim());
            if (m.find()) {
                String id = m.group(1);
                String type = pattern.contains("series") ? "series" : "novel";
                return new ContentIdResult(type, id);
            }
        }
        return null;
    }

    public void downloadNovel(String input, String saveDir, String format, DownloadCallback cb) {
        // 提取ID和类型
        ContentIdResult result = extractContentId(input);
        if (result == null) {
            cb.onError(context.getString(R.string.invalid_id));
            return;
        }
        if ("novel".equals(result.type)) {
            downloadSingleNovel(context, result.id, saveDir, format, cb);
        } else if ("series".equals(result.type)) {
            downloadSeries(result.id, saveDir, format, cb);
        }
    }

    public void downloadSingleNovel(Context ctx, String novelId, String saveDir, String format, DownloadCallback cb) {
        mainHandler.post(() -> cb.onProgress(0, "获取小说信息..."));
        String url = "https://www.pixiv.net/ajax/novel/" + novelId;
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", "https://www.pixiv.net/")
                .build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> cb.onError("网络错误: " + e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) {
                try {
                    String body = response.body().string();
                    JSONObject obj = new JSONObject(body);
                    if (obj.optBoolean("error")) {
                        mainHandler.post(() -> cb.onError("Pixiv接口错误"));
                        return;
                    }
                    JSONObject novel = obj.getJSONObject("body");
                    String title = novel.optString("title", "未命名小说");
                    String content = novel.optString("content", "");
                    String ext = "txt";
                    String fileContent = content;
                    if ("HTML".equalsIgnoreCase(format)) {
                        fileContent = "<html><body><h1>" + title + "</h1><div>" + content.replace("\n", "<br>") + "</div></body></html>";
                        ext = "html";
                    } else if ("Markdown".equalsIgnoreCase(format)) {
                        fileContent = "# " + title + "\n\n" + content;
                        ext = "md";
                    }
                    File dir = new File(ctx.getExternalFilesDir(null), saveDir);
                    if (!dir.exists()) dir.mkdirs();
                    String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "");
                    File file = new File(dir, safeTitle + "." + ext);
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(fileContent.getBytes("UTF-8"));
                    }
                    mainHandler.post(() -> {
                        cb.onProgress(100, "下载完成");
                        cb.onSuccess(title, file.getAbsolutePath());
                    });
                } catch (Exception e) {
                    mainHandler.post(() -> cb.onError("解析失败: " + e.getMessage()));
                }
            }
        });
    }

    public void downloadSeries(String seriesId, String saveDir, String format, DownloadCallback cb) {
        mainHandler.post(() -> cb.onProgress(0, "获取系列信息..."));
        String url = "https://www.pixiv.net/ajax/novel/series/" + seriesId;
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", "https://www.pixiv.net/")
                .build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> cb.onError("网络错误: " + e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) {
                try {
                    String body = response.body().string();
                    JSONObject obj = new JSONObject(body);
                    if (obj.optBoolean("error")) {
                        mainHandler.post(() -> cb.onError("Pixiv接口错误"));
                        return;
                    }
                    JSONObject series = obj.getJSONObject("body");
                    String seriesTitle = series.optString("title", "未命名系列");
                    // 获取小说ID列表
                    List<String> novelIds = new ArrayList<>();
                    if (series.has("seriesContents")) {
                        JSONObject contents = series.getJSONObject("seriesContents");
                        if (contents.has("contents")) {
                            for (int i = 0; i < contents.getJSONArray("contents").length(); i++) {
                                JSONObject item = contents.getJSONArray("contents").getJSONObject(i);
                                String nid = item.optString("id", null);
                                if (nid != null) novelIds.add(nid);
                            }
                        }
                    }
                    if (novelIds.isEmpty()) {
                        mainHandler.post(() -> cb.onError("系列中没有小说"));
                        return;
                    }
                    // 创建系列目录
                    File dir = new File(context.getExternalFilesDir(null), saveDir + "/" + seriesTitle.replaceAll("[\\\\/:*?\"<>|]", ""));
                    if (!dir.exists()) dir.mkdirs();
                    // 批量下载
                    int total = novelIds.size();
                    int[] successCount = {0};
                    for (int i = 0; i < total; i++) {
                        int idx = i;
                        String nid = novelIds.get(i);
                        downloadSingleNovel(context, nid, dir.getName(), format, new DownloadCallback() {
                            @Override
                            public void onProgress(int percent, String info) {
                                int progress = (int) (((idx + percent / 100.0) / total) * 100);
                                mainHandler.post(() -> cb.onProgress(progress, "系列下载: " + (idx + 1) + "/" + total));
                            }
                            @Override
                            public void onSuccess(String title, String filePath) {
                                successCount[0]++;
                                if (successCount[0] == total) {
                                    mainHandler.post(() -> cb.onSuccess(seriesTitle, dir.getAbsolutePath()));
                                }
                            }
                            @Override
                            public void onError(String errorMsg) {
                                // 记录错误但继续
                            }
                        });
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> cb.onError("解析失败: " + e.getMessage()));
                }
            }
        });
    }

    public void batchDownload(List<String> inputs, String saveDir, String format, DownloadCallback cb) {
        if (inputs == null || inputs.isEmpty()) {
            mainHandler.post(() -> cb.onError("批量下载输入为空"));
            return;
        }
        int total = inputs.size();
        int[] finished = {0};
        int[] success = {0};
        for (int i = 0; i < total; i++) {
            int idx = i;
            String input = inputs.get(i);
            ContentIdResult result = extractContentId(input);
            if (result == null) {
                mainHandler.post(() -> cb.onProgress((int) ((idx + 1) * 100.0 / total), "无效输入: " + input));
                finished[0]++;
                continue;
            }
            DownloadCallback innerCb = new DownloadCallback() {
                @Override
                public void onProgress(int percent, String info) {
                    int progress = (int) (((idx + percent / 100.0) / total) * 100);
                    mainHandler.post(() -> cb.onProgress(progress, "批量下载: " + (idx + 1) + "/" + total));
                }
                @Override
                public void onSuccess(String title, String filePath) {
                    success[0]++;
                    finished[0]++;
                    if (finished[0] == total) {
                        mainHandler.post(() -> cb.onSuccess("批量下载完成", "成功: " + success[0] + "/" + total));
                    }
                }
                @Override
                public void onError(String errorMsg) {
                    finished[0]++;
                    if (finished[0] == total) {
                        mainHandler.post(() -> cb.onSuccess("批量下载完成", "成功: " + success[0] + "/" + total));
                    }
                }
            };
            if ("novel".equals(result.type)) {
                downloadSingleNovel(context, result.id, saveDir, format, innerCb);
            } else if ("series".equals(result.type)) {
                downloadSeries(result.id, saveDir, format, innerCb);
            }
        }
    }
}