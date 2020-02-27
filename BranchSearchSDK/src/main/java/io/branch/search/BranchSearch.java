package io.branch.search;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import java.lang.ref.WeakReference;

/**
 * Main entry class for Branch Discovery. This class need to initialized before accessing any Branch
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
    private Context appContext;


    // Private Constructor.
    private BranchSearch() {
    }

    /**
     * Initialize the BranchSearch SDK with the default configuration options.
     * @param context Context
     * @return this BranchSearch instance.
     */
    public static BranchSearch init(@NonNull Context context) {
        return init(context, new BranchConfiguration());
    }

    /**
     * Initialize the BranchSearch SDK with custom configuration options.
     * @param context Context
     * @param config {@link BranchConfiguration} configuration
     * @return this BranchSearch instance.
     */
    public static BranchSearch init(@NonNull Context context, @NonNull BranchConfiguration config) {
        thisInstance = new BranchSearch();
        thisInstance.initialize(context, config);

        // Initialize Device Information that doesn't change
        BranchDeviceInfo.init(context);

        // Ensure that there is a valid key
        if (!thisInstance.branchConfiguration.hasValidKey()) {
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

    /**
     * Get the BranchSearch Version.
     * @return this BranchSearch Build version
     */
    public static String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * Query for results.
     * @param request {@link BranchSearchRequest} request
     * @param callback {@link IBranchSearchEvents} Callback to receive results
     * @return true if the request was posted
     */
    public boolean query(BranchSearchRequest request, IBranchSearchEvents callback) {
        return BranchSearchInterface.Search(request, branchConfiguration, callback);
    }

    /**
     * Retrieve a list of suggestions on kinds of things one might request.
     * @param callback {@link IBranchQueryResults} Callback to receive results.
     * @return true if the request was posted.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean queryHint(final IBranchQueryResults callback) {
        return BranchSearchInterface.QueryHint(new BranchQueryHintRequest(), branchConfiguration, callback);
    }

    /**
     * Retrieve a list of auto-suggestions based on a query parameter.
     * Example:  "piz" might return ["pizza", "pizza near me", "pizza my heart"]
     * @param request {@link BranchSearchRequest} request
     * @param callback {@link IBranchQueryResults} Callback to receive results.
     * @return true if the request was posted.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean autoSuggest(BranchSearchRequest request, final IBranchQueryResults callback) {
        return BranchSearchInterface.AutoSuggest(request, branchConfiguration, callback);
    }

    /**
     * Lets Branch track impressions on the given view. Should be called anytime the view is
     * bound to a result. When the view is bound to a different (non-Branch) type of data,
     * null can be passed so that Branch can stop tracking that view.
     *
     * This function should be called when binding data. For example, in the onBindViewHolder
     * method of RecyclerView.Adapter, or, if using ListView, in the
     * {@link android.widget.Adapter#getView(int, View, ViewGroup)} callback.
     *
     * Note: this functionality will only work on Android API 18+.
     *
     * @param view the View that will hold link data
     * @param linkResult the link that will be bound to it, or null to stop tracking the view
     */
    public void trackImpressions(@NonNull View view, @Nullable BranchLinkResult linkResult) {
        if (Build.VERSION.SDK_INT >= 18) {
            BranchImpressionTracking.trackImpressions(view, linkResult);
        }
    }

    // Package Private
    URLConnectionNetworkHandler getNetworkHandler(Channel channel) {
        return this.networkHandlers[channel.ordinal()];
    }

    private void initialize(@NonNull Context context, final BranchConfiguration config) {
        new getGAIDTask(context).execute();

        // We need a network handler for each protocol.
        for (Channel channel : Channel.values()) {
            this.networkHandlers[channel.ordinal()] = URLConnectionNetworkHandler.initialize();
        }

        this.branchConfiguration = (config == null ? new BranchConfiguration() : config);
        this.branchConfiguration.setDefaults(context);
        this.appContext = context.getApplicationContext();
    }

    // Undocumented
    public final BranchConfiguration getBranchConfiguration() {
        return branchConfiguration;
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
    public static void isServiceEnabled(@NonNull String branchKey,
                                        @NonNull IBranchServiceEnabledEvents callback) {
        BranchSearchInterface.ServiceEnabled(branchKey, callback);
    }

    /**
     * Note that this is the only place where the dependency for play-services-ads is needed.
     */
    private static class getGAIDTask extends AsyncTask<Void, Void, Void> {
        private final WeakReference<Context> mContextReference;

        getGAIDTask(Context context) {
            mContextReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... unused) {
            try {
                Context context = mContextReference.get();
                if (context != null) {
                    AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                    getInstance().branchConfiguration.setGoogleAdID(adInfo != null
                            ? adInfo.getId() : null);
                    getInstance().branchConfiguration.limitAdTracking(adInfo != null
                            && adInfo.isLimitAdTrackingEnabled());
                }
            } catch (Exception ignore) {
            } catch (NoClassDefFoundError ignore) {
                // This is thrown if no gms base library is on our classpath, ignore it
            }
            return null;
        }
    }

    @NonNull
    Context getApplicationContext() {
        return appContext;
    }

}
