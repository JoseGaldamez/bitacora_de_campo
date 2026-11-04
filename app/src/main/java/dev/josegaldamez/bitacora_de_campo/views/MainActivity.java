package dev.josegaldamez.bitacora_de_campo.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import dev.josegaldamez.bitacora_de_campo.R;
import dev.josegaldamez.bitacora_de_campo.models.Record;
import dev.josegaldamez.bitacora_de_campo.viewmodel.RegistroViewModel;

public class MainActivity extends AppCompatActivity implements RecordAdapter.OnRecordClickListener {

    private RegistroViewModel viewModel;
    private RecordAdapter recordAdapter;

    private RecyclerView rvRecords;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private LinearLayout llEmptyState;
    private LinearLayout llErrorState;
    private TextView tvErrorMessage;
    private Button btnRetry;
    private FloatingActionButton fabAddRecord;

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

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(RegistroViewModel.class);

        // Bind views
        rvRecords = findViewById(R.id.rv_records);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        progressBar = findViewById(R.id.progress_bar);
        llEmptyState = findViewById(R.id.ll_empty_state);
        llErrorState = findViewById(R.id.ll_error_state);
        tvErrorMessage = findViewById(R.id.tv_error_message);
        btnRetry = findViewById(R.id.btn_retry);
        fabAddRecord = findViewById(R.id.fab_add_record);

        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        recordAdapter = new RecordAdapter(this);
        rvRecords.setAdapter(recordAdapter);

        setupObservers();

        swipeRefresh.setOnRefreshListener(() -> viewModel.fetchRecords());

        btnRetry.setOnClickListener(v -> {
            llErrorState.setVisibility(View.GONE);
            viewModel.fetchRecords();
        });

        fabAddRecord.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddRecordActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.fetchRecords();
    }

    private void setupObservers() {
        viewModel.getRecordsLiveData().observe(this, records -> {
            swipeRefresh.setRefreshing(false);
            llErrorState.setVisibility(View.GONE);

            if (records == null || records.isEmpty()) {
                llEmptyState.setVisibility(View.VISIBLE);
                rvRecords.setVisibility(View.GONE);
            } else {
                llEmptyState.setVisibility(View.GONE);
                rvRecords.setVisibility(View.VISIBLE);
                recordAdapter.setRecords(records);
            }
        });

        viewModel.getIsLoadingLiveData().observe(this, isLoading -> {
            if (!swipeRefresh.isRefreshing()) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
            fabAddRecord.setEnabled(!isLoading);
        });

        viewModel.getErrorMessageLiveData().observe(this, errorMsg -> {
            swipeRefresh.setRefreshing(false);
            progressBar.setVisibility(View.GONE);

            if (errorMsg != null && !errorMsg.isEmpty()) {
                // If we already have records shown, show a Toast; otherwise show error state view
                List<Record> currentRecords = viewModel.getRecordsLiveData().getValue();
                if (currentRecords == null || currentRecords.isEmpty()) {
                    llErrorState.setVisibility(View.VISIBLE);
                    rvRecords.setVisibility(View.GONE);
                    llEmptyState.setVisibility(View.GONE);
                    tvErrorMessage.setText(errorMsg);
                } else {
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
        });

        viewModel.getSuccessMessageLiveData().observe(this, successMsg -> {
            if (successMsg != null && !successMsg.isEmpty()) {
                Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show();
            }
        });
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
                    viewModel.deleteRecord(record.getId());
                    // Refresh after delete
                    viewModel.fetchRecords();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
