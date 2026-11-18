package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.FeeEngine
import com.example.data.repository.GymRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

private val Context.localAuthDataStore by preferencesDataStore(name = "local_gym_server_auth")

data class LocalSession(
    val appId: String,
    val sessionToken: String,
    val loggedInAtMillis: Long
)

class LocalAuthStore(private val context: Context) {

    companion object {
        const val DEFAULT_APP_ID = "imran"
        const val DEFAULT_PASSWORD = "imran123"
        private const val SALT = "GymFeeLocalServerSalt_v1"

        private val KEY_LOGGED_IN = booleanPreferencesKey("logged_in")
        private val KEY_APP_ID = stringPreferencesKey("app_id")
        private val KEY_TOKEN = stringPreferencesKey("session_token")
        private val KEY_LOGGED_IN_AT = longPreferencesKey("logged_in_at")
        private val KEY_CONFIGURED_APP_ID = stringPreferencesKey("configured_app_id")
        private val KEY_PASSWORD_HASH = stringPreferencesKey("password_hash")

        fun sha256(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest("$SALT:$input".toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    val sessionFlow: Flow<LocalSession?> = context.localAuthDataStore.data.map { prefs ->
        val loggedIn = prefs[KEY_LOGGED_IN] ?: false
        val appId = prefs[KEY_APP_ID]
        val token = prefs[KEY_TOKEN]
        if (loggedIn && !appId.isNullOrBlank() && !token.isNullOrBlank()) {
            LocalSession(
                appId = appId,
                sessionToken = token,
                loggedInAtMillis = prefs[KEY_LOGGED_IN_AT] ?: 0L
            )
        } else {
            null
        }
    }

    suspend fun verifyCredentials(appIdInput: String, passwordInput: String): Boolean {
        val prefs = context.localAuthDataStore.data.first()
        val storedAppId = prefs[KEY_CONFIGURED_APP_ID] ?: DEFAULT_APP_ID
        val storedHash = prefs[KEY_PASSWORD_HASH] ?: sha256(DEFAULT_PASSWORD)

        val cleanAppId = appIdInput.trim()
        return cleanAppId.equals(storedAppId, ignoreCase = true) &&
            sha256(passwordInput) == storedHash
    }

    suspend fun saveSession(appId: String, token: String = UUID.randomUUID().toString()): LocalSession {
        val now = System.currentTimeMillis()
        val normalizedId = appId.trim().lowercase()
        context.localAuthDataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = true
            prefs[KEY_APP_ID] = normalizedId
            prefs[KEY_TOKEN] = token
            prefs[KEY_LOGGED_IN_AT] = now
        }
        return LocalSession(normalizedId, token, now)
    }

    suspend fun updatePassword(newPassword: String) {
        require(newPassword.length >= 4) { "Password must be at least 4 characters" }
        context.localAuthDataStore.edit { prefs ->
            prefs[KEY_PASSWORD_HASH] = sha256(newPassword)
        }
    }

    suspend fun clearSession() {
        context.localAuthDataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = false
            prefs.remove(KEY_TOKEN)
        }
    }
}

class LocalGymHttpServer(
    private val dao: GymRecordDao,
    private val authStore: LocalAuthStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    var port: Int = 8765
        private set

    @Volatile
    var isRunning: Boolean = false
        private set

    val baseUrl: String get() = "http://127.0.0.1:$port"

    fun start() {
        if (isRunning) return
        scope.launch {
            try {
                val socket = try {
                    ServerSocket(8765, 16, InetAddress.getByName("127.0.0.1"))
                } catch (_: Exception) {
                    ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
                }
                serverSocket = socket
                port = socket.localPort
                isRunning = true
                while (isRunning && !socket.isClosed) {
                    val client = try {
                        socket.accept()
                    } catch (_: Exception) {
                        break
                    }
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                Log.w("LocalGymHttpServer", "Loopback server bind skipped: ${e.message}")
                isRunning = false
            }
        }
    }

    private suspend fun handleClient(client: Socket) {
        client.use { sock ->
            try {
                sock.soTimeout = 5000
                val reader = BufferedReader(InputStreamReader(sock.getInputStream(), Charsets.UTF_8))
                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                val method = parts.getOrNull(0) ?: "GET"
                val path = parts.getOrNull(1) ?: "/"

                var contentLength = 0
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                    if (line.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                }

                val body = if (contentLength > 0) {
                    val buf = CharArray(contentLength.coerceAtMost(65536))
                    var totalRead = 0
                    while (totalRead < buf.size) {
                        val read = reader.read(buf, totalRead, buf.size - totalRead)
                        if (read == -1) break
                        totalRead += read
                    }
                    String(buf, 0, totalRead)
                } else {
                    ""
                }

                val (statusCode, responseJson) = routeRequest(method, path, body)
                val statusText = when (statusCode) {
                    200 -> "OK"
                    401 -> "Unauthorized"
                    404 -> "Not Found"
                    else -> "Bad Request"
                }
                val bytes = responseJson.toByteArray(Charsets.UTF_8)
                val out = OutputStreamWriter(sock.getOutputStream(), Charsets.UTF_8)
                out.write("HTTP/1.1 $statusCode $statusText\r\n")
                out.write("Content-Type: application/json; charset=utf-8\r\n")
                out.write("Content-Length: ${bytes.size}\r\n")
                out.write("Connection: close\r\n\r\n")
                out.write(responseJson)
                out.flush()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun routeRequest(method: String, path: String, body: String): Pair<Int, String> {
        val ownerId = LocalAuthStore.DEFAULT_APP_ID
        return when {
            method == "GET" && path == "/health" -> {
                200 to JSONObject().apply {
                    put("status", "ok")
                    put("server", "GymFeeManagerLocalServer")
                    put("port", port)
                    put("appId", ownerId)
                }.toString()
            }
            method == "POST" && path == "/api/auth/login" -> {
                val req = runCatching { JSONObject(body) }.getOrElse { JSONObject() }
                val appId = req.optString("appId")
                val password = req.optString("password")
                if (authStore.verifyCredentials(appId, password)) {
                    val token = UUID.randomUUID().toString()
                    200 to JSONObject().apply {
                        put("authenticated", true)
                        put("appId", appId.trim().lowercase())
                        put("sessionToken", token)
                    }.toString()
                } else {
                    401 to JSONObject().apply {
                        put("authenticated", false)
                        put("error", "Invalid App ID or Password")
                    }.toString()
                }
            }
            method == "GET" && path == "/api/members" -> {
                val members = dao.getCollectionOnce(ownerId, "members")
                    .map { JSONObject(it.jsonPayload) }
                200 to JSONObject().apply {
                    put("count", members.size)
                    put("members", JSONArray(members))
                }.toString()
            }
            method == "GET" && path == "/api/dashboard" -> {
                val members = dao.getCollectionOnce(ownerId, "members").map { GymJsonCodec.decodeMember(it.jsonPayload) }
                val payments = dao.getCollectionOnce(ownerId, "payments").map { GymJsonCodec.decodePayment(it.jsonPayload) }
                val expenses = dao.getCollectionOnce(ownerId, "expenses").map { GymJsonCodec.decodeExpense(it.jsonPayload) }
                val supplements = dao.getCollectionOnce(ownerId, "supplements").map { GymJsonCodec.decodeSupplement(it.jsonPayload) }
                val equipment = dao.getCollectionOnce(ownerId, "equipment").map { GymJsonCodec.decodeEquipment(it.jsonPayload) }
                val metrics = FeeEngine.computeDashboard(members, payments, expenses, supplements, equipment, "ALL")
                200 to JSONObject().apply {
                    put("todaysCollection", metrics.todaysCollection)
                    put("todaysDueFees", metrics.todaysDueFees)
                    put("overdueFees", metrics.overdueFees)
                    put("totalMembers", metrics.totalMembers)
                    put("activeMembers", metrics.activeMembers)
                    put("expiredMembers", metrics.expiredMembers)
                    put("monthlyRevenue", metrics.monthlyRevenue)
                    put("monthlyExpenses", metrics.monthlyExpenses)
                    put("totalProfit", metrics.totalProfit)
                }.toString()
            }
            else -> {
                404 to JSONObject().put("error", "Route not found").toString()
            }
        }
    }

    suspend fun loginViaServer(appId: String, password: String): Result<LocalSession> = withContext(Dispatchers.IO) {
        if (!isRunning) start()
        if (isRunning) {
            try {
                val url = URL("$baseUrl/api/auth/login")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 2000
                    readTimeout = 2000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                val payload = JSONObject().apply {
                    put("appId", appId)
                    put("password", password)
                }.toString()
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload) }
                val code = conn.responseCode
                val respText = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: "{}"
                conn.disconnect()
                val json = runCatching { JSONObject(respText) }.getOrElse { JSONObject() }
                if (code == 200 && json.optBoolean("authenticated", false)) {
                    val session = authStore.saveSession(
                        appId = json.optString("appId", appId),
                        token = json.optString("sessionToken", UUID.randomUUID().toString())
                    )
                    return@withContext Result.success(session)
                } else {
                    return@withContext Result.failure(IllegalArgumentException("Invalid App ID or Password"))
                }
            } catch (_: Exception) {
                // Fallback to direct LocalAuthStore verification if loopback socket wasn't ready
            }
        }

        if (authStore.verifyCredentials(appId, password)) {
            Result.success(authStore.saveSession(appId))
        } else {
            Result.failure(IllegalArgumentException("Invalid App ID or Password"))
        }
    }

    fun stop() {
        isRunning = false
        runCatching { serverSocket?.close() }
        serverSocket = null
    }
}

class LocalServerHost private constructor(context: Context) {
    val database: GymDatabase = GymDatabase.getInstance(context.applicationContext)
    val authStore: LocalAuthStore = LocalAuthStore(context.applicationContext)
    val httpServer: LocalGymHttpServer = LocalGymHttpServer(database.gymRecordDao(), authStore).apply { start() }
    val repository: GymRepository = GymRepository(database.gymRecordDao())

    companion object {
        @Volatile
        private var INSTANCE: LocalServerHost? = null

        fun getInstance(context: Context): LocalServerHost {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalServerHost(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
