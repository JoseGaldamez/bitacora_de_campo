package dev.josegaldamez.bitacora_de_campo.views

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NoConnectionError
import com.android.volley.Request
import com.android.volley.ServerError
import com.android.volley.TimeoutError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.josegaldamez.bitacora_de_campo.R
import dev.josegaldamez.bitacora_de_campo.data.remote.RetrofitClient
import dev.josegaldamez.bitacora_de_campo.models.Record
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Response as RetrofitResponse
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class ComparativaActivity : AppCompatActivity() {

    private lateinit var btnVolley: Button
    private lateinit var btnOkHttp: Button
    private lateinit var btnRetrofit: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvCliente: TextView
    private lateinit var tvTiempo: TextView
    private lateinit var tvRegistros: TextView
    private lateinit var tvEstado: TextView

    private val endpointUrl = "http://localhost:5033/api/registros"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comparativa)

        btnVolley = findViewById(R.id.btnVolley)
        btnOkHttp = findViewById(R.id.btnOkHttp)
        btnRetrofit = findViewById(R.id.btnRetrofit)
        progressBar = findViewById(R.id.progressBar)
        tvCliente = findViewById(R.id.tvCliente)
        tvTiempo = findViewById(R.id.tvTiempo)
        tvRegistros = findViewById(R.id.tvRegistros)
        tvEstado = findViewById(R.id.tvEstado)

        btnVolley.setOnClickListener { probarVolley() }
        btnOkHttp.setOnClickListener { probarOkHttp() }
        btnRetrofit.setOnClickListener { probarRetrofit() }
    }

    private fun mostrarCargando(cliente: String) {
        progressBar.visibility = View.VISIBLE
        tvCliente.text = "Cliente: $cliente"
        tvTiempo.text = "Tiempo de respuesta: Calculando..."
        tvRegistros.text = "Registros recibidos: --"
        tvEstado.text = "Estado: Enviando petición..."
        habilitarBotones(false)
    }

    private fun mostrarExito(cliente: String, cantidad: Int, tiempoMs: Long) {
        progressBar.visibility = View.GONE
        tvCliente.text = "Cliente: $cliente"
        tvTiempo.text = "Tiempo de respuesta: $tiempoMs ms"
        tvRegistros.text = "Registros recibidos: $cantidad"
        tvEstado.text = "Estado: 200 OK (Éxito)"
        habilitarBotones(true)
    }

    private fun mostrarError(cliente: String, errorMsj: String, tiempoMs: Long) {
        progressBar.visibility = View.GONE
        tvCliente.text = "Cliente: $cliente"
        tvTiempo.text = "Tiempo de respuesta: $tiempoMs ms"
        tvRegistros.text = "Registros recibidos: 0"
        tvEstado.text = "Estado: $errorMsj"
        habilitarBotones(true)
    }

    private fun habilitarBotones(habilitar: Boolean) {
        btnVolley.isEnabled = habilitar
        btnOkHttp.isEnabled = habilitar
        btnRetrofit.isEnabled = habilitar
    }

    private fun traducirExcepcionRed(t: Throwable): String {
        val msg = t.message.orEmpty()
        return when {
            t is SocketTimeoutException ->
                "TimeoutError (${t.javaClass.simpleName}) -> Tiempo de espera agotado."
            t is ConnectException || msg.contains("Failed to connect", ignoreCase = true) ->
                "ConnectException -> No se pudo conectar con la API (API detenida)."
            t is SocketException || msg.contains("Network is unreachable", ignoreCase = true) ->
                "SocketException -> Sin conexión de red (Modo avión activo)."
            else ->
                "${t.javaClass.simpleName} -> ${t.message ?: "Error de red"}"
        }
    }

    private fun traducirCodigoHttp(codigo: Int): String {
        return when (codigo) {
            401 -> "HTTP 401 -> No autorizado: Token ausente, inválido o vencido."
            500 -> "HTTP 500 -> Error interno del servidor."
            else -> "HTTP $codigo -> Error inesperado del servidor."
        }
    }

    // -------------------------------------------------------------
    // 1. VOLLEY (Respuesta llega en Hilo Principal)
    // -------------------------------------------------------------
    private fun probarVolley() {
        mostrarCargando("Volley")
        val inicio = System.currentTimeMillis()
        val cola = Volley.newRequestQueue(this)

        val request = object : JsonArrayRequest(
            Request.Method.GET,
            endpointUrl,
            null,
            { response ->
                val tiempo = System.currentTimeMillis() - inicio
                mostrarExito("Volley", response.length(), tiempo)
            },
            { error ->
                val tiempo = System.currentTimeMillis() - inicio
                val codigo = error.networkResponse?.statusCode
                val mensaje = when {
                    error is TimeoutError ->
                        "TimeoutError -> Tiempo de espera agotado."
                    codigo == 401 || error is AuthFailureError ->
                        "AuthFailureError (401) -> No autorizado: Token ausente, inválido o vencido."
                    codigo == 500 || error is ServerError ->
                        "ServerError (${codigo ?: 500}) -> Error interno del servidor."
                    error is NoConnectionError -> {
                        val causa = error.cause?.message.orEmpty()
                        if (causa.contains("Network is unreachable", ignoreCase = true)) {
                            "NoConnectionError -> Sin conexión de red (Modo avión activo)."
                        } else {
                            "NoConnectionError -> No se pudo conectar con la API (API detenida)."
                        }
                    }
                    else -> "${error.javaClass.simpleName} -> ${error.message ?: "Error de red"}"
                }
                mostrarError("Volley", mensaje, tiempo)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val token = RetrofitClient.TOKEN_JWT
                if (!token.isNullOrEmpty()) {
                    headers["Authorization"] = "Bearer $token"
                }
                return headers
            }
        }

        request.retryPolicy = DefaultRetryPolicy(
            5000,
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        cola.add(request)
    }

    // -------------------------------------------------------------
    // 2. OKHTTP (Respuesta llega en Hilo Secundario -> runOnUiThread)
    // -------------------------------------------------------------
    private fun probarOkHttp() {
        mostrarCargando("OkHttp")
        val inicio = System.currentTimeMillis()

        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        val requestBuilder = okhttp3.Request.Builder()
            .url(endpointUrl)
            .get()

        val token = RetrofitClient.TOKEN_JWT
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        client.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val tiempo = System.currentTimeMillis() - inicio
                val mensaje = traducirExcepcionRed(e)
                runOnUiThread {
                    mostrarError("OkHttp", mensaje, tiempo)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val tiempo = System.currentTimeMillis() - inicio
                response.use { res ->
                    val codigo = res.code
                    val cuerpo = res.body?.string()
                    runOnUiThread {
                        if (res.isSuccessful && cuerpo != null) {
                            try {
                                val tipoLista = object : TypeToken<List<Record>>() {}.type
                                val registros: List<Record> = Gson().fromJson(cuerpo, tipoLista)
                                mostrarExito("OkHttp", registros.size, tiempo)
                            } catch (e: Exception) {
                                mostrarError("OkHttp", "Error al deserializar JSON", tiempo)
                            }
                        } else {
                            mostrarError("OkHttp", traducirCodigoHttp(codigo), tiempo)
                        }
                    }
                }
            }
        })
    }

    // -------------------------------------------------------------
    // 3. RETROFIT (Reutiliza RetrofitClient.getApi())
    // -------------------------------------------------------------
    private fun probarRetrofit() {
        mostrarCargando("Retrofit")
        val inicio = System.currentTimeMillis()

        val call = RetrofitClient.getApi().records

        call.enqueue(object : retrofit2.Callback<List<Record>> {
            override fun onResponse(
                call: retrofit2.Call<List<Record>>,
                response: RetrofitResponse<List<Record>>
            ) {
                val tiempo = System.currentTimeMillis() - inicio
                if (response.isSuccessful) {
                    val cantidad = response.body()?.size ?: 0
                    mostrarExito("Retrofit", cantidad, tiempo)
                } else {
                    mostrarError("Retrofit", traducirCodigoHttp(response.code()), tiempo)
                }
            }

            override fun onFailure(call: retrofit2.Call<List<Record>>, t: Throwable) {
                val tiempo = System.currentTimeMillis() - inicio
                mostrarError("Retrofit", traducirExcepcionRed(t), tiempo)
            }
        })
    }
}