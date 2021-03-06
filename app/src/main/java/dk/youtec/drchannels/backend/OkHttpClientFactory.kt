package dk.youtec.drchannels.backend

import android.content.Context
import android.util.Log

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

object OkHttpClientFactory {

    private val TAG = OkHttpClientFactory::class.java.simpleName
    private val SIZE_OF_CACHE = (10 * 1024 * 1024).toLong()
    private var client: OkHttpClient? = null

    /**
     * It is an error to have multiple caches accessing the same cache directory simultaneously.
     * Most applications should call new OkHttpClient() exactly once, configure it with their cache,
     * and use that same instance everywhere. Otherwise the two cache instances will stomp on each other,
     * corrupt the response cache, and possibly crash your program.
     *
     *
     * https://github.com/square/okhttp/wiki/Recipes#response-caching

     * @return singleTon instance of the OkHttpClient.
     */
    fun getInstance(context: Context): OkHttpClient {
        return if (client == null) {
            val builder = OkHttpClient.Builder()
            builder.connectTimeout(30, TimeUnit.SECONDS)
            builder.writeTimeout(30, TimeUnit.SECONDS)
            builder.readTimeout(30, TimeUnit.SECONDS)

            builder.addNetworkInterceptor(LoggingInterceptor())

            enableCache(context, builder)

            client = builder.build()
            client as OkHttpClient
        } else {
            client as OkHttpClient
        }
    }

    private fun enableCache(context: Context, builder: OkHttpClient.Builder) {
        try {
            val cacheDirectory = File(context.cacheDir.absolutePath, "okhttp")
            val responseCache = Cache(cacheDirectory, SIZE_OF_CACHE)
            builder.cache(responseCache)
        } catch (e: Exception) {
            Log.d(TAG, "Unable to set http cache", e)
        }

    }

    fun clearCache() {
        if (client != null) {
            try {
                (client as OkHttpClient).cache().evictAll()
            } catch (e: IOException) {
                Log.e(TAG, e.message, e)
            }

        }
    }

    private class LoggingInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()

            val t1 = System.nanoTime()
            //L.v(TAG, String.format("Sending request %s on %s%n%s", request.url(), chain.connection(), request.headers()));
            Log.v(TAG, String.format("Sending request %s", request.url()))

            val response = chain.proceed(request)

            val t2 = System.nanoTime()
            //L.v(TAG, String.format("Received response for %s in %.1fms%n%s", response.request().url(), (t2 - t1) / 1e6d, response.headers()));
            Log.v(TAG, String.format("Received response for %s in %.1fms%n", response.request().url(), (t2 - t1) / 1e6))

            return response
        }
    }
}
