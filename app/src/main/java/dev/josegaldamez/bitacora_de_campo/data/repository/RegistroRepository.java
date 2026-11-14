package dev.josegaldamez.bitacora_de_campo.data.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import dev.josegaldamez.bitacora_de_campo.R;
import dev.josegaldamez.bitacora_de_campo.data.remote.RegistroApi;
import dev.josegaldamez.bitacora_de_campo.data.remote.RetrofitClient;
import dev.josegaldamez.bitacora_de_campo.models.Record;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistroRepository {

    private static final String TAG = "LaboratorioHttp";
    private final RegistroApi api;
    private final Context context;

    public interface RepositoryCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public RegistroRepository(Context context) {
        this.context = context.getApplicationContext();
        RetrofitClient.initialize(this.context);
        this.api = RetrofitClient.getApi();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return true;
    }

    public Call<List<Record>> getRecords(RepositoryCallback<List<Record>> callback) {
        return getRecords(true, callback);
    }

    public Call<List<Record>> getRecords(boolean sendToken, RepositoryCallback<List<Record>> callback) {
        return enqueueComparison(api.getRecords(sendToken), sendToken, true, callback);
    }

    public Call<Void> probeDelete(int id, boolean sendToken, RepositoryCallback<Void> callback) {
        return enqueueComparison(api.deleteRecord(id, sendToken), sendToken, false, callback);
    }

    public Call<List<Record>> probeTimeout(RepositoryCallback<List<Record>> callback) {
        return enqueueComparison(api.probeTimeout(), true, true, callback);
    }

    private <T> Call<T> enqueueComparison(Call<T> request, boolean sendToken,
                                          boolean requiresBody, RepositoryCallback<T> callback) {
        String operation = "Retrofit " + request.request().method() + " " + request.request().url().encodedPath();
        if (!isNetworkAvailable()) {
            Log.d(TAG, operation + ": Sin conexión a internet (pre-chequeo)");
            callback.onError(context.getString(R.string.comparativa_offline));
            return null;
        }

        request.enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
                if (response.isSuccessful() && (!requiresBody || response.body() != null)) {
                    Log.d(TAG, operation + ": HTTP " + response.code() + " OK");
                    callback.onSuccess(response.body());
                } else {
                    int code = response.code();
                    Log.d(TAG, operation + ": HTTP " + code);
                    if (code == 401 && !sendToken) {
                        callback.onError(context.getString(R.string.comparativa_unauthenticated));
                    } else if (code == 403 && !requiresBody) {
                        callback.onError(context.getString(R.string.comparativa_forbidden));
                    } else if (code == 404 && !requiresBody) {
                        callback.onError(context.getString(R.string.comparativa_probe_not_found));
                    } else {
                        callback.onError(parseError(response));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
                if (!call.isCanceled()) {
                    Log.d(TAG, operation + ": " + t.getClass().getSimpleName() + " - " + t.getMessage());
                }
                callback.onError(handleFailure(t));
            }
        });
        return request;
    }

    public void getRecordById(int id, RepositoryCallback<Record> callback) {
        if (!isNetworkAvailable()) {
            callback.onError(context.getString(R.string.comparativa_offline));
            return;
        }

        api.getRecordById(id).enqueue(new Callback<Record>() {
            @Override
            public void onResponse(@NonNull Call<Record> call, @NonNull Response<Record> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Record> call, @NonNull Throwable t) {
                callback.onError(handleFailure(t));
            }
        });
    }

    public void createRecord(Record record, RepositoryCallback<Record> callback) {
        if (!isNetworkAvailable()) {
            callback.onError(context.getString(R.string.comparativa_offline));
            return;
        }

        api.createRecord(record).enqueue(new Callback<Record>() {
            @Override
            public void onResponse(@NonNull Call<Record> call, @NonNull Response<Record> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Record> call, @NonNull Throwable t) {
                callback.onError(handleFailure(t));
            }
        });
    }

    public void updateRecord(int id, Record record, RepositoryCallback<Record> callback) {
        if (!isNetworkAvailable()) {
            callback.onError(context.getString(R.string.comparativa_offline));
            return;
        }

        api.updateRecord(id, record).enqueue(new Callback<Record>() {
            @Override
            public void onResponse(@NonNull Call<Record> call, @NonNull Response<Record> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Record> call, @NonNull Throwable t) {
                callback.onError(handleFailure(t));
            }
        });
    }

    public void deleteRecord(int id, RepositoryCallback<Boolean> callback) {
        if (!isNetworkAvailable()) {
            callback.onError(context.getString(R.string.comparativa_offline));
            return;
        }

        api.deleteRecord(id, true).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(true);
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(handleFailure(t));
            }
        });
    }

    private String parseError(Response<?> response) {
        int code = response.code();
        if (code == 401) return context.getString(R.string.comparativa_session_expired);
        if (code == 403) return context.getString(R.string.comparativa_forbidden);
        if (code == 404) return "Recurso no encontrado (HTTP 404).";
        if (code >= 400 && code < 500) {
            return context.getString(R.string.comparativa_http_error, code);
        } else if (code >= 500) {
            return context.getString(R.string.comparativa_server_error, code);
        }
        return context.getString(R.string.comparativa_http_error, code);
    }

    private String handleFailure(Throwable t) {
        if (t instanceof SocketTimeoutException) {
            return context.getString(R.string.comparativa_timeout);
        } else if (t instanceof IOException) {
            return context.getString(R.string.comparativa_unreachable);
        }
        return context.getString(R.string.comparativa_invalid_response);
    }
}
