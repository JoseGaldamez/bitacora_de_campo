package dev.josegaldamez.bitacora_de_campo.data.remote;

import java.util.List;

import dev.josegaldamez.bitacora_de_campo.models.Record;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface RegistroApi {

    @GET("api/registros")
    Call<List<Record>> getRecords();

    @GET("api/registros/{id}")
    Call<Record> getRecordById(@Path("id") int id);

    @POST("api/registros")
    Call<Record> createRecord(@Body Record record);

    @PUT("api/registros/{id}")
    Call<Record> updateRecord(@Path("id") int id, @Body Record record);

    @DELETE("api/registros/{id}")
    Call<Void> deleteRecord(@Path("id") int id);
}
