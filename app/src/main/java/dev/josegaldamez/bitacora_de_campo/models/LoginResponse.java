package dev.josegaldamez.bitacora_de_campo.models;
public class LoginResponse {
    @com.google.gson.annotations.SerializedName("token") public String token;
    @com.google.gson.annotations.SerializedName("expiration") public String expiration;
    @com.google.gson.annotations.SerializedName("role") public String role;
    @com.google.gson.annotations.SerializedName("email") public String email;
}