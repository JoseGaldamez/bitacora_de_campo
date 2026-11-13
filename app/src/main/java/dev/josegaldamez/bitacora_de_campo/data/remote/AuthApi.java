package dev.josegaldamez.bitacora_de_campo.data.remote;
import dev.josegaldamez.bitacora_de_campo.models.*;
import retrofit2.Call;
import retrofit2.http.*;
public interface AuthApi {
    @POST("api/auth/login") Call<LoginResponse> login(@Body LoginRequest request);
}