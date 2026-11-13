package dev.josegaldamez.bitacora_de_campo.views;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import dev.josegaldamez.bitacora_de_campo.R;
import dev.josegaldamez.bitacora_de_campo.models.User;
import dev.josegaldamez.bitacora_de_campo.services.DatabaseService;
import dev.josegaldamez.bitacora_de_campo.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_PREFILL_EMAIL = "extra_prefill_email";

    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private Button btnLogin;
    private Button btnQuickDemo;
    private Button btnGoToRegister;
    private ProgressBar progressBar;

    private DatabaseService dbService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbService = new DatabaseService(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupListeners();
        handleIntentData();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.til_login_email);
        tilPassword = findViewById(R.id.til_login_password);
        etEmail = findViewById(R.id.et_login_email);
        etPassword = findViewById(R.id.et_login_password);
        btnLogin = findViewById(R.id.btn_login);
        btnQuickDemo = findViewById(R.id.btn_quick_demo);
        btnGoToRegister = findViewById(R.id.btn_go_to_register);
        progressBar = findViewById(R.id.login_progress_bar);
    }

    private void setupListeners() {
        etEmail.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilEmail.setError(null);
            }
        });

        etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilPassword.setError(null);
            }
        });

        btnQuickDemo.setOnClickListener(v -> {
            etEmail.setText("admin@uth.hn");
            etPassword.setText("123456");
            tilEmail.setError(null);
            tilPassword.setError(null);
            Toast.makeText(this, "Credenciales de prueba cargadas", Toast.LENGTH_SHORT).show();
        });

        btnLogin.setOnClickListener(v -> attemptLogin());

        btnGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void handleIntentData() {
        if (getIntent() != null && getIntent().hasExtra(EXTRA_PREFILL_EMAIL)) {
            String registeredEmail = getIntent().getStringExtra(EXTRA_PREFILL_EMAIL);
            if (registeredEmail != null && !registeredEmail.isEmpty()) {
                etEmail.setText(registeredEmail);
                etPassword.requestFocus();
            }
        }
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        boolean cancel = false;
        View focusView = null;

        // Validar contraseña
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Ingrese su contraseña");
            focusView = etPassword;
            cancel = true;
        } else if (password.length() < 6) {
            tilPassword.setError("La contraseña debe tener al menos 6 caracteres");
            focusView = etPassword;
            cancel = true;
        }

        // Validar correo
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Ingrese su correo electrónico");
            focusView = etEmail;
            cancel = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Ingrese un formato de correo válido (ejemplo@correo.com)");
            focusView = etEmail;
            cancel = true;
        }

        if (cancel) {
            if (focusView != null) {
                focusView.requestFocus();
            }
            return;
        }

        // Mostrar progreso
        setLoadingState(true);

        // Validar en base de datos local SQLite
        boolean isValid = dbService.checkUserCredentials(email, password);

        setLoadingState(false);

        if (isValid) {
            User user = dbService.getUserByEmail(email);
            String displayName = (user != null && user.getNombre() != null) ? user.getNombre() : "Usuario";

            sessionManager.createLoginSession(email, displayName);

            Toast.makeText(this, "¡Bienvenido, " + displayName + "!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            tilPassword.setError("Correo o contraseña incorrectos");
            Toast.makeText(this, "Credenciales incorrectas. Verifique sus datos o regístrese.", Toast.LENGTH_LONG).show();
            etPassword.requestFocus();
        }
    }

    private void setLoadingState(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        btnQuickDemo.setEnabled(!isLoading);
        btnGoToRegister.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void afterTextChanged(Editable s) {}
    }
}
