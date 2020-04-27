package io.branch.sdk.android.search.analytics;

import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * Analytics module tracks 'clicks' and 'impressions' of objects that belong to the Search SDK APIs
 * (e.g. 'BranchQueryHint', 'BranchAutoSuggestion', 'BranchAppResult'/'BranchLinkResult').
 * The latter objects must implement the interface, 'TrackedEntity', which contains three methods,
 *
 *      'JSONObject getImpressionJson()'                       - return null disable tracking impressions
 *      'JSONObject getClickJson()'                            - return null disable tracking clicks
 *      'String getAPI()'                                      - return key prefix for payload formatting (e.g. "api_clicks":[...], "api_impressions":[...])
 *
 * Analytics module then collect 'clicks' and 'impressions' via the following APIs:
 * 
 *      'trackImpressions(View view, TrackedEntity impression)'- Called at creation time of the view.
 *      'trackClick(TrackedEntity click)'                      - Called on click of some TrackedEntity
 *      TODO overload with 'boolean countRepeats'
 *
 *      TODO add a some 50 or so local variables of List objects, when we spot a new api, assign value to variable and synchronize over that list instead of impressions hashmap for all APIs
 *
 * Analytics module can also track custom (json compliant) key value pairs via the following APIs:
 *
 *      trackObject(String key, JSONObject customEvent)
 *      trackString(String key, String customString)
 *      trackInt(String key, Integer customInt)
 *      trackDouble(String key, Double customDouble)
 *      trackArray(String key, JSONArray customArray)
 *      TODO overload the above with 'String jsonParentKey'
 *
 * If there are tracked 'clicks', 'impressions' or custom objects, the upload to the server does not
 * happen but the count of these empty sessions is kept and reported in the next upload.
 *
 * Finally, there are also APIs to 'add' objects to the payload. These objects are considered static,
 * or otherwise, not important enough to post them to the server in the case there are no tracked events.
 * Static objects will survive sessions though, so once added, they will be reported in the next payload
 * whenever it happens. Note, also that, unlike tracked events, added objects/events can be removed by
 * passing in `null`. Alternatively, client can remove all static values with the API, `clearStaticValues()`.
 *
 *      addObject(String key, JSONObject customEvent)
 *      addString(String key, String customString)
 *      addInt(String key, Integer customInt)
 *      addDouble(String key, Double customDouble)
 *      addArray(String key, JSONArray customArray)
 *
 *      clearStaticValues()
 * 
 * Note, on the server side, 'session' has a different (business logic) meaning and lasts across multiple
 * app visibility lifecycles.
 * 
 * Analytics module tech spec: https://www.notion.so/branchdisco/SDK-side-Analytics-Module-spec-ff2b69a0438649d287a794b7298a5f10
 */
public class BranchAnalytics {
    static final String LOGTAG = "BranchAnalytics";

    /**
     * Initialize Analytics in App.onCreate()
     */
    public static void init(Context application) {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(BranchAnalyticsInternal.getInstance());
    }

    /**
     * Use APIs registerClickEvent and registerImpressionEvent to register default clicks and events,
     * or split them up in the payload by "clickCategory" and "impressionCategory"
     */
    public static void trackClick(@NonNull TrackedEntity click, @NonNull String clickType) {
        if (click.getClickJson() == null) return;
        BranchAnalyticsInternal.getInstance().registerClick(click, clickType);
    }

    /**
     * `trackXXX` APIs add the XXX entity to top level of the payload and are treated as records of
     * user behavior, thus `isEmptySession()` will return false if there is even a single record or
     * user behavior and the module will proceed to make the upload to the server.
     */
    public static void trackObject(@NonNull String key, @NonNull JSONObject customEvent) {
        BranchAnalyticsInternal.getInstance().trackObject(key, customEvent);
    }

    public static void trackString(@NonNull String key, @NonNull String customString) {
        BranchAnalyticsInternal.getInstance().trackString(key, customString);
    }

    public static void trackInt(@NonNull String key, Integer customInt) {
        BranchAnalyticsInternal.getInstance().trackInt(key, customInt);
    }

    public static void trackDouble(@NonNull String key, @NonNull Double customDouble) {
        BranchAnalyticsInternal.getInstance().trackDouble(key, customDouble);
    }

    public static void trackArray(@NonNull String key, @NonNull JSONArray customArray) {
        BranchAnalyticsInternal.getInstance().trackArray(key, customArray);
    }

    /**
     * `addXXX(key, value)` APIs add the XXX entity to top level of the payload. These values are treated as static
     * and not related to the user behavior. If static data is the only data passed to the analytics
     * module during this session, analytics module will treat it as an empty session and will not make
     * the upload to the server. Note, each of the addXXX(key, value) APIs accepts `null` as value, in
     * which case the associated value is removed.
     */
    public static void addObject(@NonNull String key, @Nullable JSONObject staticObject) {
        BranchAnalyticsInternal.getInstance().addObject(key, staticObject);// (e.g. 'device_info', 'sdk_configuration')
    }

    public static void addString(@NonNull String key, @Nullable String staticString) {
        BranchAnalyticsInternal.getInstance().addString(key, staticString);// (e.g. 'branch_key')
    }

    public static void addInt(@NonNull String key, @Nullable Integer staticInt) {
        BranchAnalyticsInternal.getInstance().addInt(key, staticInt);// (e.g. ??)
    }

    public static void addDouble(@NonNull String key, @Nullable Double staticDouble) {
        BranchAnalyticsInternal.getInstance().addDouble(key, staticDouble);// (e.g. ???)
    }

    public static void addArray(@NonNull String key, @Nullable JSONArray staticArray) {
        BranchAnalyticsInternal.getInstance().addArray(key, staticArray);// (e.g. ???)
    }

    /**
     * Get the current state of the analytics batch
     */
    public static JSONObject getAnalyticsData() {
        return BranchAnalyticsInternal.getInstance().formatPayload();
    }

    /**
     * Get analytics window id (so it can added to API requests for analytics data matching)
     */
    public static String getAnalyticsWindowId() {
        return BranchAnalyticsInternal.getInstance().sessionId;
    }

    /**
     * Clear values added via the addXXX(key, XXX) APIs,
     * you may also delete individual values by passing in `null` e.g. addXXX(key, null)
     */
    public static void clearStaticValues() {
        BranchAnalyticsInternal.getInstance().clearStaticValues();
    }

    public static void trackImpressions(@NonNull View view, @NonNull TrackedEntity result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            BranchImpressionTracking.trackImpressions(view, result);
        }
    }
}
