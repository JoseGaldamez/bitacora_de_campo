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
import retrofit2.http.Tag;

public interface RegistroApi {

    String LIST_PATH = "api/registros";

    @GET(LIST_PATH)
    Call<List<Record>> getRecords(@Tag Boolean sendToken);

    @GET("api/registros/{id}")
    Call<Record> getRecordById(@Path("id") int id);

    @POST("api/registros")
    Call<Record> createRecord(@Body Record record);

    @PUT("api/registros/{id}")
    Call<Record> updateRecord(@Path("id") int id, @Body Record record);

    @DELETE(LIST_PATH + "/{id}")
    Call<Void> deleteRecord(@Path("id") int id, @Tag Boolean sendToken);
}
