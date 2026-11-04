package dev.josegaldamez.bitacora_de_campo.views;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

import dev.josegaldamez.bitacora_de_campo.R;
import dev.josegaldamez.bitacora_de_campo.controllers.RecordController;
import dev.josegaldamez.bitacora_de_campo.models.Record;

public class DetailRecordActivity extends AppCompatActivity {

    public static final String EXTRA_RECORD_ID = "extra_record_id";

    private RecordController recordController;
    private Record currentRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_record);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recordController = new RecordController(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ImageView ivPhoto = findViewById(R.id.iv_detail_photo);
        TextView tvTitle = findViewById(R.id.tv_detail_title);
        TextView tvDate = findViewById(R.id.tv_detail_date);
        TextView tvDescription = findViewById(R.id.tv_detail_description);
        Button btnDelete = findViewById(R.id.btn_delete_detail);

        int recordId = getIntent().getIntExtra(EXTRA_RECORD_ID, -1);
        if (recordId != -1) {
            currentRecord = recordController.getRecordById(recordId);
        }

        if (currentRecord == null) {
            Toast.makeText(this, "No se pudo cargar el registro", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTitle.setText(currentRecord.getTitulo());
        tvDate.setText(currentRecord.getFecha());
        tvDescription.setText(currentRecord.getDescripcion());

        Bitmap bitmap = RecordController.base64ToBitmap(currentRecord.getFotografiaBase64());
        if (bitmap != null) {
            ivPhoto.setPadding(0, 0, 0, 0);
            ivPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ivPhoto.setImageBitmap(bitmap);
        } else {
            int paddingPx = (int) (32 * getResources().getDisplayMetrics().density);
            ivPhoto.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
            ivPhoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
            ivPhoto.setImageResource(R.drawable.ic_camera_placeholder);
        }

        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Registro")
                .setMessage("¿Estás seguro de que deseas eliminar este registro de la bitácora?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    boolean success = recordController.deleteRecord(currentRecord.getId());
                    if (success) {
                        Toast.makeText(this, "Registro eliminado", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Error al eliminar el registro", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
