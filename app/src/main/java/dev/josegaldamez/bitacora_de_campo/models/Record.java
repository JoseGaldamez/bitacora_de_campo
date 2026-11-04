package dev.josegaldamez.bitacora_de_campo.models;

public class Record {
    private int id;
    private String titulo;
    private String descripcion;
    private String fotografiaBase64;
    private String fecha;

    public Record() {
    }

    public Record(int id, String titulo, String descripcion, String fotografiaBase64, String fecha) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fotografiaBase64 = fotografiaBase64;
        this.fecha = fecha;
    }

    public Record(String titulo, String descripcion, String fotografiaBase64, String fecha) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fotografiaBase64 = fotografiaBase64;
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
