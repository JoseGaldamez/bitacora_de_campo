package dev.josegaldamez.bitacora_de_campo.views;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import dev.josegaldamez.bitacora_de_campo.R;
import dev.josegaldamez.bitacora_de_campo.models.Record;
import dev.josegaldamez.bitacora_de_campo.viewmodel.RegistroViewModel;

public class AddRecordActivity extends AppCompatActivity {

    public static final String EXTRA_RECORD_ID = "extra_record_id";

    private EditText etTitle;
    private EditText etDescription;
    private ImageView ivPhotoPreview;
    private ProgressBar progressBar;
    private Button btnSaveRecord;
    private Bitmap capturedBitmap;
    private String existingPhotoBase64 = "";

    private RegistroViewModel viewModel;
    private int editRecordId = -1;
    private boolean isEditMode = false;

    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    capturedBitmap = resizeBitmap(bitmap, 1024);
                    ivPhotoPreview.setPadding(0, 0, 0, 0);
                    ivPhotoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivPhotoPreview.setImageBitmap(capturedBitmap);
                }
            });

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    cameraLauncher.launch(null);
                } else {
                    Toast.makeText(this, "Permiso de cámara requerido para tomar fotografías.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> speechToTextLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0);
                        String currentText = etDescription.getText().toString().trim();
                        if (currentText.isEmpty()) {
                            etDescription.setText(spokenText);
                        } else {
                            etDescription.setText(String.format("%s %s", currentText, spokenText));
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_record);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(RegistroViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etTitle = findViewById(R.id.et_title);
        etDescription = findViewById(R.id.et_description);
        ivPhotoPreview = findViewById(R.id.iv_photo_preview);
        progressBar = findViewById(R.id.progress_bar);

        Button btnTakePhoto = findViewById(R.id.btn_take_photo);
        Button btnVoiceToText = findViewById(R.id.btn_voice_to_text);
        btnSaveRecord = findViewById(R.id.btn_save_record);

        // Check if editing
        if (getIntent().hasExtra(EXTRA_RECORD_ID)) {
            editRecordId = getIntent().getIntExtra(EXTRA_RECORD_ID, -1);
            if (editRecordId != -1) {
                isEditMode = true;
                toolbar.setTitle("Editar Registro");
                btnSaveRecord.setText("Actualizar Registro");
                viewModel.fetchRecordById(editRecordId);
            }
        }

        btnTakePhoto.setOnClickListener(v -> openCamera());
        btnVoiceToText.setOnClickListener(v -> startSpeechToText());
        btnSaveRecord.setOnClickListener(v -> saveRecord());

        setupObservers();
    }

    private void setupObservers() {
        if (isEditMode) {
            viewModel.getRecordDetailLiveData().observe(this, record -> {
                if (record != null) {
                    etTitle.setText(record.getTitulo());
                    etDescription.setText(record.getDescripcion());
                    existingPhotoBase64 = record.getFotografiaBase64() != null ? record.getFotografiaBase64() : "";

                    Bitmap bitmap = base64ToBitmap(existingPhotoBase64);
                    if (bitmap != null) {
                        capturedBitmap = bitmap;
                        ivPhotoPreview.setPadding(0, 0, 0, 0);
                        ivPhotoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        ivPhotoPreview.setImageBitmap(bitmap);
                    }
                }
            });
        }

        viewModel.getIsLoadingLiveData().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnSaveRecord.setEnabled(!isLoading);
        });

        viewModel.getErrorMessageLiveData().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getOperationSuccessLiveData().observe(this, success -> {
            if (success != null && success) {
                finish();
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null);
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hable para dictar la descripción...");

        try {
            speechToTextLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "La función de Voz a Texto no está disponible en este dispositivo.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveRecord() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Validations matching API specifications
        if (title.isEmpty()) {
            etTitle.setError("El título es requerido");
            etTitle.requestFocus();
            return;
        }
        if (title.length() < 3 || title.length() > 100) {
            etTitle.setError("El título debe tener entre 3 y 100 caracteres");
            etTitle.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("La descripción es requerida");
            etDescription.requestFocus();
            return;
        }
        if (description.length() > 500) {
            etDescription.setError("La descripción no puede exceder los 500 caracteres");
            etDescription.requestFocus();
            return;
        }

        if (capturedBitmap == null && existingPhotoBase64.isEmpty()) {
            Toast.makeText(this, "Por favor tome una fotografía antes de guardar.", Toast.LENGTH_SHORT).show();
            return;
        }

        String base64Photo = capturedBitmap != null ? bitmapToBase64(capturedBitmap) : existingPhotoBase64;
        String fechaActual = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

        Record record = new Record(title, description, base64Photo, fechaActual);

        if (isEditMode) {
            record.setId(editRecordId);
            viewModel.updateRecord(editRecordId, record);
        } else {
            viewModel.createRecord(record);
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String base64Str) {
        if (base64Str == null || base64Str.trim().isEmpty()) return null;
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
