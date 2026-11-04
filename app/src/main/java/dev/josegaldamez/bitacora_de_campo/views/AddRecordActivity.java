package dev.josegaldamez.bitacora_de_campo.views;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.Locale;

import dev.josegaldamez.bitacora_de_campo.R;
import dev.josegaldamez.bitacora_de_campo.controllers.RecordController;

public class AddRecordActivity extends AppCompatActivity {

    private EditText etTitle;
    private EditText etDescription;
    private ImageView ivPhotoPreview;
    private Bitmap capturedBitmap;

    private RecordController recordController;

    private final ActivityResultLauncher<Void> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    capturedBitmap = bitmap;
                    ivPhotoPreview.setPadding(0, 0, 0, 0);
                    ivPhotoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivPhotoPreview.setImageBitmap(bitmap);
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

        recordController = new RecordController(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etTitle = findViewById(R.id.et_title);
        etDescription = findViewById(R.id.et_description);
        ivPhotoPreview = findViewById(R.id.iv_photo_preview);

        Button btnTakePhoto = findViewById(R.id.btn_take_photo);
        Button btnVoiceToText = findViewById(R.id.btn_voice_to_text);
        Button btnSaveRecord = findViewById(R.id.btn_save_record);

        btnTakePhoto.setOnClickListener(v -> openCamera());

        btnVoiceToText.setOnClickListener(v -> startSpeechToText());

        btnSaveRecord.setOnClickListener(v -> saveRecord());
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

        if (title.isEmpty()) {
            etTitle.setError("Ingrese un título");
            etTitle.requestFocus();
            return;
        }

        if (capturedBitmap == null) {
            Toast.makeText(this, "Por favor tome una fotografía antes de guardar.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Ingrese o dicte una descripción");
            etDescription.requestFocus();
            return;
        }

        boolean success = recordController.addRecord(title, description, capturedBitmap);
        if (success) {
            Toast.makeText(this, "Registro guardado exitosamente en SQLite", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error al guardar el registro", Toast.LENGTH_SHORT).show();
        }
    }
}
