package dev.josegaldamez.bitacora_de_campo.views;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import dev.josegaldamez.bitacora_de_campo.R;
import dev.josegaldamez.bitacora_de_campo.controllers.RecordController;
import dev.josegaldamez.bitacora_de_campo.models.Record;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {

    public interface OnRecordClickListener {
        void onRecordClick(Record record);
        void onDeleteClick(Record record);
    }

    private List<Record> recordList = new ArrayList<>();
    private final OnRecordClickListener listener;

    public RecordAdapter(OnRecordClickListener listener) {
        this.listener = listener;
    }

    public void setRecords(List<Record> records) {
        this.recordList = records != null ? records : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        Record record = recordList.get(position);
        holder.bind(record, listener);
    }

    @Override
    public int getItemCount() {
        return recordList.size();
    }

    public static class RecordViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvTitle;
        TextView tvDate;
        TextView tvDescription;
        ImageButton btnDelete;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_record_photo);
            tvTitle = itemView.findViewById(R.id.tv_record_title);
            tvDate = itemView.findViewById(R.id.tv_record_date);
            tvDescription = itemView.findViewById(R.id.tv_record_description);
            btnDelete = itemView.findViewById(R.id.btn_delete_record);
        }

        public void bind(Record record, OnRecordClickListener listener) {
            tvTitle.setText(record.getTitulo());


            try {
                OffsetDateTime fecha = OffsetDateTime.parse(record.getFecha());

                DateTimeFormatter formato = DateTimeFormatter.ofPattern(
                        "d 'de' MMMM 'de' yyyy, h:mm a",
                        new Locale("es", "HN")
                );

                String fechaLegible = fecha.atZoneSameInstant(
                        java.time.ZoneId.systemDefault()
                ).format(formato);

                tvDate.setText(fechaLegible);

            } catch (Exception e) {
                tvDate.setText(record.getFecha());
            }

            tvDescription.setText(record.getDescripcion());

            Bitmap bitmap = RecordController.base64ToBitmap(record.getFotografiaBase64());
            if (bitmap != null) {
                ivPhoto.setPadding(0, 0, 0, 0);
                ivPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                ivPhoto.setImageBitmap(bitmap);
            } else {
                int paddingPx = (int) (18 * itemView.getResources().getDisplayMetrics().density);
                ivPhoto.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
                ivPhoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
                ivPhoto.setImageResource(R.drawable.ic_camera_placeholder);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onRecordClick(record);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(record);
            });
        }
    }
}
