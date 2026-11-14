package dev.josegaldamez.bitacora_de_campo.views;

import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.ParseError;
import com.android.volley.RequestQueue;
import com.android.volley.TimeoutError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dev.josegaldamez.bitacora_de_campo.R;
import dev.josegaldamez.bitacora_de_campo.data.remote.RetrofitClient;
import dev.josegaldamez.bitacora_de_campo.data.repository.RegistroRepository;
import dev.josegaldamez.bitacora_de_campo.models.Record;
import dev.josegaldamez.bitacora_de_campo.utils.SessionManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Pantalla de Laboratorio de Red - Tarea 2.5 (Linda Nolasco)
 * Compara y evalúa el comportamiento de Volley, OkHttp y Retrofit
 * ante el mismo endpoint GET /api/registros y diversos escenarios de falla.
 */
public class LaboratorioActivity extends ProtectedActivity {

    private static final String TAG = "LaboratorioHttp";
    private static final int DELETE_PROBE_ID = 0;
    private static final Type RECORD_LIST = new TypeToken<List<Record>>() {}.getType();

    private final Gson gson = new Gson();
    private final OkHttpClient okHttp = new OkHttpClient.Builder()
            .connectTimeout(RetrofitClient.TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(RetrofitClient.TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build();

    private SessionManager session;
    private RequestQueue volleyQueue;
    private RegistroRepository repository;

    private Call okHttpCall;
    private retrofit2.Call<?> retrofitCall;

    private MaterialSwitch switchSendToken;
    private MaterialSwitch switchDeleteProbe;
    private TextView tvEndpoint;
    private MaterialButton btnTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!requireSession()) return;

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_laboratorio);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.laboratorio_root), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.laboratorio_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        session = new SessionManager(this);
        volleyQueue = Volley.newRequestQueue(getApplicationContext());
        repository = new RegistroRepository(this);

        switchSendToken = findViewById(R.id.switch_send_token);
        switchDeleteProbe = findViewById(R.id.switch_delete_probe);
        tvEndpoint = findViewById(R.id.tv_endpoint);

        switchDeleteProbe.setOnCheckedChangeListener((view, checked) -> {
            if (checked) {
                tvEndpoint.setText(getString(R.string.comparativa_delete_endpoint, DELETE_PROBE_ID));
            } else {
                tvEndpoint.setText(getString(R.string.comparativa_endpoint));
            }
        });

        ResultCard cardVolley = new ResultCard(R.id.btn_run_volley, R.id.card_result_volley);
        ResultCard cardOkHttp = new ResultCard(R.id.btn_run_okhttp, R.id.card_result_okhttp);
        ResultCard cardRetrofit = new ResultCard(R.id.btn_run_retrofit, R.id.card_result_retrofit);
        btnTimeout = findViewById(R.id.btn_run_timeout);

        cardVolley.button.setOnClickListener(v -> queryVolley(cardVolley, false));
        cardOkHttp.button.setOnClickListener(v -> queryOkHttp(cardOkHttp, false));
        cardRetrofit.button.setOnClickListener(v -> queryRetrofit(cardRetrofit, false));

        btnTimeout.setOnClickListener(v -> {
            if (!requireSession()) return;
            queryVolley(cardVolley, true);
            queryOkHttp(cardOkHttp, true);
            queryRetrofit(cardRetrofit, true);
        });
    }

    private void queryVolley(ResultCard result, boolean timeoutProbe) {
        if (!requireSession()) return;
        boolean sendToken = !timeoutProbe && switchSendToken.isChecked();
        boolean delete = !timeoutProbe && switchDeleteProbe.isChecked();
        String token = (timeoutProbe || sendToken) ? session.getToken() : null;

        long started = result.start();
        String url = requestUrl(delete, timeoutProbe);
        Map<String, String> headers = token == null ? Collections.emptyMap()
                : Collections.singletonMap("Authorization", "Bearer " + token);

        ErrorListener onError = error -> {
            int status = error.networkResponse == null ? 0 : error.networkResponse.statusCode;
            String operation = timeoutProbe ? getString(R.string.comparativa_timeout_endpoint)
                    : (delete ? "DELETE" : "GET");
            Log.d(TAG, "Volley " + operation + ": " + error.getClass().getSimpleName()
                    + (status == 0 ? "" : " (HTTP " + status + ")"));

            if (error instanceof TimeoutError) {
                result.error(getString(R.string.comparativa_timeout));
            } else if (error instanceof ParseError) {
                result.error(getString(R.string.comparativa_invalid_response));
            } else if (status != 0) {
                showHttpError(result, status, token, delete);
            } else {
                result.error(networkErrorMessage());
            }
        };

        com.android.volley.Request<?> request;
        if (delete) {
            request = new StringRequest(com.android.volley.Request.Method.DELETE, url,
                    responseBody -> {
                        long elapsed = SystemClock.elapsedRealtime() - started;
                        Log.d(TAG, "Volley DELETE: HTTP 200 OK en " + elapsed + "ms");
                        result.deleteSuccess(elapsed);
                    }, onError) {
                @Override
                public Map<String, String> getHeaders() {
                    return headers;
                }
            };
        } else {
            request = new JsonArrayRequest(url,
                    records -> {
                        long elapsed = SystemClock.elapsedRealtime() - started;
                        Log.d(TAG, "Volley GET: " + records.length() + " registros en " + elapsed + "ms");
                        result.listResponse(records.length(), started, timeoutProbe);
                    }, onError) {
                @Override
                public Map<String, String> getHeaders() {
                    return headers;
                }
            };
        }

        request.setTag(this);
        request.setShouldCache(false);
        request.setRetryPolicy(new DefaultRetryPolicy(RetrofitClient.TIMEOUT_MS, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        volleyQueue.add(request);
    }

    private void queryOkHttp(ResultCard result, boolean timeoutProbe) {
        if (!requireSession()) return;
        boolean sendToken = !timeoutProbe && switchSendToken.isChecked();
        boolean delete = !timeoutProbe && switchDeleteProbe.isChecked();
        String token = (timeoutProbe || sendToken) ? session.getToken() : null;

        long started = result.start();
        Request.Builder builder = new Request.Builder().url(requestUrl(delete, timeoutProbe));
        if (delete) builder.delete();
        if (token != null) builder.header("Authorization", "Bearer " + token);
        Request request = builder.build();

        okHttpCall = okHttp.newCall(request);
        okHttpCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException error) {
                if (call.isCanceled()) return;
                Log.d(TAG, "OkHttp " + request.method() + " " + request.url().encodedPath()
                        + ": " + error.getClass().getSimpleName() + " - " + error.getMessage());
                String message = error instanceof SocketTimeoutException
                        ? getString(R.string.comparativa_timeout) : networkErrorMessage();
                runOnUiThread(() -> result.error(message));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (Response received = response) {
                    if (call.isCanceled()) return;
                    if (!received.isSuccessful()) {
                        int status = received.code();
                        Log.d(TAG, "OkHttp " + request.method() + ": HTTP " + status);
                        runOnUiThread(() -> showHttpError(result, status, token, delete));
                        return;
                    }

                    if (delete) {
                        long elapsed = SystemClock.elapsedRealtime() - started;
                        Log.d(TAG, "OkHttp DELETE: HTTP 200 OK en " + elapsed + "ms");
                        runOnUiThread(() -> result.deleteSuccess(elapsed));
                        return;
                    }

                    ResponseBody body = received.body();
                    List<Record> records = body == null ? null : gson.fromJson(body.string(), RECORD_LIST);
                    if (records == null) {
                        throw new JsonParseException("Se esperaba un arreglo JSON no nulo");
                    }
                    long elapsed = SystemClock.elapsedRealtime() - started;
                    Log.d(TAG, "OkHttp GET: " + records.size() + " registros en " + elapsed + "ms");
                    runOnUiThread(() -> result.listResult(records.size(), elapsed, timeoutProbe));
                } catch (IOException error) {
                    onFailure(call, error);
                } catch (JsonParseException error) {
                    Log.d(TAG, "OkHttp JsonParseException: " + error.getMessage());
                    runOnUiThread(() -> result.error(getString(R.string.comparativa_invalid_response)));
                }
            }
        });
    }

    private void queryRetrofit(ResultCard result, boolean timeoutProbe) {
        if (!requireSession()) return;
        boolean sendToken = !timeoutProbe && switchSendToken.isChecked();
        long started = result.start();

        if (!timeoutProbe && switchDeleteProbe.isChecked()) {
            retrofitCall = repository.probeDelete(DELETE_PROBE_ID, sendToken,
                    new RegistroRepository.RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            long elapsed = SystemClock.elapsedRealtime() - started;
                            result.deleteSuccess(elapsed);
                        }

                        @Override
                        public void onError(String message) {
                            result.error(message);
                        }
                    });
            return;
        }

        RegistroRepository.RepositoryCallback<List<Record>> callback = new RegistroRepository.RepositoryCallback<List<Record>>() {
            @Override
            public void onSuccess(List<Record> records) {
                result.listResponse(records.size(), started, timeoutProbe);
            }

            @Override
            public void onError(String message) {
                result.error(message);
            }
        };

        retrofitCall = timeoutProbe ? repository.probeTimeout(callback)
                : repository.getRecords(sendToken, callback);
    }

    private String requestUrl(boolean delete, boolean timeoutProbe) {
        if (timeoutProbe) return RetrofitClient.getTimeoutProbeUrl();
        String recordsUrl = RetrofitClient.getRecordsUrl();
        return delete ? recordsUrl + "/" + DELETE_PROBE_ID : recordsUrl;
    }

    private void updateTimeoutButton() {
        btnTimeout.setEnabled(findViewById(R.id.btn_run_volley).isEnabled()
                && findViewById(R.id.btn_run_okhttp).isEnabled()
                && findViewById(R.id.btn_run_retrofit).isEnabled());
    }

    private void showHttpError(ResultCard result, int status, String token, boolean delete) {
        if (isFinishing() || isDestroyed()) return;
        if (status == 401) {
            if (token == null) {
                result.error(getString(R.string.comparativa_unauthenticated));
                return;
            }
            result.error(getString(R.string.comparativa_session_expired));
            session.invalidate(token);
        } else if (status == 403 && delete) {
            result.error(getString(R.string.comparativa_forbidden));
        } else if (status == 404 && delete) {
            result.error(getString(R.string.comparativa_probe_not_found));
        } else if (status >= 500) {
            result.error(getString(R.string.comparativa_server_error, status));
        } else {
            result.error(getString(R.string.comparativa_http_error, status));
        }
    }

    private String networkErrorMessage() {
        ConnectivityManager connectivity = getSystemService(ConnectivityManager.class);
        if (connectivity != null && connectivity.getActiveNetwork() == null) {
            return getString(R.string.comparativa_offline);
        }
        return getString(R.string.comparativa_unreachable);
    }

    @Override
    protected void onDestroy() {
        if (volleyQueue != null) {
            volleyQueue.cancelAll(this);
            volleyQueue.stop();
        }
        if (okHttpCall != null) okHttpCall.cancel();
        if (retrofitCall != null) retrofitCall.cancel();
        super.onDestroy();
    }

    private class ResultCard {
        private final MaterialButton button;
        private final MaterialCardView card;
        private final TextView title;
        private final TextView detail;
        private final TextView metric;

        ResultCard(int buttonId, int cardId) {
            button = findViewById(buttonId);
            card = findViewById(cardId);
            title = card.findViewById(R.id.card_client_name);
            title.setText(button.getText());
            detail = card.findViewById(R.id.card_client_detail);
            metric = card.findViewById(R.id.card_client_metric);
        }

        long start() {
            render(getString(R.string.comparativa_loading), "", false);
            button.setEnabled(false);
            updateTimeoutButton();
            return SystemClock.elapsedRealtime();
        }

        void listResponse(int count, long started, boolean timeoutProbe) {
            listResult(count, SystemClock.elapsedRealtime() - started, timeoutProbe);
        }

        void listResult(int count, long elapsed, boolean timeoutProbe) {
            String message = timeoutProbe ? getString(R.string.comparativa_timeout_responded)
                    : getResources().getQuantityString(R.plurals.comparativa_records, count, count);
            render(message, getString(R.string.comparativa_elapsed, elapsed), false);
        }

        void error(String message) {
            render(message, getString(R.string.comparativa_error), true);
        }

        void deleteSuccess(long elapsed) {
            render(getString(R.string.comparativa_delete_success),
                    getString(R.string.comparativa_elapsed, elapsed), false);
        }

        private void render(String message, String value, boolean error) {
            if (isFinishing() || isDestroyed()) return;
            detail.setText(message);
            metric.setText(value);
            metric.setVisibility(value.isEmpty() ? TextView.GONE : TextView.VISIBLE);

            if (error) {
                card.setCardBackgroundColor(ColorStateList.valueOf(MaterialColors.getColor(card,
                        com.google.android.material.R.attr.colorErrorContainer)));
                card.setStrokeColor(ColorStateList.valueOf(MaterialColors.getColor(card,
                        androidx.appcompat.R.attr.colorError)));
                title.setTextColor(MaterialColors.getColor(card,
                        com.google.android.material.R.attr.colorOnErrorContainer));
                detail.setTextColor(MaterialColors.getColor(card,
                        com.google.android.material.R.attr.colorOnErrorContainer));
                metric.setTextColor(MaterialColors.getColor(card,
                        com.google.android.material.R.attr.colorOnErrorContainer));
            } else {
                card.setCardBackgroundColor(ColorStateList.valueOf(MaterialColors.getColor(card,
                        com.google.android.material.R.attr.colorSurface)));
                card.setStrokeColor(ColorStateList.valueOf(MaterialColors.getColor(card,
                        com.google.android.material.R.attr.colorOutlineVariant)));
                title.setTextColor(MaterialColors.getColor(card,
                        com.google.android.material.R.attr.colorOnSurface));
                detail.setTextColor(MaterialColors.getColor(card,
                        com.google.android.material.R.attr.colorOnSurfaceVariant));
                metric.setTextColor(MaterialColors.getColor(card,
                        androidx.appcompat.R.attr.colorPrimary));
            }

            button.setEnabled(true);
            updateTimeoutButton();
        }
    }
}
