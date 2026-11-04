package dev.josegaldamez.bitacora_de_campo.data.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import dev.josegaldamez.bitacora_de_campo.data.remote.RegistroApi;
import dev.josegaldamez.bitacora_de_campo.data.remote.RetrofitClient;
import dev.josegaldamez.bitacora_de_campo.models.Record;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistroRepository {

    private final RegistroApi api;
    private final Context context;

    public interface RepositoryCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public RegistroRepository(Context context) {
        this.context = context.getApplicationContext();
        this.api = RetrofitClient.getApi();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return true; // fallback if cm is null
    }

    public void getRecords(RepositoryCallback<List<Record>> callback) {
        if (!isNetworkAvailable()) {
            callback.onError("Sin conexión a internet. Verifique su red y reintente.");
            return;
        }

        api.getRecords().enqueue(new Callback<List<Record>>() {
            @Override
            public void onResponse(@NonNull Call<List<Record>> call, @NonNull Response<List<Record>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Record>> call, @NonNull Throwable t) {
                callback.onError(handleFailure(t));
            }
        });
    }

    public void getRecordById(int id, RepositoryCallback<Record> callback) {
        if (!isNetworkAvailable()) {
            callback.onError("Sin conexión a internet. Verifique su red y reintente.");
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
            callback.onError("Sin conexión a internet. Verifique su red y reintente.");
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
            callback.onError("Sin conexión a internet. Verifique su red y reintente.");
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
            callback.onError("Sin conexión a internet. Verifique su red y reintente.");
            return;
        }

        api.deleteRecord(id).enqueue(new Callback<Void>() {
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
        try {
            if (response.errorBody() != null) {
                String errorBodyStr = response.errorBody().string();
                if (!errorBodyStr.isEmpty()) {
                    return "Error " + code + ": " + errorBodyStr;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (code >= 400 && code < 500) {
            return "Error del cliente (" + code + "): Solicitud inválida o datos incorrectos.";
        } else if (code >= 500) {
            return "Error del servidor (" + code + "): Intente más tarde.";
        }
        return "Error desconocido en la respuesta (" + code + ")";
    }

    private String handleFailure(Throwable t) {
        if (t instanceof SocketTimeoutException) {
            return "Tiempo de espera agotado (Timeout). Verifique su conexión.";
        } else if (t instanceof IOException) {
            return "Sin conexión o error de red. Verifique su conexión a internet.";
        }
        return "Error de conexión: " + t.getLocalizedMessage();
    }
}
