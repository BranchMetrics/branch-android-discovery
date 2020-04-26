package io.branch.sdk.android.search.analytics;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.branch.sdk.android.search.analytics.BranchAnalytics.LOGTAG;
import static io.branch.sdk.android.search.analytics.Defines.AnalyticsJsonKey.Area;
import static io.branch.sdk.android.search.analytics.Defines.AnalyticsJsonKey.Timestamp;

// todo register receiver for ACTION_SHUTDOWN intent to clean up (https://developer.android.com/reference/android/content/Intent.html#ACTION_SHUTDOWN)
class BranchAnalyticsInternal implements LifecycleObserver {
    private static final String BNC_ANALYTICS_NO_VAL = "BNC_ANALYTICS_NO_VAL";

    @NonNull String sessionId = BNC_ANALYTICS_NO_VAL;
    static BranchAnalyticsInternal instance;

    // default clicks and impressions
    private List<JSONObject> clicks = Collections.synchronizedList(new LinkedList<JSONObject>());
    private List<JSONObject> impressions = Collections.synchronizedList(new LinkedList<JSONObject>());

    // clicks and impressions per API
    private final HashMap<String, List<JSONObject>> clicksPerApi = new HashMap<>();
    private final HashMap<String, List<JSONObject>> impressionsPerApi = new HashMap<>();

    // tracked values
    private final ConcurrentHashMap<String, JSONObject> trackedObjects = new ConcurrentHashMap<>();// e.g. ???
    private final ConcurrentHashMap<String, String> trackedStrings = new ConcurrentHashMap<>();// e.g. ???
    private final ConcurrentHashMap<String, Integer> trackedInts = new ConcurrentHashMap<>();// e.g. ???
    private final ConcurrentHashMap<String, Double> trackedDoubles = new ConcurrentHashMap<>();// e.g. ???
    private final ConcurrentHashMap<String, JSONArray> trackedArrays = new ConcurrentHashMap<>();// e.g. ???

    // static values
    private final ConcurrentHashMap<String, JSONObject> staticObjects = new ConcurrentHashMap<>();// e.g. "device_info", "sdk_config"
    private final ConcurrentHashMap<String, String> staticStrings = new ConcurrentHashMap<>();// e.g. "branch_key"
    private final ConcurrentHashMap<String, Integer> staticInts = new ConcurrentHashMap<>();// e.g. "empty_sessions"
    private final ConcurrentHashMap<String, Double> staticDoubles = new ConcurrentHashMap<>();// e.g. ???
    private final ConcurrentHashMap<String, JSONArray> staticArrays = new ConcurrentHashMap<>();// e.g. ???

    private int emptySessionCount = 0;

    static BranchAnalyticsInternal getInstance() {
        if (instance == null) {
            instance = new BranchAnalyticsInternal();
        }
        return instance;
    }

