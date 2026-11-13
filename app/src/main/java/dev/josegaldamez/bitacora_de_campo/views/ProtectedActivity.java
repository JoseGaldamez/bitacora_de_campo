package dev.josegaldamez.bitacora_de_campo.views;
import android.content.Intent;
import android.os.*;
import androidx.appcompat.app.AppCompatActivity;
import dev.josegaldamez.bitacora_de_campo.utils.SessionManager;
public abstract class ProtectedActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean leaving;
    private final Runnable expire = () -> { new SessionManager(this).logout(); requireSession(); };
    protected boolean requireSession() {
        if (new SessionManager(this).isLoggedIn()) return true;
        if (!leaving) {
            leaving = true;
            startActivity(new Intent(this, LoginActivity.class).putExtra("expired", true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }
        return false;
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        new SessionManager(this).validity().observe(this, valid -> { if (!new SessionManager(this).isLoggedIn()) requireSession(); });
    }
    @Override protected void onResume() {
        super.onResume();
        if (requireSession()) handler.postDelayed(expire, Math.max(1, new SessionManager(this).getExpiration() - System.currentTimeMillis()));
    }
    @Override protected void onPause() { handler.removeCallbacks(expire); super.onPause(); }
}