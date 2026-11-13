package dev.josegaldamez.bitacora_de_campo.data.remote;
import dev.josegaldamez.bitacora_de_campo.models.*;
import retrofit2.Call;
import retrofit2.http.*;
public interface AuthApi {
    @POST("api/auth/login") Call<LoginResponse> login(@Body LoginRequest request);
    @POST("api/auth/registro") Call<Void> register(@Body LoginRequest request);
}
