package dev.josegaldamez.bitacora_de_campo.data.remote;

import androidx.annotation.NonNull;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL =
            "https://api-bitacora-grupo3-337965348211.us-central1.run.app/";
    public static final int TIMEOUT_MS = 15_000;

    public static String getRecordsUrl() {
        return BASE_URL + RegistroApi.LIST_PATH;
    }

    private static Retrofit retrofit = null;
    private static dev.josegaldamez.bitacora_de_campo.utils.SessionManager session;
    private static boolean debug;
    public static void initialize(android.content.Context context) {
        session = new dev.josegaldamez.bitacora_de_campo.utils.SessionManager(context);
        debug = (context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
    public static AuthApi getAuthApi() {
        getApi();
        return retrofit.create(AuthApi.class);
    }

    public static synchronized RegistroApi getApi() {
        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.redactHeader("Authorization");
            loggingInterceptor.setLevel(debug ? HttpLoggingInterceptor.Level.BASIC : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .dns(new Dns() {
                        @NonNull
                        @Override
                        public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
                            List<InetAddress> addresses = Dns.SYSTEM.lookup(hostname);
                            List<InetAddress> ipv4Addresses = new ArrayList<>();
                            for (InetAddress address : addresses) {
                                if (address instanceof Inet4Address) {
                                    ipv4Addresses.add(address);
                                }
                            }
                            return ipv4Addresses.isEmpty() ? addresses : ipv4Addresses;
                        }
                    })
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .addInterceptor(chain -> {
                        okhttp3.Request request = chain.request();
                        okhttp3.HttpUrl base = okhttp3.HttpUrl.get(BASE_URL);
                        boolean same = request.url().host().equals(base.host()) && request.url().scheme().equals(base.scheme()) && request.url().port() == base.port();
                        String path = request.url().encodedPath();
                        boolean publicRequest = path.equals("/api/auth/login") || path.equals("/api/auth/registro");
                        if (same && Boolean.FALSE.equals(request.tag(Boolean.class))) {
                            return chain.proceed(request.newBuilder().removeHeader("Authorization").build());
                        }
                        String token = session == null ? null : session.getToken();
                        if (same && !publicRequest && token == null) {
                            if (session != null) session.logout();
                            return new okhttp3.Response.Builder().request(request).protocol(okhttp3.Protocol.HTTP_1_1)
                                    .code(401).message("Unauthorized").body(okhttp3.ResponseBody.create(null, new byte[0])).build();
                        }
                        okhttp3.Request.Builder builder = request.newBuilder().removeHeader("Authorization");
                        if (same && !publicRequest) builder.header("Authorization", "Bearer " + token);
                        okhttp3.Response response = chain.proceed(builder.build());
                        if (same && !publicRequest && response.code() == 401 && session != null) session.invalidate(token);
                        return response;
                    })
                    .addInterceptor(loggingInterceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(RegistroApi.class);
    }
}
