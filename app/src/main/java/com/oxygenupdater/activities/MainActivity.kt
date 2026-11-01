package com.oxygenupdater.activities

import android.util.Log
import android.content.Intent
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.IntIntPair
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeRuntimeFlags
import androidx.compose.runtime.Composer
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.R
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.extensions.showToast
import com.oxygenupdater.icons.ImageFilled
import com.oxygenupdater.icons.LogoNotification
import com.oxygenupdater.icons.Logos
import com.oxygenupdater.icons.Symbols
import com.oxygenupdater.internal.NotSet
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.KeyAdvancedMode
import com.oxygenupdater.internal.settings.KeyIgnoreIncorrectDeviceWarnings
import com.oxygenupdater.internal.settings.KeyIgnoreNotificationPermissionSheet
import com.oxygenupdater.internal.settings.KeyIgnoreUnsupportedDeviceWarnings
import com.oxygenupdater.internal.settings.KeySetupDone
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.ui.CollapsingAppBar
import com.oxygenupdater.ui.TopAppBar
import com.oxygenupdater.ui.about.AboutScreen
import com.oxygenupdater.ui.common.rememberState
import com.oxygenupdater.ui.device.DefaultDeviceName
import com.oxygenupdater.ui.device.DeviceScreen
import com.oxygenupdater.ui.device.DeviceViewModel
import com.oxygenupdater.ui.device.IncorrectDeviceDialog
import com.oxygenupdater.ui.device.UnsupportedDeviceOsSpecDialog
import com.oxygenupdater.ui.faq.FaqScreen
import com.oxygenupdater.ui.faq.FaqViewModel
import com.oxygenupdater.ui.install.InstallGuideScreen
import com.oxygenupdater.ui.install.InstallGuideViewModel
import com.oxygenupdater.ui.main.AppUpdateInfo
import com.oxygenupdater.ui.main.ArticleRoute
import com.oxygenupdater.ui.main.ChildRoute
import com.oxygenupdater.ui.main.DeepLinkAbout
import com.oxygenupdater.ui.main.DeepLinkArticle
import com.oxygenupdater.ui.main.DeepLinkDevice
import com.oxygenupdater.ui.main.DeepLinkFaq
import com.oxygenupdater.ui.main.DeepLinkGuide
import com.oxygenupdater.ui.main.DeepLinkNewsList
import com.oxygenupdater.ui.main.DeepLinkSettings
import com.oxygenupdater.ui.main.DeepLinkUpdate
import com.oxygenupdater.ui.main.DownloadedArg
import com.oxygenupdater.ui.main.ExternalArg
import com.oxygenupdater.ui.main.FaqRoute
import com.oxygenupdater.ui.main.FlexibleAppUpdateProgress
import com.oxygenupdater.ui.main.GuideRoute
import com.oxygenupdater.ui.main.MainMenu
import com.oxygenupdater.ui.main.MainNavigationBar
import com.oxygenupdater.ui.main.MainNavigationRail
import com.oxygenupdater.ui.main.MainRoute
import com.oxygenupdater.ui.main.MainSnackbar
import com.oxygenupdater.ui.main.NavType
import com.oxygenupdater.ui.main.NoConnectionSnackbarData
import com.oxygenupdater.ui.main.NotificationPermission
import com.oxygenupdater.ui.main.OuScheme
import com.oxygenupdater.ui.main.Route
import com.oxygenupdater.ui.main.ServerStatusBanner
import com.oxygenupdater.ui.main.ServerStatusDialogs
import com.oxygenupdater.ui.main.directionalScalePredictivePopTransitionSpec
import com.oxygenupdater.ui.main.scaleChildTransitionSpec
import com.oxygenupdater.ui.main.scalePopTransitionSpec
import com.oxygenupdater.ui.news.ArticleScreen
import com.oxygenupdater.ui.news.ArticleViewModel
import com.oxygenupdater.ui.news.NewsListScreen
import com.oxygenupdater.ui.news.NewsListViewModel
import com.oxygenupdater.ui.onboarding.OnboardingScreen
import com.oxygenupdater.ui.settings.SettingsScreen
import com.oxygenupdater.ui.settings.SettingsViewModel
import com.oxygenupdater.ui.theme.AppTheme
import com.oxygenupdater.ui.theme.light
import com.oxygenupdater.ui.update.KeyDownloadErrorMessage
import com.oxygenupdater.ui.update.UpdateInformationViewModel
import com.oxygenupdater.ui.update.UpdateScreen
import com.oxygenupdater.ui.update.WorkProgress
import com.oxygenupdater.utils.LocalNotifications
import com.oxygenupdater.utils.UpdateCheckScheduler
import com.oxygenupdater.utils.Utils.checkPlayServices
import com.oxygenupdater.utils.logDebug
import com.oxygenupdater.viewmodels.MainViewModel
import com.oxygenupdater.workers.WorkDataDownloadBytesDone
import com.oxygenupdater.workers.WorkDataDownloadEta
import com.oxygenupdater.workers.WorkDataDownloadFailureExtraHttpCode
import com.oxygenupdater.workers.WorkDataDownloadFailureExtraHttpMessage
import com.oxygenupdater.workers.WorkDataDownloadFailureType
import com.oxygenupdater.workers.WorkDataDownloadProgress
import com.oxygenupdater.workers.WorkDataDownloadTotalBytes
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single activity.
 *
 * We're using [AppCompatActivity] instead of [androidx.activity.ComponentActivity] because of
 * [automatic per-app language](https://developer.android.com/guide/topics/resources/app-languages#androidx-impl)
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val updateViewModel: UpdateInformationViewModel by viewModels()
    private val newsListViewModel: NewsListViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    @Volatile
    private var currentRoute: Route? = null

    @Inject
    lateinit var updateCheckScheduler: UpdateCheckScheduler

    private var downloadErrorMessage: String? = null

    private lateinit var startRoute: MainRoute
    private lateinit var backStack: SnapshotStateList<Route>

    /**
     * Passthrough from [MainViewModel] to avoid sending a request again, but only after server request for
     * all devices completes ([MainViewModel] sends it on init, which would be `null` initially).
     *
     * This additional check is required only during onboarding to avoid unnecessary requests being sent.
     * We don't want to send a server request for enabled devices simultaneously, because that can be derived
     * from all devices anyway. Additionally, unnecessary changes in device lists will cause a repeated request
     * for update methods to also be sent.
     */
    @Composable
    private inline fun PassthroughEnabledDevicesToSettingsViewModel(enabledDevices: List<Device>?) {
        if (enabledDevices == null) return
        SideEffect(enabledDevices) {
            settingsViewModel.fetchEnabledDevices(enabledDevices)
        }
    }

    @Composable
    @ReadOnlyComposable
    private fun EdgeToEdge() {
        val light = MaterialTheme.colorScheme.light
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { !light },
            navigationBarStyle = navigationBarStyle,
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun OnboardingContent(windowSize: WindowSizeClass) = Column {
        val allDevices by viewModel.allDevicesState.collectAsStateWithLifecycle()
        val cachedEnabledDevices = remember(allDevices) { allDevices?.filter { it.enabled } }
        PassthroughEnabledDevicesToSettingsViewModel(cachedEnabledDevices)

        val deviceName = remember(cachedEnabledDevices) {
            cachedEnabledDevices?.find {
                it.productNames.contains(SystemVersionProperties.deviceProductName)
            }?.name
        }

        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        // Use normal app bar if height isn't enough (e.g. landscape phones)
        if (windowSize.heightSizeClass == WindowHeightSizeClass.Compact) TopAppBar(
            scrollBehavior = scrollBehavior,
            subtitleResId = R.string.onboarding,
            root = true,
        ) else CollapsingAppBar(
            scrollBehavior = scrollBehavior,
            image = { modifier ->
                val context = LocalContext.current
                AsyncImage(
                    model = deviceName?.let {
                        val density = LocalDensity.current
                        remember(it, maxWidth) {
                            ImageRequest.Builder(context)
                                .data(Device.constructImageUrl(it))
                                .size(density.run { Size(maxWidth.roundToPx(), 256.dp.roundToPx()) })
                                .build()
                        }
                    },
                    contentDescription = stringResource(R.string.device_information_image_description),
                    placeholder = rememberVectorPainter(Symbols.ImageFilled),
                    error = rememberVectorPainter(Logos.LogoNotification),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (deviceName == null) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null,
                    modifier = modifier
                )
            },
            title = stringResource(R.string.app_name),
            subtitle = stringResource(R.string.onboarding),
        )

        val deviceConfig by settingsViewModel.deviceConfigState.collectAsStateWithLifecycle()
        val methodConfig by settingsViewModel.methodConfigState.collectAsStateWithLifecycle()
        val deviceListLoadFailed by viewModel.deviceFetchFailedState.collectAsStateWithLifecycle()
        OnboardingScreen(
            windowWidthSize = windowSize.widthSizeClass,
            scrollBehavior = scrollBehavior,
            deviceConfig = deviceConfig,
            onDeviceSelect = settingsViewModel::saveSelectedDevice,
            methodConfig = methodConfig,
            onMethodSelect = settingsViewModel::saveSelectedMethod,
            deviceListLoadFailed = deviceListLoadFailed,
            onRetryLoadDevices = { viewModel.fetchAllDevices() },
            getPrefStr = viewModel::getPref,
            getPrefBool = viewModel::getPref,
            persistBool = viewModel::persist,
            onStartAppClick = {
                settingsViewModel.resubscribeToFcmTopic()

                if (viewModel.isDeviceAndMethodSet) {
                    viewModel.persist(KeySetupDone, true)
                    viewModel.shouldShowOnboarding = false
                } else {
                    Log.w(TAG, "Required preferences not valid: ${viewModel.deviceId}, ${viewModel.updateMethodId}")
                    showToast(R.string.settings_entered_incorrectly)
                    viewModel.shouldShowOnboarding = true
                }
            },
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Content(windowSize: WindowSizeClass) {
        SideEffect(Unit, effect = viewModel::fetchServerMessages) // run only on init

        val showDeviceBadge = viewModel.deviceOsSpec.let {
            it != null && it != DeviceOsSpec.SupportedDeviceAndOs
        } || viewModel.deviceMismatch.let { it != null && it.first }
        MainRoute.Device.badge = if (showDeviceBadge) "!" else null

        val allDevices by viewModel.allDevicesState.collectAsStateWithLifecycle()
        val enabledDevices = remember(allDevices) { allDevices?.filter { it.enabled } }

        viewModel.deviceOsSpec?.let {
            /**
             * We're maintaining `show` outside of [UnsupportedDeviceOsSpecDialog]
             * to allow re-opening after hiding it the first time.
             */
            var show by rememberState(viewModel.shouldShowUnsupportedDeviceDialog)
            UnsupportedDeviceOsSpecDialog(
                show = show,
                hide = { ignore ->
                    viewModel.persist(KeyIgnoreUnsupportedDeviceWarnings, ignore)
                    show = false
                },
                spec = it,
            )
        }

        if (viewModel.shouldShowIncorrectDeviceDialog) viewModel.deviceMismatch?.let { mismatchStatus ->
            IncorrectDeviceDialog(
                hide = { viewModel.persist(KeyIgnoreIncorrectDeviceWarnings, it) },
                mismatchStatus = mismatchStatus,
            )
        }

        // Referential equality because we're reusing static Pairs
        var snackbarText by rememberState<IntIntPair?>(null, referentialEqualityPolicy())

        val appUpdateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()
        appUpdateInfo?.let { it ->
            AppUpdateInfo(
                status = it.installStatus(),
                availability = it::updateAvailability,
                snackbarMessageId = { snackbarText?.first },
                updateSnackbarText = { snackbarText = it },
                resetAppUpdateIgnoreCount = viewModel::resetAppUpdateIgnoreCount,
                incrementAppUpdateIgnoreCount = viewModel::incrementAppUpdateIgnoreCount,
                unregisterAppUpdateListener = viewModel::unregisterAppUpdateListener,
                requestUpdate = viewModel::requestUpdate,
                requestImmediateUpdate = viewModel::requestImmediateAppUpdate,
            )
        }

        // Display the "No connection" banner if required
        val isNetworkAvailable by OxygenUpdater.isNetworkAvailable.collectAsStateWithLifecycle()
        if (!isNetworkAvailable) snackbarText = NoConnectionSnackbarData
        else if (snackbarText?.first == NoConnectionSnackbarData.first) {
            // Dismiss only this snackbar
            snackbarText = null
        }

        Row {
            val windowWidthSize = windowSize.widthSizeClass
            val navType = NavType.from(windowWidthSize)
            val isNavTypeBottomBar = navType == NavType.BottomBar

            currentRoute = backStack.lastOrNull() // declare/assign before use
            // Main => top-level destination
            val isMainScreen = currentRoute?.let { it is MainRoute } == true

            var subtitleResId by rememberState(currentRoute, (currentRoute ?: startRoute).let {
                if (it is MainRoute && it.useVersionName) 0 else it.labelResId
            })

            val onNavIconClick = dropUnlessResumed {
                if (isMainScreen) backStack.navigate(MainRoute.About)
                // Nav icon to go back is only a behaviour in child routes
                else backStack.removeLastOrNull().let {}
            }

            AnimatedVisibility(!isNavTypeBottomBar) {
                MainNavigationRail(
                    currentRoute = currentRoute,
                    onNavIconClick = onNavIconClick,
                    root = isMainScreen,
                    navigateTo = { backStack.navigate(it) },
                )
            }

            AnimatedVisibility(!isNavTypeBottomBar) {
                VerticalDivider()
            }

            Column {
                val isArticleScreen = currentRoute?.let { it is ArticleRoute } == true
                val scrollBehavior = if (isArticleScreen) {
                    TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                } else TopAppBarDefaults.enterAlwaysScrollBehavior()

                AnimatedContent(
                    targetState = isArticleScreen,
                    label = "ArticleCollapsingAppBarAnimatedContent",
                ) { showCollapsingAppBar ->
                    if (showCollapsingAppBar) CollapsingAppBar(
                        scrollBehavior = scrollBehavior,
                        image = { modifier ->
                            val context = LocalContext.current
                            val imageUrl = ArticleViewModel.item?.imageUrl
                            AsyncImage(
                                model = imageUrl?.let {
                                    val density = LocalDensity.current
                                    remember(it, maxWidth) {
                                        ImageRequest.Builder(context)
                                            .data(it)
                                            .size(density.run { Size(maxWidth.roundToPx(), 256.dp.roundToPx()) })
                                            .build()
                                    }
                                },
                                contentDescription = stringResource(R.string.news),
                                placeholder = rememberVectorPainter(Symbols.ImageFilled),
                                error = rememberVectorPainter(Logos.LogoNotification),
                                contentScale = ContentScale.Crop,
                                colorFilter = if (imageUrl == null) {
                                    ColorFilter.tint(MaterialTheme.colorScheme.primary)
                                } else null,
                                modifier = modifier
                            )
                        },
                        title = ArticleViewModel.item?.title ?: stringResource(R.string.loading),
                        subtitle = ArticleViewModel.item?.authorName ?: stringResource(R.string.summary_please_wait),
                        // Don't show nav icon if SideRail is shown
                        onNavIconClick = if (isNavTypeBottomBar) onNavIconClick else null,
                    ) else TopAppBar(
                        scrollBehavior = scrollBehavior,
                        // Don't show nav icon if SideRail is shown
                        onNavIconClick = if (isNavTypeBottomBar) onNavIconClick else null,
                        subtitleResId = subtitleResId,
                        root = isMainScreen,
                    ) {
                        val serverMessages by viewModel.serverMessages.collectAsStateWithLifecycle()
                        MainMenu(
                            serverMessages = serverMessages,
                            showMarkAllRead = currentRoute === MainRoute.NewsList,
                            onMarkAllReadClick = newsListViewModel::markAllRead,
                        )
                    }
                }

                val appUpdateStatus by viewModel.appUpdateStatus.collectAsStateWithLifecycle()
                appUpdateStatus?.let { it ->
                    FlexibleAppUpdateProgress(
                        status = it.installStatus(),
                        bytesDownloaded = it::bytesDownloaded,
                        totalBytesToDownload = it::totalBytesToDownload,
                        updateSnackbarText = { snackbarText = it },
                        unregisterAppUpdateListener = viewModel::unregisterAppUpdateListener,
                    )
                }

                val serverStatus by viewModel.serverStatus.collectAsStateWithLifecycle()
                ServerStatusDialogs(serverStatus.status, ::openPlayStorePage)
                ServerStatusBanner(serverStatus, ::openPlayStorePage)

                NavDisplay(
                    backStack = backStack,
                    onBack = dropUnlessResumed {
                        if (backStack.shouldStopNavigateAwayFromSettings) showSettingsWarning()
                        else backStack.removeLastOrNull() // default behaviour
                    },
                    popTransitionSpec = remember(calculation = ::scalePopTransitionSpec),
                    predictivePopTransitionSpec = remember(calculation = ::directionalScalePredictivePopTransitionSpec),
                    entryProvider = entryProvider {
                        updateScreen(
                            navType = navType,
                            windowWidthSize = windowWidthSize,
                            setSubtitleResId = { subtitleResId = it },
                            openInstallGuide = dropUnlessResumed {
                                backStack.navigate(GuideRoute(true))
                            }
                        )

                        val currentLifecycle = LocalLifecycleOwner.current.lifecycle
                        newsListScreen(
                            navType = navType,
                            windowSize = windowSize,
                        ) {
                            // Manage `dropUnlessResumed` ourselves, because we have an argument to pass
                            if (currentLifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                backStack.navigate(ArticleRoute(it))
                            }
                        }

                        deviceScreen(
                            navType = navType,
                            windowWidthSize = windowWidthSize,
                            allDevices = allDevices,
                        )

                        aboutScreen(
                            navType = navType,
                            windowWidthSize = windowWidthSize,
                        ) {
                            // Manage `dropUnlessResumed` ourselves, because we have an argument to pass
                            if (currentLifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                backStack.navigate(it)
                            }
                        }

                        settingsScreen(
                            navType = navType,
                            cachedEnabledDevices = enabledDevices,
                            updateMismatchStatus = viewModel::updateDeviceMismatch,
                            openAboutScreen = dropUnlessResumed { backStack.navigate(MainRoute.About) },
                        )

                        articleScreen(scrollBehavior = scrollBehavior)
                        installGuideScreen()
                        faqScreen()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                )

                val showNavBar = isNavTypeBottomBar && isMainScreen

                AnimatedVisibility(showNavBar) {
                    MainNavigationBar(
                        currentRoute = currentRoute,
                        navigateTo = { backStack.navigate(it) },
                    )
                }
            }
        }

        // Gets placed over TopAppBar
        MainSnackbar(
            snackbarText = snackbarText,
            openPlayStorePage = ::openPlayStorePage,
            completeAppUpdate = viewModel::completeAppUpdate,
        )
    }

    /**
     * @param metadata allows overriding transition specs per route
     */
    private inline fun <reified T : Route> EntryProviderScope<Route>.surfacedEntry(
        metadata: Map<String, Any> = emptyMap(),
        noinline content: @Composable (T) -> Unit,
    ) = entry<T>(metadata = metadata) {
        // Each screen needs a background of its own; otherwise, when 2 screens
        // are shown together (during predictive back), there's no clear visual
        // separation of foreground elements.
        Surface { content(it) }
    }

    private fun EntryProviderScope<Route>.updateScreen(
        navType: NavType,
        windowWidthSize: WindowWidthSizeClass,
        setSubtitleResId: (Int) -> Unit,
        openInstallGuide: () -> Unit,
    ) = surfacedEntry<MainRoute.Update> {
        DisposableEffect(Unit) {
            // Run this every time this screen is visited: always a real (no-cache)
            // fetch, so reloading the screen always reloads from the server
            updateViewModel.refresh()

            // Prune finished work when leaving this composable, e.g. when
            // switching to another screen or opening a new activity
            onDispose(viewModel::maybePruneWork)
        }

        val state = updateViewModel.state.collectAsStateWithLifecycle(null).value ?: return@surfacedEntry
        val (workInfo, downloadStatus) = viewModel.workInfoWithStatus.collectAsStateWithLifecycle().value
        val rolloutSnapshotsJson by viewModel.rolloutSnapshotsJson.collectAsStateWithLifecycle()

        // Pick up snapshots written by the background check while the app was closed
        DisposableEffect(Unit) {
            viewModel.refreshRolloutSnapshots()
            onDispose {}
        }

        val outputData = workInfo?.outputData
        val progress = workInfo?.progress
        val failureType = outputData?.getInt(WorkDataDownloadFailureType, NotSet) ?: NotSet
        UpdateScreen(
            navType = navType,
            windowWidthSize = windowWidthSize,
            state = state,
            onRefresh = {
                viewModel.fetchServerStatus()
                updateViewModel.refresh()
            },
            _downloadStatus = downloadStatus,
            failureType = failureType,
            httpFailureCodeAndMessage = {
                outputData?.let {
                    val code = it.getInt(WorkDataDownloadFailureExtraHttpCode, NotSet)
                    val message = it.getString(WorkDataDownloadFailureExtraHttpMessage)
                    if (code != NotSet && !message.isNullOrBlank()) code to message else null
                }
            },
            workProgress = if (progress == null) null else remember(progress) {
                WorkProgress(
                    bytesDone = progress.getLong(WorkDataDownloadBytesDone, NotSetL),
                    totalBytes = progress.getLong(WorkDataDownloadTotalBytes, NotSetL),
                    currentProgress = progress.getInt(WorkDataDownloadProgress, NotSet),
                    downloadEta = progress.getString(WorkDataDownloadEta),
                )
            },
            forceDownloadErrorDialog = downloadErrorMessage != null,
            getPrefStr = viewModel::getPref,
            getPrefBool = viewModel::getPref,
            rolloutSnapshotsJson = rolloutSnapshotsJson,
            setSubtitleResId = setSubtitleResId,
            enqueueDownload = viewModel::enqueueDownloadWork,
            pauseDownload = viewModel::pauseDownloadWork,
            cancelDownload = { viewModel.cancelDownloadWork(this@MainActivity, it) },
            deleteDownload = { viewModel.deleteDownloadedFile(this@MainActivity, it) },
            openInstallGuide = openInstallGuide,
            logDownloadError = { if (outputData != null) viewModel.logDownloadError(outputData) },
            hideDownloadCompleteNotification = {
                LocalNotifications.hideDownloadCompleteNotification(this@MainActivity)
            },
            showDownloadFailedNotification = {
                LocalNotifications.showDownloadFailedNotification(
                    context = this@MainActivity,
                    resumable = false,
                    message = R.string.download_error_storage,
                    notificationMessage = R.string.download_notification_error_storage_full,
                )
            },
        )
    }

    private fun EntryProviderScope<Route>.newsListScreen(
        navType: NavType,
        windowSize: WindowSizeClass,
        openArticle: (Long) -> Unit,
    ) = surfacedEntry<MainRoute.NewsList> {
        val state by newsListViewModel.state.collectAsStateWithLifecycle()
        NewsListScreen(
            navType = navType,
            windowWidthSize = windowSize.widthSizeClass,
            windowHeightSize = windowSize.heightSizeClass,
            state = state,
            onRefresh = {
                viewModel.fetchServerStatus()
                newsListViewModel.refresh()
            },
            unreadCountState = newsListViewModel.unreadCount,
            onMarkAllReadClick = newsListViewModel::markAllRead,
            onToggleReadClick = newsListViewModel::toggleReadLocally,
            openItem = {
                // TODO(compose/news): handle shared element transition, see
                //  https://developer.android.com/develop/ui/compose/animation/shared-elements
                openArticle(it)
            },
        )
    }

    private fun EntryProviderScope<Route>.deviceScreen(
        navType: NavType,
        windowWidthSize: WindowWidthSizeClass,
        allDevices: List<Device>?,
    ) = surfacedEntry<MainRoute.Device> {
        val cpuFrequencies by hiltViewModel<DeviceViewModel>(this@MainActivity)
            .cpuFrequencies.collectAsStateWithLifecycle()

        DeviceScreen(
            navType = navType,
            windowWidthSize = windowWidthSize,
            deviceName = remember(allDevices) {
                allDevices?.find {
                    it.productNames.contains(SystemVersionProperties.deviceProductName)
                }?.name ?: DefaultDeviceName
            },
            deviceOsSpec = viewModel.deviceOsSpec,
            deviceMismatchStatus = viewModel.deviceMismatch,
            cpuFrequencies = cpuFrequencies,
        )
    }

    private fun EntryProviderScope<Route>.aboutScreen(
        navType: NavType,
        windowWidthSize: WindowWidthSizeClass,
        showChildScreen: (ChildRoute) -> Unit,
    ) = surfacedEntry<MainRoute.About> {
        AboutScreen(
            navType = navType,
            windowWidthSize = windowWidthSize,
            navigateTo = showChildScreen,
            openEmail = { viewModel.openEmail(this@MainActivity) }
        )
    }

    private fun EntryProviderScope<Route>.settingsScreen(
        navType: NavType,
        cachedEnabledDevices: List<Device>?,
        updateMismatchStatus: () -> Unit,
        openAboutScreen: () -> Unit,
    ) = surfacedEntry<MainRoute.Settings> {
        PassthroughEnabledDevicesToSettingsViewModel(cachedEnabledDevices)

        val deviceConfig by settingsViewModel.deviceConfigState.collectAsStateWithLifecycle()
        val methodConfig by settingsViewModel.methodConfigState.collectAsStateWithLifecycle()
        val deviceListLoadFailed by viewModel.deviceFetchFailedState.collectAsStateWithLifecycle()
        SettingsScreen(
            navType = navType,
            deviceConfig = deviceConfig,
            onDeviceSelect = {
                settingsViewModel.saveSelectedDevice(it)
                updateMismatchStatus()
            },
            methodConfig = methodConfig,
            onMethodSelect = {
                settingsViewModel.saveSelectedMethod(it)
                settingsViewModel.resubscribeToFcmTopic()

                if (!checkPlayServices(this@MainActivity, true)) {
                    showToast(R.string.notification_no_notification_support)
                }
            },
            onThemeSelect = settingsViewModel::updateTheme,
            onAdvancedModeChange = { viewModel.persist(KeyAdvancedMode, it) },
            onUpdateCheckIntervalChange = { updateCheckScheduler.schedule(this@MainActivity, it) },
            deviceListLoadFailed = deviceListLoadFailed,
            onRetryLoadDevices = { viewModel.fetchAllDevices() },
            openAboutScreen = openAboutScreen,
            getPrefStr = viewModel::getPref,
            getPrefBool = viewModel::getPref,
            getPrefLong = viewModel::getPref,
            persistBool = viewModel::persist,
            persistLong = viewModel::persist,
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun EntryProviderScope<Route>.articleScreen(
        scrollBehavior: TopAppBarScrollBehavior,
    ) = surfacedEntry<ArticleRoute> { // no transition override, looks worse than default
        val id = it.id
        if (id == NotSetL) return@surfacedEntry

        ArticleScreen(
            viewModel = hiltViewModel<ArticleViewModel>(this@MainActivity),
            id = id,
            scrollBehavior = scrollBehavior,
            onLoadFinished = newsListViewModel::markRead,
        )
    }

    private fun EntryProviderScope<Route>.installGuideScreen() = surfacedEntry<GuideRoute>(
        metadata = NavDisplay.transitionSpec(scaleChildTransitionSpec()),
    ) {
        InstallGuideScreen(
            viewModel = hiltViewModel<InstallGuideViewModel>(this@MainActivity),
            downloaded = it.downloaded,
        )
    }

    private fun EntryProviderScope<Route>.faqScreen() = surfacedEntry<FaqRoute>(
        metadata = NavDisplay.transitionSpec(scaleChildTransitionSpec()),
    ) {
        FaqScreen(
            viewModel = hiltViewModel<FaqViewModel>(this@MainActivity),
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (viewModel.isDeviceAndMethodSet) {
            settingsViewModel.resubscribeToFcmTopic()
            // (Re)schedule the periodic background update check and run one now
            updateCheckScheduler.schedule(this)
            updateCheckScheduler.checkNow(this)
        }
        if (!checkPlayServices(this, false)) {
            showToast(R.string.notification_no_notification_support)
        }

        /** Do this as early as possible because it's used in [onNewIntent] */
        var deepLink: Route? = null
        // Action will be set if opened from a shortcut. Start route will never change once set.
        startRoute = when (intent?.action) {
            ActionPageUpdate -> MainRoute.Update
            ActionPageNews -> MainRoute.NewsList
            ActionPageDevice -> MainRoute.Device
            else -> {
                val isArticleDeepLink = getDeepLinkFromIntent(intent)?.let {
                    deepLink = it
                    it is ArticleRoute
                } == true
                if (isArticleDeepLink) MainRoute.NewsList else MainRoute.Update
            }
        }
        backStack = mutableStateListOf(startRoute)
        // Add deep link over the starting/base route
        deepLink?.let { backStack.addIfNot(it) }

        // Force-show download error dialog if started from an intent with error info
        // TODO(compose/update): download error dialog logic is simplified in UpdateScreen. Verify if the dialog meant
        //  to be shown after clicking the download failed notification (i.e. on activity start) is shown
        //  immediately in the UI itself, in both cases: app in foreground and background/killed.
        //  Otherwise, see KEY_DOWNLOAD_ERROR_MESSAGE in UIF.setupServerResponseObservers()
        downloadErrorMessage = try {
            intent?.getStringExtra(KeyDownloadErrorMessage)
        } catch (_: IndexOutOfBoundsException) {
            null
        }

        // RELEASE builds rely on configuration via proguard
        @OptIn(ExperimentalComposeApi::class)
        if (BuildConfig.DEBUG) ComposeRuntimeFlags.isLinkBufferComposerEnabled = true

        // https://kotlinlang.org/docs/whatsnew23.html#compose-compiler-stack-traces-for-minified-android-applications
        Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.GroupKeys)
        setContent {
            val windowSize = calculateWindowSizeClass(this)

            AppTheme(settingsViewModel.theme) {
                EdgeToEdge()

                // We're using Surface to avoid Scaffold's recomposition-on-scroll issue (when using scrollBehaviour and consuming innerPadding)
                Surface(Modifier.semantics {
                    @OptIn(ExperimentalComposeUiApi::class)
                    testTagsAsResourceId = true
                }) {
                    Crossfade(
                        targetState = viewModel.shouldShowOnboarding,
                        label = "OnboardingMainCrossfade",
                    ) { shouldShowOnboarding ->
                        if (shouldShowOnboarding) OnboardingContent(windowSize) else {
                            if (viewModel.isOnboardingComplete) viewModel.shouldShowOnboarding = false else {
                                viewModel.shouldShowOnboarding = true
                                return@Crossfade // we shouldn't be here; exit
                            }

                            if (SDK_INT >= VERSION_CODES.TIRAMISU) NotificationPermission(
                                canShow = viewModel.canShowNotifPermissionSheet,
                                hide = { viewModel.persist(KeyIgnoreNotificationPermissionSheet, it) }
                            )

                            Content(windowSize)
                        }
                    }
                }
            }
        }

    }

    /** Required because we use `android:launchMode="singleTask"` in AndroidManifest.xml */
    override fun onNewIntent(intent: Intent) = super.onNewIntent(intent).also {
        // We always add to the back stack if we receive an intent while the app was already open
        getDeepLinkFromIntent(intent)?.let { backStack.addIfNot(it) }
    }

    override fun onResume() = super.onResume().also {
        viewModel.checkForStalledAppUpdate()
    }

    override fun onDestroy() = super.onDestroy().also {
        viewModel.maybePruneWork()
        viewModel.unregisterAppUpdateListener()
    }

    /**
     * Navigates to the specified [route] (either a [ChildRoute] or [MainRoute], i.e.
     * top-level destination). The following constraints are respected:
     * - Child routes are always added over main routes, and vice versa
     * - Exiting [MainRoute.Settings] isn't allowed until important settings are saved
     * - Main routes replace each other unless it's the [startRoute]. We preserve that
     *   because the user may consider it the 'homescreen' of the app, which means they
     *   might expect to return back to that before exiting the app.
     *
     * **Note:** should be protected with [dropUnlessResumed] or similar.
     */
    private fun SnapshotStateList<Route>.navigate(route: Route) {
        // Child routes should always be added over main routes
        if (route is ChildRoute) addIfNot(route)
        // Child routes can never be the Settings screen
        else if (shouldStopNavigateAwayFromSettings) showSettingsWarning()
        // If user goes back to the starting route, we reset the whole stack
        else if (route === startRoute) {
            if (firstOrNull() === startRoute) { // double-check
                val size = size
                if (size > 1) removeRange(1, size) // remove others
            } else {
                // Fallback to recreating the stack
                clear()
                add(route)
            }
        } else {
            val lastIndex = lastIndex
            if (lastIndex < 0) {
                // Somehow if we don't have anything in the stack, make corrections
                add(startRoute) // always maintain the starting route
                addIfNot(route) // then add the new route if needed
                return // nothing more to do
            }

            // Now it's guaranteed that IndexOutOfBoundsException won't be thrown
            val lastRoute = this[lastIndex]
            // Add on top of the stack if the previous route was a child route,
            // or if the only route on the stack is what we started with.
            if (lastRoute is ChildRoute || (lastIndex == 0 && lastRoute === startRoute)) addIfNot(route)
            // Otherwise, top-level destinations should be switched to directly
            else set(lastIndex, route)
        }
    }

    /** Only add if we're not already on the same screen */
    private inline fun SnapshotStateList<Route>.addIfNot(route: Route) {
        if (lastOrNull() != route) add(route)
    }

    /** @return true if we're on [MainRoute.Settings] and required prefs aren't saved */
    private inline val SnapshotStateList<Route>.shouldStopNavigateAwayFromSettings
        get() = lastOrNull() == MainRoute.Settings && !viewModel.isDeviceAndMethodSet

    private fun showSettingsWarning() {
        val deviceId = viewModel.deviceId
        val updateMethodId = viewModel.updateMethodId

        if (deviceId == NotSetL || updateMethodId == NotSetL) {
            Log.w(TAG, "Required preferences not valid: $deviceId, $updateMethodId")
            showToast(R.string.settings_entered_incorrectly)
        } else showToast(R.string.settings_saving)
    }

    /**
     * Used in [onNewIntent] and also in [onCreate] for cold launches.
     *
     * @param intent the [Intent], whose [data][Intent.getData] we check
     *
     * @return the resolved [ChildRoute] for the deep link. Meant to be added to [backStack].
     */
    private fun getDeepLinkFromIntent(intent: Intent?): Route? {
        val uri = intent?.data?.normalizeScheme() ?: return null
        val host = uri.host
        if (host.isNullOrEmpty()) return null

        return when (uri.scheme ?: return null) {
            // Compare host without the `https://` at the beginning.
            // If it's somehow not our domain, return early.
            "http", "https" -> {
                if ("$host/" != BuildConfig.SERVER_DOMAIN.substring(8)) return null

                val segments = uri.pathSegments
                if (segments.isNullOrEmpty()) return null

                // Compare path segments:
                // - First one must be either "api" or "article"
                // - if "api", the consecutive segments must be "news-content" followed by a number
                // - if "article", the next segment must be a number
                val type = segments.getOrNull(0)
                if (type.isNullOrEmpty()) return null

                when (type) {
                    // https://oxygenupdater.com/api/<version>/news-content/<id>/<lang>/<theme>
                    "api" -> {
                        // Other API paths aren't deep links into the app
                        if (segments.getOrNull(2) != "news-content") return null

                        val id = segments.getOrNull(3)?.toLongOrNull() ?: return null
                        // Web article links are always 'external'
                        if (id <= 0L) null else ArticleRoute(id, true)
                    }
                    // https://oxygenupdater.com/article/<id>/
                    DeepLinkArticle -> {
                        val id = segments.getOrNull(1)?.toLongOrNull() ?: return null
                        // Web article links are always 'external'
                        if (id <= 0L) null else ArticleRoute(id, true)
                    }

                    // No other domain URLs are deep linked into the app
                    else -> null
                }
            }

            OuScheme -> when (host) {
                DeepLinkArticle -> {
                    // Can't use `uri.path` as it's prefixed by "/"
                    val segments = uri.pathSegments
                    if (segments.isNullOrEmpty()) return null

                    val id = segments.getOrNull(0)?.toLongOrNull() ?: return null
                    if (id <= 0L) null else {
                        // It's marked as "external" only if intent is from a notification
                        val external = uri.getBooleanQueryParameter(ExternalArg, false)
                        ArticleRoute(id, external)
                    }
                }

                DeepLinkGuide -> GuideRoute(uri.getBooleanQueryParameter(DownloadedArg, false))
                DeepLinkFaq -> FaqRoute

                DeepLinkUpdate -> MainRoute.Update
                DeepLinkNewsList -> MainRoute.NewsList
                DeepLinkDevice -> MainRoute.Device
                DeepLinkAbout -> MainRoute.About
                DeepLinkSettings -> MainRoute.Settings

                // No other OuScheme URLs are deep linked
                else -> null
            }

            else -> null
        }.also {
            if (it == null) logDebug(TAG, "Intent data: $uri")
            else logDebug(TAG, "Handled deep link for '$uri': $it")
        }
    }

    companion object {
        private const val TAG = "MainActivity"

        private const val ActionPageUpdate = "com.oxygenupdater.action.page_update"
        private const val ActionPageNews = "com.oxygenupdater.action.page_news"
        private const val ActionPageDevice = "com.oxygenupdater.action.page_device"

        /**
         * Force even 3-button nav to be completely transparent on [Android 10+](https://github.com/android/nowinandroid/pull/817#issuecomment-1647079628)
         */
        private val navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
    }
}
