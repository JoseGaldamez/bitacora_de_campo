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
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "http://localhost:5033/";
    public static String TOKEN_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1laWQiOiIzIiwiZW1haWwiOiJ3ZWJhbmVnYXNAdXRoLmhuIiwicm9sZSI6IkFkbWluIiwibmJmIjoxNzk0NjMzMTQ3LCJleHAiOjE3OTQ2MzM3NDcsImlhdCI6MTc5NDYzMzE0NywiaXNzIjoiQml0YWNvcmFBcGkiLCJhdWQiOiJCaXRhY29yYUFwcCJ9.5WKDnDyt-0hM1dUsCoIHweEfLgtJvG4af86mjoDNni8";
    private static Retrofit retrofit = null;

    public static RegistroApi getApi() {
        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
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
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder();
                        if (TOKEN_JWT != null && !TOKEN_JWT.isEmpty()) {
                            builder.header("Authorization", "Bearer " + TOKEN_JWT);
                        }
                        return chain.proceed(builder.build());
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