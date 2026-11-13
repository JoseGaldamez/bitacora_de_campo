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
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import dev.josegaldamez.bitacora_de_campo.R;
import dev.josegaldamez.bitacora_de_campo.data.remote.RetrofitClient;
import dev.josegaldamez.bitacora_de_campo.models.LoginRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilName;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirmPassword;

    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;

    private Button btnRegister;
    private Button btnGoToLogin;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private Call<Void> request;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



        initViews();
        etPassword.setSaveEnabled(false);
        etConfirmPassword.setSaveEnabled(false);
        setupToolbar();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tilName = findViewById(R.id.til_register_name);
        tilEmail = findViewById(R.id.til_register_email);
        tilPassword = findViewById(R.id.til_register_password);
        tilConfirmPassword = findViewById(R.id.til_register_confirm_password);

        etName = findViewById(R.id.et_register_name);
        etEmail = findViewById(R.id.et_register_email);
        etPassword = findViewById(R.id.et_register_password);
        etConfirmPassword = findViewById(R.id.et_register_confirm_password);

        btnRegister = findViewById(R.id.btn_register);
        btnGoToLogin = findViewById(R.id.btn_go_to_login);
        progressBar = findViewById(R.id.register_progress_bar);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupListeners() {
        etName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilName.setError(null);
            }
        });

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

        etConfirmPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilConfirmPassword.setError(null);
            }
        });

        btnRegister.setOnClickListener(v -> attemptRegister());

        btnGoToLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        boolean cancel = false;
        View focusView = null;

        // Validar confirmación de contraseña
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Confirme su contraseña");
            focusView = etConfirmPassword;
            cancel = true;
        } else if (!confirmPassword.equals(password)) {
            tilConfirmPassword.setError("Las contraseñas no coinciden");
            focusView = etConfirmPassword;
            cancel = true;
        }

        // Validar contraseña
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Ingrese una contraseña");
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
            tilEmail.setError("Ingrese un correo electrónico válido");
            focusView = etEmail;
            cancel = true;
        }

        // Validar nombre
        if (TextUtils.isEmpty(name)) {
            tilName.setError("Ingrese su nombre completo");
            focusView = etName;
            cancel = true;
        } else if (name.length() < 3) {
            tilName.setError("El nombre debe tener al menos 3 caracteres");
            focusView = etName;
            cancel = true;
        }

        if (cancel) {
            if (focusView != null) {
                focusView.requestFocus();
            }
            return;
        }

        setLoadingState(true);
        request = RetrofitClient.getAuthApi().register(new LoginRequest(email, password));
        request.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                setLoadingState(false);
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Cuenta creada. Inicia sesión.", Toast.LENGTH_SHORT).show();
                    Intent login = new Intent(RegisterActivity.this, LoginActivity.class)
                            .putExtra(LoginActivity.EXTRA_PREFILL_EMAIL, email)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(login);
                    finish();
                    return;
                }

                String message = response.code() == 400
                        ? "No se pudo registrar el correo. Verifica si ya tienes una cuenta."
                        : "No se pudo completar el registro (HTTP " + response.code() + ").";
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable error) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                setLoadingState(false);
                Toast.makeText(RegisterActivity.this,
                        "No se pudo confirmar el registro. Comprueba tu conexión e intenta iniciar sesión.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (request != null) request.cancel();
        super.onDestroy();
    }

    private void setLoadingState(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
        btnGoToLogin.setEnabled(!isLoading);
        etName.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        etConfirmPassword.setEnabled(!isLoading);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void afterTextChanged(Editable s) {}
    }
}
