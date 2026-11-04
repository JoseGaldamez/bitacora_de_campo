package dev.josegaldamez.bitacora_de_campo.controllers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dev.josegaldamez.bitacora_de_campo.models.Record;
import dev.josegaldamez.bitacora_de_campo.services.DatabaseService;

public class RecordController {

    private final DatabaseService dbService;

    public RecordController(Context context) {
        this.dbService = new DatabaseService(context);
    }

    public boolean addRecord(String titulo, String descripcion, Bitmap photoBitmap) {
        String base64Photo = "";
        if (photoBitmap != null) {
            base64Photo = bitmapToBase64(photoBitmap);
        }

        String fechaActual = getCurrentDateFormatted();
        Record newRecord = new Record(titulo, descripcion, base64Photo, fechaActual);

        long result = dbService.insertRecord(newRecord);
        return result != -1;
    }

    public List<Record> getAllRecords() {
        return dbService.getAllRecords();
    }

    public Record getRecordById(int id) {
        return dbService.getRecordById(id);
    }

    public boolean deleteRecord(int id) {
        int rowsAffected = dbService.deleteRecord(id);
        return rowsAffected > 0;
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public static Bitmap base64ToBitmap(String base64Str) {
        if (base64Str == null || base64Str.trim().isEmpty()) return null;
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getCurrentDateFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }
}
