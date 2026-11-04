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

    private static final String BASE_URL = "http://10.0.2.2:5033/";
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
