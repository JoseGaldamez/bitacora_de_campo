package dev.josegaldamez.bitacora_de_campo.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import dev.josegaldamez.bitacora_de_campo.models.Record;

public class DatabaseService extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bitacora_campo.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "registros";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITULO = "titulo";
    public static final String COLUMN_DESCRIPCION = "descripcion";
    public static final String COLUMN_FOTOGRAFIA_BASE64 = "fotografia_base64";
    public static final String COLUMN_FECHA = "fecha";

    public DatabaseService(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITULO + " TEXT, " +
                COLUMN_DESCRIPCION + " TEXT, " +
                COLUMN_FOTOGRAFIA_BASE64 + " TEXT, " +
                COLUMN_FECHA + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public long insertRecord(Record record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITULO, record.getTitulo());
        values.put(COLUMN_DESCRIPCION, record.getDescripcion());
        values.put(COLUMN_FOTOGRAFIA_BASE64, record.getFotografiaBase64());
        values.put(COLUMN_FECHA, record.getFecha());

        long id = db.insert(TABLE_NAME, null, values);
        db.close();
        return id;
    }

    public List<Record> getAllRecords() {
        List<Record> records = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_ID + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Record record = new Record();
                record.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                record.setTitulo(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITULO)));
                record.setDescripcion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPCION)));
                record.setFotografiaBase64(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOTOGRAFIA_BASE64)));
                record.setFecha(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA)));
                records.add(record);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return records;
    }

    public Record getRecordById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME,
                new String[]{COLUMN_ID, COLUMN_TITULO, COLUMN_DESCRIPCION, COLUMN_FOTOGRAFIA_BASE64, COLUMN_FECHA},
                COLUMN_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null);

        Record record = null;
        if (cursor != null && cursor.moveToFirst()) {
            record = new Record(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITULO)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPCION)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOTOGRAFIA_BASE64)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA))
            );
            cursor.close();
        }
        db.close();
        return record;
    }

    public int deleteRecord(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public int updateRecord(Record record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITULO, record.getTitulo());
        values.put(COLUMN_DESCRIPCION, record.getDescripcion());
        values.put(COLUMN_FOTOGRAFIA_BASE64, record.getFotografiaBase64());
        values.put(COLUMN_FECHA, record.getFecha());

        int rows = db.update(TABLE_NAME, values, COLUMN_ID + "=?", new String[]{String.valueOf(record.getId())});
        db.close();
        return rows;
    }
}
