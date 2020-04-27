package io.branch.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.branch.sdk.android.search.analytics.BranchAnalytics;

import static io.branch.sdk.android.search.analytics.Defines.AnalyticsJsonKey.BranchKey;

/**
 * Branch Configuration.  Use the Branch Configuration to override default Search options.
 * <br><br>
 * Note that any empty or null options will continue to use the defaults.
 */
public class BranchConfiguration {
    final static String MANIFEST_KEY = "io.branch.sdk.BranchKey";
    private static final long SYNC_TIME_MILLIS = 1000 * 60 * 60; // 1 hour

    private long lastSyncTimeMillis = 0L;
    private final Object syncLock = new Object();

    private String url = null;
    private String key = null;
    private String googleAdID = null;
    private boolean isLat = false;
    private Locale locale; // Overrides BranchDeviceInfo
    private String countryCode;
    private int intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK;
    private final Map<String, Object> requestExtra = new HashMap<>();
    private IBranchShortcutHandler shortcutHandler = IBranchShortcutHandler.DEFAULT;

    // JSONKeys associated with a Configuration
    enum JSONKey {
        BranchKey("branch_key"),
        Country("country"),
        GAID("gaid"),
        LAT("is_lat"),
        /** matched by {@link BranchDiscoveryRequest#KEY_EXTRA} */
        RequestExtra("extra_data"),
        /** overrides {@link BranchDeviceInfo.JSONKey#Locale} */
        Locale(BranchDeviceInfo.JSONKey.Locale.toString());

        JSONKey(@NonNull String key) {
            _key = key;
        }

        @NonNull
        @Override
        public String toString() {
            return _key;
        }

        private String _key;
    }


    public BranchConfiguration() {
    }

    /**
     * Update this object to default values, fetching them if necessary.
     * @param context a context
     */
    void sync(@NonNull Context context) {
        // Check to see if the configuration already has a valid branch key. Fetch if not.
        if (!hasValidKey()) {
            fetchBranchKey(context);
        }

        // Check to see if the configuration already has a valid country code. Default if not.
        if (!hasValidCountryCode()) {
            setCountryCode(Util.getCountryCode(context));
        }

        // Check to see if the configuration already has a valid url. Default if not.
        if (!hasValidUrl()) {
            setUrl(BranchSearchInterface.BRANCH_SEARCH_URL);
        }

        // Refetch GAID even if we have a valid one. It might have changed
        // and it's not an expensive call anyway because of our SYNC_TIME_MILLIS.
        fetchGAID();

        lastSyncTimeMillis = System.currentTimeMillis();
    }

    /**
     * @return true if the url is valid.
     */
    private boolean hasValidUrl() {
        return !TextUtils.isEmpty(url);
    }

    /**
     * @return true if the Branch Key is valid.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean hasValidKey() {
        return (this.key != null && this.key.startsWith("key_live"));
    }

    /**
     * @return true if the country code is valid.
     */
    private boolean hasValidCountryCode() {
        return !TextUtils.isEmpty(countryCode);
    }

    /**
     * Override the Search URL.
     * @param url URL to use, or null to use the default
     * @return this BranchConfiguration
     */
    @NonNull
    public BranchConfiguration setUrl(@Nullable String url) {
        this.url = url;
        return this;
    }

    // Package Private
    String getUrl() {
        return this.url;
    }

    /**
     * Override the default Branch Key.
     * @param key Key to use, or null to use the default
     * @return this BranchConfiguration
     */
    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public BranchConfiguration setBranchKey(@Nullable String key) {
        this.key = key;
        BranchAnalytics.addString(BranchKey.getKey(), key);
        return this;
    }

    /**
     * Override the default Intent Launch Flags.
     * See {@link android.content.Intent#addFlags(int)} for available flags.
     * @param flags The flags to set
     * @return this BranchConfiguration
     */
    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public BranchConfiguration setLaunchIntentFlags(int flags) {
        this.intentFlags = flags;
        return this;
    }

    // Undocumented
    public int getLaunchIntentFlags() {
        return intentFlags;
    }

    /**
     * Enables or disables ad tracking limiting.
     * @param limit true to limit
     */
    void limitAdTracking(boolean limit) {
        isLat = limit;
    }

    /**
     * Whether ad tracking is limited or not.
     * @return true if limited
     */
    @SuppressWarnings("WeakerAccess")
    boolean isAdTrackingLimited() {
        return this.isLat;
    }

    /**
     * @return the Key.
     */
    @Nullable
    String getBranchKey() {
        return this.key;
    }

    /**
     * Set the Configuration Google Ad ID
     * @param id Google Ad ID
     * @return this BranchConfiguration
     */
    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    BranchConfiguration setGoogleAdID(@Nullable String id) {
        this.googleAdID = id;
        return this;
    }

    /**
     * @return the Google Ad Id.
     */
    @SuppressWarnings("WeakerAccess")
    @Nullable
    String getGoogleAdID() {
        return this.googleAdID;
    }

