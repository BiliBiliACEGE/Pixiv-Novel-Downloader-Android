package net.ace.PixivNovelDownloader;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryFragment extends Fragment {
    private RecyclerView rvHistory;
    private TextView tvHistoryTitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history, container, false);
        rvHistory = v.findViewById(R.id.rvHistory);
        tvHistoryTitle = v.findViewById(R.id.tvHistoryTitle);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        List<String> history = HistoryManager.getHistory(getContext());
        rvHistory.setAdapter(new HistoryAdapter(history));
        return v;
    }

    static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        private final List<String> data;
        HistoryAdapter(List<String> d) { data = d; }
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(16, 16, 16, 16);
            return new VH(tv);
        }
        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.tv.setText(data.get(position));
        }
        @Override
        public int getItemCount() { return data.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tv;
            VH(@NonNull View itemView) { super(itemView); tv = (TextView) itemView; }
        }
    }
}
