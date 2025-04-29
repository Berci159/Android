package hu.mobilalk.gazorabejelentes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReadingAdapter extends RecyclerView.Adapter<ReadingAdapter.ViewHolder> {

    private final Context context;
    private final List<MeterReading> readings;
    private final OnReadingListener onReadingListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.", Locale.getDefault());

    public ReadingAdapter(Context context, List<MeterReading> readings, OnReadingListener onReadingListener) {
        this.context = context;
        this.readings = readings;
        this.onReadingListener = onReadingListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.reading_list_item, parent, false);
        return new ViewHolder(view, onReadingListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MeterReading reading = readings.get(position);

        String dateStr = dateFormat.format(reading.getDateAsDate());

        holder.tvDate.setText(dateStr);
        holder.tvReading.setText(String.format(Locale.getDefault(), "%.1f m³", reading.getReading()));

        holder.btnEdit.setOnClickListener(v -> onReadingListener.onEditClick(holder.getAdapterPosition()));
        holder.btnDelete.setOnClickListener(v -> onReadingListener.onDeleteClick(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return readings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvReading;
        ImageButton btnEdit;
        ImageButton btnDelete;
        OnReadingListener onReadingListener;

        public ViewHolder(@NonNull View itemView, OnReadingListener onReadingListener) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvReading = itemView.findViewById(R.id.tvReading);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            this.onReadingListener = onReadingListener;
        }
    }

    public interface OnReadingListener {
        void onEditClick(int position);
        void onDeleteClick(int position);
    }
}