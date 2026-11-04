package dev.josegaldamez.bitacora_de_campo.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import dev.josegaldamez.bitacora_de_campo.data.repository.RegistroRepository;
import dev.josegaldamez.bitacora_de_campo.models.Record;

public class RegistroViewModel extends AndroidViewModel {

    private final RegistroRepository repository;

    private final MutableLiveData<List<Record>> recordsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Record> recordDetailLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> successMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccessLiveData = new MutableLiveData<>();

    public RegistroViewModel(@NonNull Application application) {
        super(application);
        repository = new RegistroRepository(application);
    }

    public LiveData<List<Record>> getRecordsLiveData() {
        return recordsLiveData;
    }

    public LiveData<Record> getRecordDetailLiveData() {
        return recordDetailLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessageLiveData;
    }

    public LiveData<String> getSuccessMessageLiveData() {
        return successMessageLiveData;
    }

    public LiveData<Boolean> getOperationSuccessLiveData() {
        return operationSuccessLiveData;
    }

    public void fetchRecords() {
        isLoadingLiveData.setValue(true);
        repository.getRecords(new RegistroRepository.RepositoryCallback<List<Record>>() {
            @Override
            public void onSuccess(List<Record> data) {
                isLoadingLiveData.setValue(false);
                recordsLiveData.setValue(data);

                if (data != null) {
                    for (Record item : data) {
                        boolean missingPhoto = item.getFotografiaBase64() == null || item.getFotografiaBase64().trim().isEmpty();
                        if (item.isTieneFoto() && missingPhoto) {
                            repository.getRecordById(item.getId(), new RegistroRepository.RepositoryCallback<Record>() {
                                @Override
                                public void onSuccess(Record detail) {
                                    if (detail != null && detail.getFotografiaBase64() != null) {
                                        item.setFotografiaBase64(detail.getFotografiaBase64());
                                        recordsLiveData.setValue(data);
                                    }
                                }

                                @Override
                                public void onError(String message) {
                                    // Ignorar error individual de carga de miniatura
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onError(String message) {
                isLoadingLiveData.setValue(false);
                errorMessageLiveData.setValue(message);
            }
        });
    }

    public void fetchRecordById(int id) {
        isLoadingLiveData.setValue(true);
        repository.getRecordById(id, new RegistroRepository.RepositoryCallback<Record>() {
            @Override
            public void onSuccess(Record data) {
                isLoadingLiveData.setValue(false);
                recordDetailLiveData.setValue(data);
            }

            @Override
            public void onError(String message) {
                isLoadingLiveData.setValue(false);
                errorMessageLiveData.setValue(message);
            }
        });
    }

    public void createRecord(Record record) {
        isLoadingLiveData.setValue(true);
        repository.createRecord(record, new RegistroRepository.RepositoryCallback<Record>() {
            @Override
            public void onSuccess(Record data) {
                isLoadingLiveData.setValue(false);
                successMessageLiveData.setValue("Registro creado exitosamente en el servidor.");
                operationSuccessLiveData.setValue(true);
            }

            @Override
            public void onError(String message) {
                isLoadingLiveData.setValue(false);
                errorMessageLiveData.setValue(message);
                operationSuccessLiveData.setValue(false);
            }
        });
    }

    public void updateRecord(int id, Record record) {
        isLoadingLiveData.setValue(true);
        repository.updateRecord(id, record, new RegistroRepository.RepositoryCallback<Record>() {
            @Override
            public void onSuccess(Record data) {
                isLoadingLiveData.setValue(false);
                successMessageLiveData.setValue("Registro actualizado exitosamente en el servidor.");
                operationSuccessLiveData.setValue(true);
            }

            @Override
            public void onError(String message) {
                isLoadingLiveData.setValue(false);
                errorMessageLiveData.setValue(message);
                operationSuccessLiveData.setValue(false);
            }
        });
    }

    public void deleteRecord(int id) {
        isLoadingLiveData.setValue(true);
        repository.deleteRecord(id, new RegistroRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                isLoadingLiveData.setValue(false);
                successMessageLiveData.setValue("Registro eliminado exitosamente.");
                operationSuccessLiveData.setValue(true);
            }

            @Override
            public void onError(String message) {
                isLoadingLiveData.setValue(false);
                errorMessageLiveData.setValue(message);
                operationSuccessLiveData.setValue(false);
            }
        });
    }
}
