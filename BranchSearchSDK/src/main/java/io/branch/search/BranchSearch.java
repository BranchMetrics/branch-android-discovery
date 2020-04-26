package io.branch.search;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import io.branch.sdk.android.search.analytics.BranchAnalytics;

/**
 * Main entry class for Branch Discovery. This class need to be initialized before accessing any Branch
 * discovery functionality.
 *
 * Note that Branch Discovery needs location permission for better discovery experience. Please make sure
 * your app has location permission granted.
 */
public class BranchSearch {
    // Each protocol that we handle has to have its own Network channel
    enum Channel { SEARCH, AUTOSUGGEST, QUERYHINT }

    private static final String TAG = "BranchSearch";
    private static BranchSearch thisInstance;

    @VisibleForTesting
    URLConnectionNetworkHandler[] networkHandlers
            = new URLConnectionNetworkHandler[Channel.values().length];

    private BranchConfiguration branchConfiguration;
    private BranchDeviceInfo branchDeviceInfo;
    private Context appContext;

    /**
     * Initialize the BranchSearch SDK with the default configuration options.
     * @param context Context
     * @return this BranchSearch instance.
     */
    public static BranchSearch init(@NonNull Context context) {
        BranchAnalytics.init(context);
        return init(context, new BranchConfiguration());
    }

    /**
     * Initialize the BranchSearch SDK with custom configuration options.
     * @param context Context
     * @param config {@link BranchConfiguration} configuration
     * @return this BranchSearch instance.
     */
    public static BranchSearch init(@NonNull Context context, @NonNull BranchConfiguration config) {
        thisInstance = new BranchSearch(context, config, new BranchDeviceInfo());

        // Initialize BranchSearch objects.
        thisInstance.branchDeviceInfo.sync(thisInstance.getApplicationContext());
        thisInstance.branchConfiguration.sync(thisInstance.getApplicationContext());

        // Ensure that there is a valid key
        // TODO dev gave us a bad key. why would we return null here (making getInstance() nullable
        //  and crashing later in unexpected ways) instead of crashing with a clear message?
        //  We need a key to work! Our code would also be more elegant since we could crash in config.sync().
        if (!config.hasValidKey()) {
            Log.e(TAG, "Invalid Branch Key.");
            thisInstance = null;
        }
        return thisInstance;
    }

    /**
     * Get the BranchSearch Instance.
     * @return this BranchSearch instance.
     */
    public static BranchSearch getInstance() {
        return thisInstance;
    }

    private BranchSearch(@NonNull Context context,
                         @NonNull BranchConfiguration config,
                         @NonNull BranchDeviceInfo info) {
        this.appContext = context.getApplicationContext();
        this.branchConfiguration = config;
        this.branchDeviceInfo = info;

        // We need a network handler for each protocol.
        for (Channel channel : Channel.values()) {
            this.networkHandlers[channel.ordinal()] = URLConnectionNetworkHandler.initialize();
        }
    }

    /**
     * Get the BranchSearch Version.
     * @return this BranchSearch Build version
     */
    @NonNull
    public static String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * Query for results.
     * @param request {@link BranchSearchRequest} request
     * @param callback {@link IBranchSearchEvents} Callback to receive results
     * @return true if the request was posted
     */
    public boolean query(@NonNull BranchSearchRequest request,
                         @NonNull IBranchSearchEvents callback) {
        return BranchSearchInterface.search(request, callback);
    }

    /**
     * Retrieve a list of suggestions on kinds of things one might request.
     * @param request A request object
     * @param callback {@link IBranchQueryHintEvents} Callback to receive results.
     * @return true if the request was posted.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean queryHint(@NonNull BranchQueryHintRequest request,
                             @NonNull IBranchQueryHintEvents callback) {
        return BranchSearchInterface.queryHint(request, callback);
    }

    /**
     * Retrieve a list of suggestions on kinds of things one might request.
     * @param callback {@link IBranchQueryHintEvents} Callback to receive results.
     * @return true if the request was posted.
     */
    @SuppressWarnings({"UnusedReturnValue", "unused"})
    public boolean queryHint(@NonNull IBranchQueryHintEvents callback) {
        return queryHint(BranchQueryHintRequest.create(), callback);
    }