    /**
     * Start collecting data
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onMoveToForeground() {
        Log.d(LOGTAG, "Returning to foreground");
        sessionId =  UUID.randomUUID().toString();
    }

    /**
     * This is where we stop collecting analytics and start uploading them to the server
     * Note, this method is invoked with 700 millisecond delay to ensure app is actually going to
     * background, and that it's not just device rotation change.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onMoveToBackground() {
        Log.d(LOGTAG, "Moving to background");
        if (!isEmptySession()) {
            startUpload(formatPayload());
            emptySessionCount = 0;
            cleanupSessionData();
        } else {
            emptySessionCount++;
        }
        // cleanup
        sessionId = BNC_ANALYTICS_NO_VAL;
    }

    private void cleanupSessionData() {
        //todo clear the maps
    }

    private boolean isEmptySession() {
        // we ignore static payload values
        boolean noClicksOrImpressions = clicks.isEmpty() && impressions.isEmpty() && clicksPerApi.isEmpty() && impressionsPerApi.isEmpty();
        boolean noTrackedValues = trackedObjects.isEmpty() && trackedStrings.isEmpty() && trackedInts.isEmpty() && trackedDoubles.isEmpty() && trackedArrays.isEmpty();
        return noClicksOrImpressions && noTrackedValues;
    }

    private void startUpload(@NonNull JSONObject payload) {
        AnalyticsUtil.makeUpload(payload.toString());
    }

    void registerClick(@NonNull TrackedEntity entity, @NonNull String clickType) {
        JSONObject clickJson = entity.getClickJson();
        if (clickJson == null) return;

        try {
            clickJson.putOpt("click_type", clickType);
        } catch (JSONException ignored) { }

        if (TextUtils.isEmpty(entity.getAPI())) {
            // write to default clicks
            clicks.add(clickJson);
        } else {
            synchronized (clicksPerApi) {
                List<JSONObject> apiClicks = clicksPerApi.get(entity.getAPI());
                if (apiClicks == null) {
                    apiClicks = new LinkedList<JSONObject>();
                }
                apiClicks.add(clickJson);
                clicksPerApi.put(entity.getAPI(), apiClicks);
            }
        }
    }

    void trackImpression(@NonNull TrackedEntity entity, float area) {
        if (entity.getImpressionJson() == null) return;

        JSONObject impression = entity.getImpressionJson();
        try {
            impression = impression.put(Area.getKey(), area);
            impression = impression.put(Timestamp.getKey(), System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (TextUtils.isEmpty(entity.getAPI())) {
            // write to default impressions
            impressions.add(impression);
        } else {
            synchronized (impressionsPerApi) {
                List<JSONObject> apiImpressions = impressionsPerApi.get(entity.getAPI());
                if (apiImpressions == null) {
                    apiImpressions = new LinkedList<JSONObject>();
                }
                apiImpressions.add(impression);
                impressionsPerApi.put(entity.getAPI(), apiImpressions);
            }
        }
    }

    @NonNull JSONObject formatPayload() {

        JSONObject payload = new JSONObject();
        try {
            payload.putOpt("analytics_window_id", sessionId);
            payload.putOpt("empty_sessions", emptySessionCount);

            // top level: static data not related to user actions
            for (Map.Entry<String, String> staticStringEntry : staticStrings.entrySet()) {
                payload.putOpt(staticStringEntry.getKey(), staticStringEntry.getValue());
            }
            for (Map.Entry<String, Integer> staticIntEntry : staticInts.entrySet()) {
                payload.putOpt(staticIntEntry.getKey(), staticIntEntry.getValue());
            }
            for (Map.Entry<String, Double> staticDoubleEntry : staticDoubles.entrySet()) {
                payload.putOpt(staticDoubleEntry.getKey(), staticDoubleEntry.getValue());
            }
            for (Map.Entry<String, JSONArray> staticDoubleEntry : staticArrays.entrySet()) {
                payload.putOpt(staticDoubleEntry.getKey(), staticDoubleEntry.getValue());
            }
            for (Map.Entry<String, JSONObject> staticObjectEntry : staticObjects.entrySet()) {
                payload.putOpt(staticObjectEntry.getKey(), staticObjectEntry.getValue());
            }

            // top level: records of user actions
            for (Map.Entry<String, String> trackedStringEntry : trackedStrings.entrySet()) {
                payload.putOpt(trackedStringEntry.getKey(), trackedStringEntry.getValue());
            }
            for (Map.Entry<String, Integer> trackedIntEntry : trackedInts.entrySet()) {
                payload.putOpt(trackedIntEntry.getKey(), trackedIntEntry.getValue());
            }
            for (Map.Entry<String, Double> trackedDoubleEntry : trackedDoubles.entrySet()) {
                payload.putOpt(trackedDoubleEntry.getKey(), trackedDoubleEntry.getValue());
            }
            for (Map.Entry<String, JSONArray> trackedDoubleEntry : trackedArrays.entrySet()) {
                payload.putOpt(trackedDoubleEntry.getKey(), trackedDoubleEntry.getValue());
            }
            for (Map.Entry<String, JSONObject> trackedObjectEntry : trackedObjects.entrySet()) {
                payload.putOpt(trackedObjectEntry.getKey(), trackedObjectEntry.getValue());
            }

            // 2nd level: records of common user actions that can will be grouped together
            for (Map.Entry<String, List<JSONObject>> apiClickEntry : clicksPerApi.entrySet()) {
                payload.putOpt(apiClickEntry.getKey() + "_clicks", new JSONArray(apiClickEntry.getValue()));
            }
            for (Map.Entry<String, List<JSONObject>> apiImpressionEntry : impressionsPerApi.entrySet()) {
                payload.putOpt(apiImpressionEntry.getKey() + "_impressions", new JSONArray(apiImpressionEntry.getValue()));
            }
            // todo validate json when it's being added/tracked anywhere
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }

    /**
     * `recordXXX` APIs add the XXX entity to top level of the payload and are treated as records of
     * user behavior, thus `isEmptySession()` will return false if there is even a single record or user behavior
     */
    void trackObject(@NonNull String key, @NonNull JSONObject trackedObject) {
        trackedObjects.put(key, trackedObject);
    }

    void trackString(@NonNull String key, @NonNull String trackedString) {
        trackedStrings.put(key, trackedString);
    }

    void trackInt(@NonNull String key, @NonNull Integer trackedInt) {
        trackedInts.put(key, trackedInt);
    }

    void trackDouble(@NonNull String key, @NonNull Double trackedDouble) {
        trackedDoubles.put(key, trackedDouble);
    }

    void trackArray(@NonNull String key, @NonNull JSONArray trackedArray) {
        trackedArrays.put(key, trackedArray);
    }

    /**
     * `addXXX` APIs add the XXX entity to top level of the payload. These values are treated as static
     * and not related to the user behavior. If static data is the only data recorded during this session,
     * we will treat it as an empty session and will not make the upload to the servers.
     */
    void addObject(@NonNull String key, @NonNull JSONObject staticObject) {
        staticObjects.put(key, staticObject);
    }

    void addString(@NonNull String key, @NonNull String staticString) {
        staticStrings.put(key, staticString);
    }

    void addInt(@NonNull String key, @NonNull Integer staticInt) {
        staticInts.put(key, staticInt);
    }

    void addDouble(@NonNull String key, @NonNull Double staticDouble) {
        staticDoubles.put(key, staticDouble);
    }

    void addArray(@NonNull String key, @NonNull JSONArray staticArray) {
        staticArrays.put(key, staticArray);
    }
}
