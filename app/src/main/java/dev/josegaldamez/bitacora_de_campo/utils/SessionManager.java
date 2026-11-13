package dev.josegaldamez.bitacora_de_campo.utils;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import java.io.*;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import dev.josegaldamez.bitacora_de_campo.models.LoginResponse;

public class SessionManager {
    private static JSONObject data = new JSONObject();
    private static boolean loaded;
    private static final androidx.lifecycle.MutableLiveData<Boolean> validity = new androidx.lifecycle.MutableLiveData<>();
    private final File file;

    public SessionManager(Context context) {
        file = new File(context.getNoBackupFilesDir(), "jwt_session");
        context.getSharedPreferences("bitacora_session_pref", Context.MODE_PRIVATE).edit().clear().apply();
        synchronized (SessionManager.class) {
            if (loaded) return;
            loaded = true;
            try (DataInputStream input = new DataInputStream(new FileInputStream(file))) {
                byte[] iv = new byte[12];
                input.readFully(iv);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int n;
                while ((n = input.read(buffer)) != -1) bytes.write(buffer, 0, n);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
                data = new JSONObject(new String(cipher.doFinal(bytes.toByteArray()), java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception ignored) { data = new JSONObject(); }
        }
    }

    private javax.crypto.SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias("bitacora_jwt")) return (javax.crypto.SecretKey) store.getKey("bitacora_jwt", null);
        KeyGenerator generator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder("bitacora_jwt", KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }

    public boolean save(LoginResponse response) {
        synchronized (SessionManager.class) {
            try {
                if (response == null || response.token == null || response.email == null || response.email.isEmpty()
                        || response.role == null || response.role.isEmpty() || response.expiration == null) return false;
                String[] parts = response.token.split("\\.");
                if (parts.length != 3 || parts[2].isEmpty()) return false;
                JSONObject claims = new JSONObject(new String(Base64.decode(parts[1], Base64.URL_SAFE), java.nio.charset.StandardCharsets.UTF_8));
                long expiry = Math.min(Math.multiplyExact(claims.getLong("exp"), 1000L), parseExpiration(response.expiration));
                if (expiry <= System.currentTimeMillis()) return false;
                JSONObject next = new JSONObject().put("token", response.token).put("email", response.email)
                        .put("role", response.role).put("expiry", expiry);
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, key());
                byte[] encrypted = cipher.doFinal(next.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                try (FileOutputStream output = new FileOutputStream(file)) {
                    output.write(cipher.getIV());
                    output.write(encrypted);
                }
                data = next;
                validity.postValue(true);
                return true;
            } catch (Exception ignored) { logout(); return false; }
        }
    }

    private long parseExpiration(String value) {
        String normalized = value.replaceFirst("(\\.\\d{3})\\d+", "$1");
        for (String pattern : new String[]{"yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss"}) {
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(pattern, java.util.Locale.US);
            format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            format.setLenient(false);
            java.text.ParsePosition position = new java.text.ParsePosition(0);
            java.util.Date result = format.parse(normalized, position);
            if (result != null && position.getIndex() == normalized.length()) return result.getTime();
        }
        throw new IllegalArgumentException();
    }
    public boolean isLoggedIn() { synchronized (SessionManager.class) { return !data.optString("token").isEmpty() && getExpiration() > System.currentTimeMillis(); } }
    public long getExpiration() { synchronized (SessionManager.class) { return data.optLong("expiry"); } }
    public String getToken() { synchronized (SessionManager.class) { return isLoggedIn() ? data.optString("token") : null; } }
    public String getUserEmail() { synchronized (SessionManager.class) { return data.optString("email"); } }
    public String getUserName() { return getUserEmail(); }
    public String getRole() { synchronized (SessionManager.class) { return data.optString("role"); } }
    public androidx.lifecycle.LiveData<Boolean> validity() { return validity; }
    public void invalidate(String token) { synchronized (SessionManager.class) { if (data.optString("token").equals(token)) logout(); } }
    public void logout() {
        synchronized (SessionManager.class) {
            data = new JSONObject();
            file.delete();
            validity.postValue(false);
        }
    }
}