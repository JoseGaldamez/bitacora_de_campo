package dev.josegaldamez.bitacora_de_campo.models;

import com.google.gson.annotations.SerializedName;

public class Record {
    @SerializedName("id")
    private int id;

    @SerializedName("esPropio")
    private boolean esPropio;

    public boolean isPropio() {
        return esPropio;
    }

    @SerializedName("titulo")
    private String titulo;

    @SerializedName("descripcion")
    private String descripcion;

    @SerializedName("tieneFoto")
    private boolean tieneFoto;

    @SerializedName(value = "fotoBase64", alternate = {"fotografiaBase64", "fotografia", "fotografia_base64"})
    private String fotografiaBase64;

    @SerializedName(value = "fechaCreacion", alternate = {"fecha"})
    private String fecha;

    public Record() {
    }

    public Record(int id, String titulo, String descripcion, String fotografiaBase64, String fecha) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fotografiaBase64 = fotografiaBase64;
        this.tieneFoto = fotografiaBase64 != null && !fotografiaBase64.isEmpty();
        this.fecha = fecha;
    }

    public Record(String titulo, String descripcion, String fotografiaBase64, String fecha) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fotografiaBase64 = fotografiaBase64;
        this.tieneFoto = fotografiaBase64 != null && !fotografiaBase64.isEmpty();
        this.fecha = fecha;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isTieneFoto() {
        return tieneFoto;
    }

    public void setTieneFoto(boolean tieneFoto) {
        this.tieneFoto = tieneFoto;
    }

    public String getFotografiaBase64() {
        return fotografiaBase64;
    }

    public void setFotografiaBase64(String fotografiaBase64) {
        this.fotografiaBase64 = fotografiaBase64;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}
