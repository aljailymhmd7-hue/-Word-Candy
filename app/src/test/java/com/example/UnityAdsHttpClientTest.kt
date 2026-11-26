package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ads.UnityAdsManager
import com.unity3d.ads.core.domain.HttpClientProvider
import com.unity3d.services.core.device.StorageManager
import com.unity3d.services.core.di.ServiceKey
import com.unity3d.services.core.di.ServiceProvider
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UnityAdsHttpClientTest {

    @Test
    fun `configureNonGmsHttpClient selects OkHttp3Client without GMS Cronet`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        UnityAdsManager.configureNonGmsHttpClient(context)

        val memoryStorage = StorageManager.getStorage(StorageManager.StorageType.MEMORY)
        val traits = memoryStorage?.get("mediation.traits.value") as? JSONObject
        assertEquals("OKHTTP3", traits?.optString("useHttpClient"))

        val registry = ServiceProvider.getRegistry()
        val readerClass = Class.forName("com.unity3d.ads.core.configuration.MediationTraitsMetadataReader").kotlin
        val reader = registry.resolveService<Any>(ServiceKey("", readerClass))
        val getStringTrait = readerClass.java.getMethod("getStringTrait", String::class.java)
        assertEquals("OKHTTP3", getStringTrait.invoke(reader, "useHttpClient"))

        val httpClientProvider = registry.resolveService<HttpClientProvider>(
            ServiceKey("", HttpClientProvider::class)
        )
        val client = httpClientProvider.invoke()
        assertEquals("OkHttp3Client", client::class.java.simpleName)
    }
}
