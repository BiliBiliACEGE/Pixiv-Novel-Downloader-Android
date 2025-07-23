package net.ace.PixivNovelDownloader;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.documentfile.provider.DocumentFile;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadManager {
    private static final String TAG = "DownloadManager";
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

    public void downloadSingleNovel(Context ctx, String novelId, String saveSubDir, String format, DownloadCallback cb) {
        mainHandler.post(() -> cb.onProgress(0, ctx.getString(R.string.progress_fetching_novel)));
        String url = "https://www.pixiv.net/ajax/novel/" + novelId;
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", "https://www.pixiv.net/")
                .build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) {
                try {
                    String body = response.body().string();
                    JSONObject obj = new JSONObject(body);
                    if (obj.optBoolean("error")) {
                        mainHandler.post(() -> cb.onError(ctx.getString(R.string.error_pixiv_api)));
                        return;
                    }
                    JSONObject novel = obj.getJSONObject("body");
                    String title = novel.optString("title", ctx.getString(R.string.default_untitled_novel));
                    String content = novel.optString("content", "");

                    // 获取保存目录信息
                    SaveDirInfo saveDirInfo = getSaveDirInfo(ctx, saveSubDir);
                    if (saveDirInfo == null) {
                        mainHandler.post(() -> cb.onError(ctx.getString(R.string.error_save_dir)));
                        return;
                    }

                    // 生成文件名和MIME类型
                    String fileExtension = getFileExtension(format);
                    String fileName = title.replaceAll("[\\\\/:*?\"<>|]", "") + "." + fileExtension;
                    String mimeType = getMimeType(format);

                    // 创建文件内容
                    String fileContent;
                    switch (format.toLowerCase()) {
                        case "html":
                            fileContent = "<html><body><h1>" + title + "</h1><div>" + content.replace("\n", "<br>") + "</div></body></html>";
                            break;
                        case "markdown":
                            fileContent = "# " + title + "\n\n" + content;
                            break;
                        default:
                            fileContent = content;
                    }

                    // 保存文件
                    String filePath = saveFile(ctx, saveDirInfo, fileName, mimeType, fileContent);

                    if (filePath != null) {
                        mainHandler.post(() -> {
                            cb.onProgress(100, ctx.getString(R.string.progress_download_complete));
                            cb.onSuccess(title, filePath);
                        });
                    } else {
                        mainHandler.post(() -> cb.onError(ctx.getString(R.string.error_write_failed)));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> cb.onError(ctx.getString(R.string.error_parse_failed) + e.getMessage()));
                }
            }
        });
    }

    // =============== 存储辅助方法 ===============
    private static class SaveDirInfo {
        public final boolean isSaf;
        public final DocumentFile safDir;   // SAF目录
        public final File fileDir;          // 传统文件目录
        public final String path;            // 路径信息

        public SaveDirInfo(DocumentFile safDir) {
            this.isSaf = true;
            this.safDir = safDir;
            this.fileDir = null;
            this.path = safDir.getUri().toString();
        }

        public SaveDirInfo(File fileDir) {
            this.isSaf = false;
            this.safDir = null;
            this.fileDir = fileDir;
            this.path = fileDir.getAbsolutePath();
        }
    }

    private SaveDirInfo getSaveDirInfo(Context context, String subDir) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String uriString = prefs.getString("save_path_uri", null);
        String path = prefs.getString("save_path", null);

        // 优先使用SAF URI
        if (!TextUtils.isEmpty(uriString)) {
            try {
                Uri treeUri = Uri.parse(uriString);
                DocumentFile rootDir = DocumentFile.fromTreeUri(context, treeUri);

                if (rootDir != null && rootDir.exists() && rootDir.canWrite()) {
                    // 处理子目录
                    if (!TextUtils.isEmpty(subDir)) {
                        DocumentFile subDirFile = findOrCreateDirectory(rootDir, subDir);
                        if (subDirFile != null) {
                            return new SaveDirInfo(subDirFile);
                        }
                    }
                    return new SaveDirInfo(rootDir);
                }
            } catch (SecurityException e) {
            }
        }

        // 使用传统路径
        File baseDir;
        if (!TextUtils.isEmpty(path)) {
            baseDir = new File(path);
        } else {
            baseDir = context.getExternalFilesDir(null);
        }

        // 处理子目录
        if (!TextUtils.isEmpty(subDir)) {
            baseDir = new File(baseDir, subDir);
        }

        // 确保目录存在
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            return null;
        }

        return new SaveDirInfo(baseDir);
    }

    private DocumentFile findOrCreateDirectory(DocumentFile parent, String dirName) {
        // 查找现有目录
        DocumentFile dir = parent.findFile(dirName);
        if (dir != null && dir.exists()) {
            return dir;
        }

        // 创建新目录
        return parent.createDirectory(dirName);
    }

    private String saveFile(Context context, SaveDirInfo saveDirInfo, String fileName,
                            String mimeType, String content) {
        try {
            if (saveDirInfo.isSaf) {
                // SAF方式保存
                DocumentFile dir = saveDirInfo.safDir;
                DocumentFile existingFile = dir.findFile(fileName);

                // 删除已存在文件
                if (existingFile != null && existingFile.exists()) {
                    existingFile.delete();
                }

                // 创建新文件
                DocumentFile newFile = dir.createFile(mimeType, fileName);
                if (newFile == null) {
                    return null;
                }

                // 写入内容
                try (OutputStream os = context.getContentResolver().openOutputStream(newFile.getUri())) {
                    os.write(content.getBytes("UTF-8"));
                }
                return newFile.getUri().toString();
            } else {
                // 传统方式保存
                File file = new File(saveDirInfo.fileDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(content.getBytes("UTF-8"));
                }
                return file.getAbsolutePath();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String getFileExtension(String format) {
        switch (format.toLowerCase()) {
            case "html": return "html";
            case "markdown": return "md";
            default: return "txt";
        }
    }

    private String getMimeType(String format) {
        switch (format.toLowerCase()) {
            case "html": return "text/html";
            case "markdown": return "text/markdown";
            default: return "text/plain";
        }
    }

    // =============== 系列下载方法 ===============
    public void downloadSeries(String seriesId, String saveSubDir, String format, DownloadCallback cb) {
        mainHandler.post(() -> cb.onProgress(0, context.getString(R.string.progress_fetching_series)));
        String url = "https://www.pixiv.net/ajax/novel/series/" + seriesId;
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Referer", "https://www.pixiv.net/")
                .build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> cb.onError(context.getString(R.string.error_network) + e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) {
                try {
                    String body = response.body().string();
                    JSONObject obj = new JSONObject(body);
                    if (obj.optBoolean("error")) {
                        mainHandler.post(() -> cb.onError(context.getString(R.string.error_pixiv_api)));
                        return;
                    }
                    JSONObject series = obj.getJSONObject("body");
                    String seriesTitle = series.optString("title", context.getString(R.string.default_untitled_series));

                    // 获取系列保存目录
                    SaveDirInfo seriesDirInfo = getSaveDirInfo(context,
                            saveSubDir + "/" + seriesTitle.replaceAll("[\\\\/:*?\"<>|]", ""));

                    if (seriesDirInfo == null) {
                        mainHandler.post(() -> cb.onError(context.getString(R.string.error_save_dir)));
                        return;
                    }

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
                        mainHandler.post(() -> cb.onError(context.getString(R.string.error_empty_series)));
                        return;
                    }

                    // 批量下载
                    int total = novelIds.size();
                    int[] successCount = {0};
                    for (int i = 0; i < total; i++) {
                        int idx = i;
                        String nid = novelIds.get(i);
                        downloadSingleNovel(context, nid, null, format, new DownloadCallback() {
                            @Override
                            public void onProgress(int percent, String info) {
                                int progress = (int) (((idx + percent / 100.0) / total) * 100);
                                mainHandler.post(() -> cb.onProgress(progress,
                                        context.getString(R.string.progress_series_download) + (idx + 1) + "/" + total));
                            }
                            @Override
                            public void onSuccess(String title, String filePath) {
                                successCount[0]++;
                                if (successCount[0] == total) {
                                    mainHandler.post(() -> cb.onSuccess(seriesTitle, seriesDirInfo.path));
                                }
                            }
                            @Override
                            public void onError(String errorMsg) {
                                // 记录错误但继续
                                if (++successCount[0] == total) {
                                    mainHandler.post(() -> cb.onSuccess(seriesTitle, seriesDirInfo.path));
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> cb.onError(context.getString(R.string.error_parse_failed) + e.getMessage()));
                }
            }
        });
    }

    // =============== 批量下载方法 ===============
    public void batchDownload(List<String> inputs, String saveSubDir, String format, DownloadCallback cb) {
        if (inputs == null || inputs.isEmpty()) {
            mainHandler.post(() -> cb.onError(context.getString(R.string.error_empty_batch)));
            return;
        }

        // 获取保存目录
        SaveDirInfo batchDirInfo = getSaveDirInfo(context, saveSubDir);
        if (batchDirInfo == null) {
            mainHandler.post(() -> cb.onError(context.getString(R.string.error_save_dir)));
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
                mainHandler.post(() -> cb.onProgress((int) ((idx + 1) * 100.0 / total),
                        context.getString(R.string.error_invalid_input) + input));
                finished[0]++;
                continue;
            }

            DownloadCallback innerCb = new DownloadCallback() {
                @Override
                public void onProgress(int percent, String info) {
                    int progress = (int) (((idx + percent / 100.0) / total) * 100);
                    mainHandler.post(() -> cb.onProgress(progress,
                            context.getString(R.string.progress_batch_download) + (idx + 1) + "/" + total));
                }
                @Override
                public void onSuccess(String title, String filePath) {
                    success[0]++;
                    finished[0]++;
                    if (finished[0] == total) {
                        mainHandler.post(() -> cb.onSuccess(
                                context.getString(R.string.progress_batch_complete),
                                context.getString(R.string.progress_batch_success) + success[0] + "/" + total));
                    }
                }
                @Override
                public void onError(String errorMsg) {
                    finished[0]++;
                    if (finished[0] == total) {
                        mainHandler.post(() -> cb.onSuccess(
                                context.getString(R.string.progress_batch_complete),
                                context.getString(R.string.progress_batch_success) + success[0] + "/" + total));
                    }
                }
            };

            if ("novel".equals(result.type)) {
                downloadSingleNovel(context, result.id, null, format, innerCb);
            } else if ("series".equals(result.type)) {
                downloadSeries(result.id, saveSubDir, format, innerCb);
            }
        }
    }
}