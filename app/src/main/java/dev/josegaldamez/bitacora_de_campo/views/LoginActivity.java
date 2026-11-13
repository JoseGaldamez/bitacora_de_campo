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

    private retrofit2.Call<dev.josegaldamez.bitacora_de_campo.models.LoginResponse> request;
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

        dev.josegaldamez.bitacora_de_campo.data.remote.RetrofitClient.initialize(this);
        sessionManager = new SessionManager(this);

        initViews();
        etPassword.setSaveEnabled(false);
        if (getIntent().getBooleanExtra("expired", false)) Toast.makeText(this, "Tu sesión ha expirado. Inicia sesión nuevamente.", Toast.LENGTH_LONG).show();
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

        btnQuickDemo.setOnClickListener(v -> Toast.makeText(this, "No hay una cuenta demo del servidor verificada. Usa tu cuenta real.", Toast.LENGTH_LONG).show());

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
        if (request != null) return;
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        boolean cancel = false;
        View focusView = null;

        // Validar contraseña
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Ingrese su contraseña");
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

        setLoadingState(true);
        request = dev.josegaldamez.bitacora_de_campo.data.remote.RetrofitClient.getAuthApi()
                .login(new dev.josegaldamez.bitacora_de_campo.models.LoginRequest(email, password));
        request.enqueue(new retrofit2.Callback<dev.josegaldamez.bitacora_de_campo.models.LoginResponse>() {
            @Override public void onResponse(retrofit2.Call<dev.josegaldamez.bitacora_de_campo.models.LoginResponse> call,
                    retrofit2.Response<dev.josegaldamez.bitacora_de_campo.models.LoginResponse> response) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                request = null;
                setLoadingState(false);
                if (response.isSuccessful() && sessionManager.save(response.body())) {
                    etPassword.setText("");
                    openMain();
                } else {
                    String message = response.isSuccessful() ? "El servidor devolvió una sesión inválida."
                            : response.code() == 400 || response.code() == 401 ? "Correo o contraseña incorrectos."
                            : response.code() == 403 ? "No tienes permisos para acceder."
                            : response.code() == 404 ? "El servidor no tiene disponible el inicio de sesión."
                            : response.code() == 429 ? "Demasiados intentos. Espera antes de volver a intentar."
                            : "Ocurrió un problema al iniciar sesión. Inténtalo nuevamente.";
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                    if (response.errorBody() != null) response.errorBody().close();
                }
            }
            @Override public void onFailure(retrofit2.Call<dev.josegaldamez.bitacora_de_campo.models.LoginResponse> call, Throwable error) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                request = null;
                setLoadingState(false);
                Toast.makeText(LoginActivity.this, error instanceof java.net.SocketTimeoutException
                        ? "El servidor tardó demasiado. Inténtalo nuevamente."
                        : error instanceof java.io.IOException ? "No se pudo conectar con el servidor. Revisa tu conexión."
                        : "La respuesta del servidor no es válida.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override protected void onResume() {
        super.onResume();
        if (sessionManager != null && sessionManager.isLoggedIn()) openMain();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentData();
    }

    @Override protected void onDestroy() {
        if (request != null) request.cancel();
        super.onDestroy();
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
