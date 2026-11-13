package dev.josegaldamez.bitacora_de_campo.models;
public class LoginRequest {
    @com.google.gson.annotations.SerializedName("email") private final String email;
    @com.google.gson.annotations.SerializedName("password") private final String password;
    public LoginRequest(String email, String password) { this.email = email; this.password = password; }
}