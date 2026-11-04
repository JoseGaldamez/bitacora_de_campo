package dev.josegaldamez.bitacora_de_campo.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import dev.josegaldamez.bitacora_de_campo.R;
import dev.josegaldamez.bitacora_de_campo.controllers.RecordController;
import dev.josegaldamez.bitacora_de_campo.models.Record;

public class MainActivity extends AppCompatActivity implements RecordAdapter.OnRecordClickListener {

    private RecordController recordController;
    private RecordAdapter recordAdapter;

    private RecyclerView rvRecords;
    private LinearLayout llEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recordController = new RecordController(this);

        rvRecords = findViewById(R.id.rv_records);
        llEmptyState = findViewById(R.id.ll_empty_state);
        FloatingActionButton fabAddRecord = findViewById(R.id.fab_add_record);

        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        recordAdapter = new RecordAdapter(this);
        rvRecords.setAdapter(recordAdapter);

        fabAddRecord.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddRecordActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecords();
    }

    private void loadRecords() {
        List<Record> records = recordController.getAllRecords();
        recordAdapter.setRecords(records);

        if (records.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            rvRecords.setVisibility(View.GONE);
        } else {
            llEmptyState.setVisibility(View.GONE);
            rvRecords.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRecordClick(Record record) {
        Intent intent = new Intent(MainActivity.this, DetailRecordActivity.class);
        intent.putExtra(DetailRecordActivity.EXTRA_RECORD_ID, record.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Record record) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Registro")
                .setMessage("¿Deseas eliminar el registro '" + record.getTitulo() + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    boolean success = recordController.deleteRecord(record.getId());
                    if (success) {
                        Toast.makeText(this, "Registro eliminado", Toast.LENGTH_SHORT).show();
                        loadRecords();
                    } else {
                        Toast.makeText(this, "Error al eliminar el registro", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
