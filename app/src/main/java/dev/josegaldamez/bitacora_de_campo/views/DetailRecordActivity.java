package dev.josegaldamez.bitacora_de_campo.views;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;

import dev.josegaldamez.bitacora_de_campo.R;
import dev.josegaldamez.bitacora_de_campo.controllers.RecordController;
import dev.josegaldamez.bitacora_de_campo.models.Record;
import dev.josegaldamez.bitacora_de_campo.viewmodel.RegistroViewModel;

public class DetailRecordActivity extends AppCompatActivity {

    public static final String EXTRA_RECORD_ID = "extra_record_id";

    private RegistroViewModel viewModel;
    private Record currentRecord;

    private ImageView ivPhoto;
    private TextView tvTitle;
    private TextView tvDate;
    private TextView tvDescription;
    private Button btnDelete;
    private ProgressBar progressBar;

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

        viewModel = new ViewModelProvider(this).get(RegistroViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivPhoto = findViewById(R.id.iv_detail_photo);
        tvTitle = findViewById(R.id.tv_detail_title);
        tvDate = findViewById(R.id.tv_detail_date);
        tvDescription = findViewById(R.id.tv_detail_description);
        btnDelete = findViewById(R.id.btn_delete_detail);

        // Add ProgressBar if not in layout, or use existing views
        progressBar = findViewById(R.id.progress_bar);

        int recordId = getIntent().getIntExtra(EXTRA_RECORD_ID, -1);
        if (recordId != -1) {
            setupObservers();
            viewModel.fetchRecordById(recordId);
        } else {
            Toast.makeText(this, "ID de registro inválido", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void setupObservers() {
        viewModel.getRecordDetailLiveData().observe(this, record -> {
            if (record != null) {
                currentRecord = record;
                tvTitle.setText(record.getTitulo());
                try {
                    java.time.OffsetDateTime fecha = java.time.OffsetDateTime.parse(record.getFecha());
                    java.time.format.DateTimeFormatter formato = java.time.format.DateTimeFormatter.ofPattern(
                            "d 'de' MMMM 'de' yyyy, h:mm a",
                            new java.util.Locale("es", "HN")
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
                    int paddingPx = (int) (32 * getResources().getDisplayMetrics().density);
                    ivPhoto.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
                    ivPhoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    ivPhoto.setImageResource(R.drawable.ic_camera_placeholder);
                }
            }
        });

        viewModel.getIsLoadingLiveData().observe(this, isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
            btnDelete.setEnabled(!isLoading);
        });

        viewModel.getErrorMessageLiveData().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getOperationSuccessLiveData().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Registro eliminado exitosamente", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showDeleteConfirmationDialog() {
        if (currentRecord == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Registro")
                .setMessage("¿Estás seguro de que deseas eliminar este registro de la bitácora?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    viewModel.deleteRecord(currentRecord.getId());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
