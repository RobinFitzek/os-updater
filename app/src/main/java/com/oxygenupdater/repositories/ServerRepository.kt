package com.oxygenupdater.repositories

import android.util.Log
import android.content.SharedPreferences
import androidx.collection.ArrayMap
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.apis.ServerApi
import com.oxygenupdater.dao.ArticleDao
import com.oxygenupdater.dao.UpdateDataDao
import com.oxygenupdater.extensions.get
import com.oxygenupdater.extensions.set
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.KeyDevice
import com.oxygenupdater.internal.settings.KeyDeviceId
import com.oxygenupdater.internal.settings.KeyNotificationDelayInSeconds
import com.oxygenupdater.internal.settings.KeyUpdateMethodId
import com.oxygenupdater.models.Article
import com.oxygenupdater.models.DeviceRequestFilter
import com.oxygenupdater.models.DownloadErrorBody
import com.oxygenupdater.models.FreshUpdateDataDownloadUrlBody
import com.oxygenupdater.models.ServerStatus
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.utils.logInfo
import com.oxygenupdater.utils.logVerbose
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val serverApi: ServerApi,
    private val updateDataDao: UpdateDataDao,
    private val articleDao: ArticleDao,
) {

    suspend fun fetchFaq() = performServerRequest { serverApi.fetchFaq() }

    suspend fun fetchInstallGuide() = performServerRequest { serverApi.fetchInstallGuide() }

    suspend fun fetchDevices(filter: DeviceRequestFilter) = performServerRequest {
        serverApi.fetchDevices(filter.value)
    }

    val updateDataFlow
        get() = updateDataDao.getFlow()

    /**
     * Always hits the network (requests carry `Cache-Control: no-store`), and updates
     * the local [updateDataDao] only when [persist] is `true` (UI-driven refreshes).
     *
     * @return the freshly fetched [UpdateData], or `null` on failure
     */
    suspend fun fetchUpdateData(
        deviceId: Long,
        methodId: Long,
        persist: Boolean = true,
        regionOverride: String = "",
    ): UpdateData? {
        val isEu = regionOverride == "EU" || (regionOverride.isEmpty() && SystemVersionProperties.isEuBuild)
        val fetched = performServerRequest {
            serverApi.fetchUpdateData(
                deviceId = deviceId,
                updateMethodId = methodId,
                incrementalSystemVersion = SystemVersionProperties.otaVersion,
                osVersion = SystemVersionProperties.osVersion,
                osType = SystemVersionProperties.osType,
                fingerprint = SystemVersionProperties.fingerprint,
                oplusPipeline = regionOverride.ifEmpty { SystemVersionProperties.pipeline },
                oplusPipelineCode = SystemVersionProperties.pipelineCode,
                oplusManifestHash = SystemVersionProperties.manifestHash,
                deviceMarketName = SystemVersionProperties.deviceMarketName,
                isEuBuild = isEu,
                appVersion = BuildConfig.VERSION_NAME,
                regionOverride = regionOverride,
            )
        } ?: return null

        val result = if (fetched.shouldFetchMostRecent) performServerRequest {
            serverApi.fetchMostRecentUpdateData(
                deviceId = deviceId,
                updateMethodId = methodId,
            )
        } ?: fetched else fetched

        if (persist) updateDataDao.refresh(result)
        return result
    }

    suspend fun getFreshUpdateDataDownloadUrl(
        deviceId: Long,
        methodId: Long,
        updateData: UpdateData,
    ) = performServerRequest {
        serverApi.getFreshUpdateDataDownloadUrl(
            deviceId = deviceId,
            updateMethodId = methodId,
            appVersion = BuildConfig.VERSION_NAME,
            body = FreshUpdateDataDownloadUrlBody(
                id = updateData.id,
                otaVersionNumber = updateData.otaVersionNumber,
                downloadUrl = updateData.downloadUrl,
                md5sum = updateData.md5sum,
            )
        )
    }.let {
        if (it == null || !it.success) return@let
        val result = it.result ?: return@let
        updateDataDao.updateDownloadUrl(updateData.id ?: return@let, result)
    }

    suspend fun fetchServerStatus() = performServerRequest { serverApi.fetchServerStatus() }.let { status ->
        val automaticInstallationEnabled = false
        val pushNotificationsDelaySeconds = sharedPreferences[KeyNotificationDelayInSeconds, 10]

        val response = if (status == null && OxygenUpdater.isNetworkAvailable.value) ServerStatus(
            ServerStatus.Status.UNREACHABLE,
            BuildConfig.VERSION_NAME,
            automaticInstallationEnabled,
            pushNotificationsDelaySeconds
        ) else status ?: ServerStatus(
            ServerStatus.Status.NORMAL,
            BuildConfig.VERSION_NAME,
            automaticInstallationEnabled,
            pushNotificationsDelaySeconds
        )

        sharedPreferences[KeyNotificationDelayInSeconds] = response.pushNotificationDelaySeconds

        response
    }

    suspend fun fetchServerMessages() = performServerRequest {
        serverApi.fetchServerMessages(
            deviceId = sharedPreferences[KeyDeviceId, NotSetL],
            updateMethodId = sharedPreferences[KeyUpdateMethodId, NotSetL],
        )
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun fetchNewsFromDb() = articleDao.getAll()

    @Suppress("RedundantSuspendModifier")
    suspend fun markAllReadLocally() = articleDao.markAllRead()

    suspend fun fetchNews() = performServerRequest {
        serverApi.fetchNews(
            deviceId = sharedPreferences[KeyDeviceId, NotSetL],
            updateMethodId = sharedPreferences[KeyUpdateMethodId, NotSetL],
        )
    }.let {
        if (!it.isNullOrEmpty()) articleDao.refreshArticles(it)

        // Note: we're returning a local copy so that read statuses are respected
        fetchNewsFromDb()
    }

    suspend fun fetchArticle(id: Long) = performServerRequest {
        serverApi.fetchArticle(id)
    }.let {
        if (it != null) articleDao.insertOrUpdate(it)

        articleDao.getById(id)
    }

    suspend fun toggleArticleReadLocally(
        article: Article,
        read: Boolean = !article.readState,
    ) = articleDao.toggleRead(article, read)

    suspend fun markArticleRead(id: Long) = performServerRequest {
        serverApi.markArticleRead(ArrayMap<String, Long>(1).apply { put("news_item_id", id) })
    }?.also {
        if (!it.success) Log.w(TAG, "Failed to mark article as read on the server: ${it.errorMessage}")
    }

    suspend fun fetchUpdateMethodsForDevice(deviceId: Long) = performServerRequest {
        serverApi.fetchUpdateMethodsForDevice(deviceId)
    }

    suspend fun logDownloadError(
        url: String?,
        filename: String?,
        version: String?,
        otaVersion: String?,
        httpCode: Int,
        httpMessage: String?,
    ) = performServerRequest {
        serverApi.logDownloadError(
            DownloadErrorBody(
                url = url,
                filename = filename,
                version = version,
                otaVersion = otaVersion,
                httpCode = httpCode,
                httpMessage = httpMessage,
                appVersion = BuildConfig.VERSION_NAME,
                deviceName = sharedPreferences[KeyDevice, "<UNKNOWN>"],
                actualDeviceName = SystemVersionProperties.deviceProductName,
            )
        )
    }

    @Suppress("RedundantSuspendModifier")
    private suspend inline fun <R> performServerRequest(block: () -> Response<R>) = try {
        val response = block()
        if (response.isSuccessful) response.body().apply {
            logVerbose(TAG, "Response: $this")
        } else null
    } catch (e: Exception) {
        // Don't log cancellations, we don't care
        if (e is CancellationException) logInfo(TAG, e.message ?: "CancellationException")
        else Log.e(TAG, "Error performing request", e)
        null
    }

    companion object {
        private const val TAG = "ServerRepository"
    }
}
