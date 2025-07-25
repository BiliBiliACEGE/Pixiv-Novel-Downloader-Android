// HistoryFragment.java
package net.ace.PixivNovelDownloader;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // 导入 Button
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog; // 导入 AlertDialog
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList; // 确保导入 ArrayList
import java.util.List;

public class HistoryFragment extends Fragment {
    private RecyclerView rvHistory;
    private TextView tvHistoryTitle;
    private Button btnClearHistory; // 添加清空按钮的引用
    private HistoryAdapter adapter; // 将 adapter 提升为成员变量
    private List<String> historyList; // 将历史数据列表提升为成员变量

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history, container, false);
        rvHistory = v.findViewById(R.id.rvHistory);
        tvHistoryTitle = v.findViewById(R.id.tvHistoryTitle);
        btnClearHistory = v.findViewById(R.id.btnClearHistory); // 初始化清空按钮

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        historyList = HistoryManager.getHistory(getContext()); // 初始化 historyList
        adapter = new HistoryAdapter(historyList, new HistoryAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(String item, int position) {
                showDeleteConfirmationDialog(item, position);
            }
        });
        rvHistory.setAdapter(adapter);

        // 设置清空按钮的点击监听器
        btnClearHistory.setOnClickListener(view -> showClearAllConfirmationDialog());

        return v;
    }

    // 显示清空所有历史记录的确认对话框
    private void showClearAllConfirmationDialog() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.dialog_title)
                .setMessage(R.string.dialog_message)
                .setPositiveButton(R.string.clear_button, (dialog, which) -> {
                    HistoryManager.clearHistory(getContext());
                    historyList.clear(); // 清空列表
                    adapter.notifyDataSetChanged(); // 通知适配器数据已更改
                    // 你也可以选择重新加载数据：
                    // historyList.clear();
                    // historyList.addAll(HistoryManager.getHistory(getContext()));
                    // adapter.notifyDataSetChanged();
                })
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }


    // 显示删除单条历史记录的确认对话框
    private void showDeleteConfirmationDialog(String item, int position) {
        String message = getString(R.string.remove_history_message) + "\n\"" + item + "\"";
        if (getContext() == null) return; // 检查 context 是否为 null
        new AlertDialog.Builder(getContext()) // 使用 getContext()
                .setTitle(R.string.remove_history)
                .setMessage(message)
                .setPositiveButton(R.string.delete_button, (dialog, which) -> {
                    HistoryManager.removeHistoryItem(getContext(), item); // 从 SharedPreferences 中删除
                    historyList.remove(position); // 从当前列表中删除
                    adapter.notifyItemRemoved(position); // 通知适配器特定项已删除
                    adapter.notifyItemRangeChanged(position, historyList.size()); // 更新后续项目的位置
                })
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }


    static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        private final List<String> data;
        private final OnItemLongClickListener longClickListener; // 长按监听器接口

        // 定义长按监听器接口
        interface OnItemLongClickListener {
            void onItemLongClick(String item, int position);
        }

        HistoryAdapter(List<String> d, OnItemLongClickListener listener) {
            data = d;
            longClickListener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(16, 16, 16, 16);
            // 可以设置 TextView 的其他属性，例如 textSize, textColor 等
            // tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String currentItem = data.get(position);
            holder.tv.setText(currentItem);

            // 设置长按监听器
            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(currentItem, holder.getAdapterPosition());
                }
                return true; // 返回 true 表示事件已消费
            });
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tv;
            VH(@NonNull View itemView) {
                super(itemView);
                tv = (TextView) itemView;
            }
        }
    }
}