    /**
     * Set the Configuration Locale.
     * @param locale Locale.
     * @return this BranchConfiguration
     */
    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    BranchConfiguration setLocale(@Nullable Locale locale) {
        this.locale = locale;
        return this;
    }

    /**
     * Set the Country Code.  eg. "US"
     * @param cc Country Code
     * @return this BranchConfiguration
     */
    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    BranchConfiguration setCountryCode(@Nullable String cc) {
        this.countryCode = cc;
        return this;
    }

    String getCountryCode() {
        return this.countryCode;
    }

    /**
     * Retrieves the Branch Key from the Package Manager Metadata.
     * @param context Context
     */
    private void fetchBranchKey(@NonNull Context context) {
        String key = null;
        try {
            final ApplicationInfo ai = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData != null) {
                key = ai.metaData.getString(MANIFEST_KEY);
            }
        } catch (final Exception ignore) {
        }
        setBranchKey(key);
    }

    /**
     * Retrieves the Google Ad ID from the play-services library.
     * Note that this is the only place where the dependency for play-services-ads is needed.
     */
    @SuppressLint("StaticFieldLeak")
    private void fetchGAID() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    BranchSearch search = BranchSearch.getInstance();
                    if (search == null) {
                        return null;
                    }
                    Context context = search.getApplicationContext();
                    BranchConfiguration config = search.getBranchConfiguration();
                    AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                    config.setGoogleAdID(adInfo != null ? adInfo.getId() : null);
                    config.limitAdTracking(adInfo != null && adInfo.isLimitAdTrackingEnabled());
                } catch (Exception e) {
                    Log.i("BranchConfiguration", "Got exception: "
                            + e.getClass().getName() + " with message: "
                            + e.getMessage());
                } catch (NoClassDefFoundError e) {
                    // This is thrown if no gms base library is on our classpath, ignore it.
                    // NOTE: This should never happen unless the library is explicitly removed from
                    // our dependency or in case of bad AAR implementation.
                    Log.i("BranchConfiguration", "Could not find the play-services lib.");
                }
                return null;
            }
        }.execute();
    }

    /**
     * Adds extra data that will be attached to server requests in form
     * of a key-value pair. If request specific values are passed to
     * {@link BranchDiscoveryRequest#setExtra(String, Object)}, those values
     * will override the ones specified here with the same key.
     * @param key a key
     * @param data value
     */
    @SuppressWarnings("WeakerAccess")
    public void addRequestExtra(@NonNull String key, @NonNull Object data) {
        requestExtra.put(key, data);
    }

    /**
     * Override the default shortcut handler to validate and launch Shortcut results.
     * @param shortcutHandler handler to use
     * @return this BranchConfiguration
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    @NonNull
    public BranchConfiguration setShortcutHandler(@NonNull IBranchShortcutHandler shortcutHandler) {
        this.shortcutHandler = shortcutHandler;
        return this;
    }

    /**
     * Returns the shortcut handler.
     * @see #setShortcutHandler(IBranchShortcutHandler)
     * @return the shortcut handler
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public IBranchShortcutHandler getShortcutHandler() {
        return shortcutHandler;
    }

    /**
     * Add Configuration Information to a JSON object.
     */
    void addConfigurationInfo(@NonNull JSONObject jsonObject) {
        // Anytime we're being used, see if we should re-sync.
        // Synchronize just in case someone fires requests from different threads at once.
        synchronized (syncLock) {
            long now = System.currentTimeMillis();
            if (now > lastSyncTimeMillis + SYNC_TIME_MILLIS) {
                BranchSearch search = BranchSearch.getInstance();
                if (search != null) {
                    sync(search.getApplicationContext());
                } else {
                    // Object being used but BranchSearch not initialized.
                    // This can happen in tests. Ignore.
                }
            }
        }

        // Write.
        try {
            jsonObject.putOpt(JSONKey.BranchKey.toString(), getBranchKey());
            jsonObject.putOpt(JSONKey.Country.toString(), countryCode);
            if (locale != null) {
                jsonObject.putOpt(JSONKey.Locale.toString(), locale.getDisplayName());
            }

            // Pass the GAID and the LAT flag.
            jsonObject.putOpt(JSONKey.GAID.toString(), getGoogleAdID());
            jsonObject.putOpt(JSONKey.LAT.toString(), (isAdTrackingLimited() ? 1 : 0));

            // Add extra request data.
            // The JSONObject for this key might already exist because the key is shared
            // between this class and BranchDiscoveryRequest.
            if (!requestExtra.keySet().isEmpty()) {
                JSONObject extraData = jsonObject.optJSONObject(JSONKey.RequestExtra.toString());
                if (extraData == null) extraData = new JSONObject();

                for (String key : requestExtra.keySet()) {
                    Object value = requestExtra.get(key);
                    if (!extraData.has(key)) {
                        extraData.putOpt(key, value);
                    }
                }
                jsonObject.putOpt(JSONKey.RequestExtra.toString(), extraData);
            }

        } catch (JSONException ignore) {
        }
    }


}