    /**
     * Retrieve a list of auto-suggestions based on a query parameter.
     * Example:  "piz" might return ["pizza", "pizza near me", "pizza my heart"]
     * @param request {@link BranchAutoSuggestRequest} request
     * @param callback {@link IBranchAutoSuggestEvents} Callback to receive results.
     * @return true if the request was posted.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean autoSuggest(@NonNull BranchAutoSuggestRequest request,
                               @NonNull IBranchAutoSuggestEvents callback) {
        return BranchSearchInterface.autoSuggest(request, callback);
    }

    // Package Private
    @NonNull
    URLConnectionNetworkHandler getNetworkHandler(@NonNull Channel channel) {
        return this.networkHandlers[channel.ordinal()];
    }

    // Undocumented
    // TODO This should not be public! Once the user creates a configuration and initializes
    //  the SDK, he should not be able to change the configuration values while we're running, or
    //  our behavior might change/break/be undefined.
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public final BranchConfiguration getBranchConfiguration() {
        return branchConfiguration;
    }

    @NonNull
    BranchDeviceInfo getBranchDeviceInfo() {
        return branchDeviceInfo;
    }

    /**
     * Static utility to check whether the service is enabled.
     *
     * Unlike {@link #isServiceEnabled(String, IBranchServiceEnabledEvents)}, this will read
     * the key defined in the manifest file. If there's none, this will throw an exception.
     *
     * @param context a context used for parsing manifest
     * @param callback a callback for receiving results
     * @throws RuntimeException if there's no key defined in the Manifest file
     */
    public static void isServiceEnabled(@NonNull Context context,
                                        @NonNull IBranchServiceEnabledEvents callback) {
        try {
            ApplicationInfo info = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            String key = info.metaData.getString(BranchConfiguration.MANIFEST_KEY);
            if (key != null) {
                isServiceEnabled(key, callback);
                return;
            }
        } catch (PackageManager.NameNotFoundException ignore) {
            // Should never happen
        }
        throw new RuntimeException("isServiceEnabled(Context, IBranchServiceEnabledEvents) was" +
                " called but no Branch key was found in the Manifest file. Please define one or" +
                " simply use isServiceEnabled(String, IBranchServiceEnabledEvents) instead.");
    }

    /**
     * Static utility to check whether the service is enabled for the given
     * Branch key.
     *
     * @param branchKey the branch key to check
     * @param callback a callback for receiving results
     */
    @SuppressWarnings("WeakerAccess")
    public static void isServiceEnabled(@NonNull String branchKey,
                                        @NonNull IBranchServiceEnabledEvents callback) {
        BranchSearchInterface.serviceEnabled(branchKey, callback);
    }

    @NonNull
    Context getApplicationContext() {
        return appContext;
    }

    /**
     * Sets the location that will be appended to all search, query hint and autosuggest requests
     * triggered by the SDK.
     * @param latitude user latitude
     * @param longitude user longitude
     */
    public void setLocation(double latitude, double longitude) {
        branchDeviceInfo.latitude = latitude;
        branchDeviceInfo.longitude = longitude;
    }

    // Legacy
    // Deprecated version of our APIs

    /**
     * Legacy: retrieve a list of suggestions on kinds of things one might request.
     * @deprecated please use {@link #queryHint(BranchQueryHintRequest, IBranchQueryHintEvents)} instead
     * @return true if the request was posted.
     */
    @Deprecated
    @SuppressWarnings("UnusedReturnValue")
    public boolean queryHint(@NonNull final IBranchQueryResults callback) {
        // Wrap the old callback in the new callback.
        return queryHint(BranchQueryHintRequest.create(), new IBranchQueryHintEvents() {
            @Override
            public void onBranchQueryHintResult(@NonNull BranchQueryHintResult result) {
                BranchQueryResult legacy = new BranchQueryResult();
                for (BranchQueryHint hint : result.getHints()) {
                    legacy.queryResults.add(hint.getQuery());
                }
                callback.onQueryResult(legacy);
            }

            @Override
            public void onBranchQueryHintError(@NonNull BranchSearchError error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Legacy: retrieve a list of auto-suggestions based on a query parameter.
     * @deprecated please use {@link #autoSuggest(BranchAutoSuggestRequest, IBranchAutoSuggestEvents)} instead
     * @return true if the request was posted.
     */
    @Deprecated
    @SuppressWarnings("UnusedReturnValue")
    public boolean autoSuggest(@NonNull BranchSearchRequest request,
                               @NonNull final IBranchQueryResults callback) {
        // Wrap the old request in the new request.
        // Wrap the old callback in the new callback.
        return BranchSearchInterface.autoSuggest(
                BranchAutoSuggestRequest.create(request.getQuery()),
                new IBranchAutoSuggestEvents() {
            @Override
            public void onBranchAutoSuggestResult(@NonNull BranchAutoSuggestResult result) {
                BranchQueryResult legacy = new BranchQueryResult();
                for (BranchAutoSuggestion suggestion : result.getSuggestions()) {
                    legacy.queryResults.add(suggestion.getQuery());
                }
                callback.onQueryResult(legacy);
            }

            @Override
            public void onBranchAutoSuggestError(@NonNull BranchSearchError error) {
                callback.onError(error);
            }
        });
    }
}
