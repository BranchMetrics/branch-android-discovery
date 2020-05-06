package io.branch.sdk.android.search.analytics;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import static io.branch.sdk.android.search.analytics.AnalyticsUtil.Logd;
import static io.branch.sdk.android.search.analytics.Defines.AnalyticsWindowId;
import static io.branch.sdk.android.search.analytics.Defines.Area;
import static io.branch.sdk.android.search.analytics.Defines.Handler;
import static io.branch.sdk.android.search.analytics.Defines.Clicks;
import static io.branch.sdk.android.search.analytics.Defines.EmptySessions;
import static io.branch.sdk.android.search.analytics.Defines.Impressions;
import static io.branch.sdk.android.search.analytics.Defines.LOGTAG;
import static io.branch.sdk.android.search.analytics.Defines.PreviousAnalyticsWindowId;
import static io.branch.sdk.android.search.analytics.Defines.Timestamp;

// TODO register receiver for ACTION_SHUTDOWN intent to capture sessions that would be lost because
//  our lifecycle observer on does fire onDestroy().

// TODO add disableAnalytics() API?
class BranchAnalyticsInternal implements LifecycleObserver {
    private static final String BNC_ANALYTICS_NO_VAL = "BNC_ANALYTICS_NO_VAL";

    @NonNull String sessionId = BNC_ANALYTICS_NO_VAL;
    private static BranchAnalyticsInternal instance;
    SharedPreferences sharedPreferences;

    // default clicks and impressions
    private List<JSONObject> clicks = Collections.synchronizedList(new LinkedList<JSONObject>());
    private List<JSONObject> impressions = Collections.synchronizedList(new LinkedList<JSONObject>());

    // clicks and impressions per API
    private final HashMap<String, List<JSONObject>> clicksPerApi = new HashMap<>();
    private final HashMap<String, List<JSONObject>> impressionsPerApi = new HashMap<>();

    private final HashMap<String, List<JSONObject>> trackedObjects = new HashMap<>();
    private final HashMap<String, List<String>> trackedStrings = new HashMap<>();
    private final HashMap<String, List<Integer>> trackedInts = new HashMap<>();
    private final HashMap<String, List<Double>> trackedDoubles = new HashMap<>();
    private final HashMap<String, List<JSONArray>> trackedArrays = new HashMap<>();
    private final HashMap<String, List<?>>[] trackedValues = new HashMap[]{trackedObjects, trackedStrings, trackedInts, trackedDoubles, trackedArrays};


    // individually tracked values
    private final ConcurrentHashMap<String, JSONObject> individuallyTrackedObjects = new ConcurrentHashMap<>();// e.g. ???
    private final ConcurrentHashMap<String, String> individuallyTrackedStrings = new ConcurrentHashMap<>();// e.g. ???
    private final ConcurrentHashMap<String, Integer> individuallyTrackedInts = new ConcurrentHashMap<>();// e.g. ???
    private final ConcurrentHashMap<String, Double> individuallyTrackedDoubles = new ConcurrentHashMap<>();// e.g. ???
    private final ConcurrentHashMap<String, JSONArray> individuallyTrackedArrays = new ConcurrentHashMap<>();// e.g. ???
    private final ConcurrentHashMap<String, ?>[] individuallyTrackedValues = new ConcurrentHashMap[]{individuallyTrackedObjects, individuallyTrackedStrings, individuallyTrackedInts, individuallyTrackedDoubles, individuallyTrackedArrays};

    // static values
    private final ConcurrentHashMap<String, JSONObject> staticObjects = new ConcurrentHashMap<>();// e.g. "device_info", "sdk_config"
    private final ConcurrentHashMap<String, String> staticStrings = new ConcurrentHashMap<>();// e.g. "branch_key"
    private final ConcurrentHashMap<String, Integer> staticInts = new ConcurrentHashMap<>();// e.g. "empty_sessions"
    private final ConcurrentHashMap<String, Double> staticDoubles = new ConcurrentHashMap<>();// e.g. ???
    private final ConcurrentHashMap<String, JSONArray> staticArrays = new ConcurrentHashMap<>();// e.g. ???
    private final ConcurrentHashMap<String, ?>[] staticValues = new ConcurrentHashMap[]{staticObjects, staticStrings, staticInts, staticDoubles, staticArrays};

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
        Logd("Returning to foreground");
        sessionId =  UUID.randomUUID().toString();
    }

    /**
     * This is where we stop collecting analytics and start uploading them to the server
     * Note, this method is invoked with 700 millisecond delay to ensure app is actually going to
     * background, and that it's not just device rotation change.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onMoveToBackground() {
        Logd("Moving to background");
        if (!isEmptySession()) {
            AnalyticsUtil.startUpload(formatPayload().toString());
            clearTrackedData();
            sharedPreferences.edit().putString(PreviousAnalyticsWindowId, sessionId).apply();
        } else {
            emptySessionCount++;
        }
        sessionId = BNC_ANALYTICS_NO_VAL;
    }

    void clearTrackedData() {
        emptySessionCount = 0;

        clicks.clear();
        impressions.clear();

        impressionsPerApi.clear();
        clicksPerApi.clear();

        clearTrackedValues();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            BranchImpressionTracking.clearImpressions();
        }
    }

    void clearStaticData() {
        for (ConcurrentHashMap<String, ?> trackedValuesOfCertainType : staticValues) {
            trackedValuesOfCertainType.clear();
        }
    }

    void clearTrackedValues() {
        for (ConcurrentHashMap<String, ?> individuallyTrackedValuesOfCertainType : individuallyTrackedValues) {
            individuallyTrackedValuesOfCertainType.clear();
        }
        for (HashMap<String, ?> individuallyTrackedValuesOfCertainType : trackedValues) {
            individuallyTrackedValuesOfCertainType.clear();
        }
    }

    private boolean isEmptySession() {
        // we ignore static payload values
        boolean noClicksOrImpressions = clicks.isEmpty() && impressions.isEmpty() && clicksPerApi.isEmpty() && impressionsPerApi.isEmpty();
        boolean noTrackedValues = trackedObjects.isEmpty() && trackedStrings.isEmpty() && trackedInts.isEmpty() && trackedDoubles.isEmpty() && trackedArrays.isEmpty();
        boolean noIndividuallyTrackedValues = individuallyTrackedObjects.isEmpty() && individuallyTrackedStrings.isEmpty() && individuallyTrackedInts.isEmpty() && individuallyTrackedDoubles.isEmpty() && individuallyTrackedArrays.isEmpty();
        return noClicksOrImpressions && noTrackedValues && noIndividuallyTrackedValues;
    }

    void trackClick(@NonNull TrackedEntity entity, @NonNull String clickType) {
        JSONObject clickJson = entity.getClickJson();
        if (clickJson == null) return;

        try {
            clickJson.putOpt(Handler, clickType);
        } catch (JSONException ignored) { }

        if (TextUtils.isEmpty(entity.getAPI())) {
            // write to default clicks
            clicks.add(clickJson);
        } else {
            synchronized (clicksPerApi) {
                List<JSONObject> apiClicks = clicksPerApi.get(entity.getAPI());
                if (apiClicks == null) {
                    apiClicks = new LinkedList<>();
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
            impression = impression.put(Area, area);
            impression = impression.put(Timestamp, System.currentTimeMillis());
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
                    apiImpressions = new LinkedList<>();
                }
                apiImpressions.add(impression);
                impressionsPerApi.put(entity.getAPI(), apiImpressions);
            }
        }
    }

    @NonNull JSONObject formatPayload() {

        JSONObject payload = new JSONObject();
        try {
            payload.putOpt(AnalyticsWindowId, sessionId);
            payload.putOpt(EmptySessions, emptySessionCount);
            payload.putOpt(PreviousAnalyticsWindowId, sharedPreferences.getString(PreviousAnalyticsWindowId, null));

            loadIndividuallyRecordedValues(payload, staticValues);
            loadIndividuallyRecordedValues(payload, individuallyTrackedValues);

            loadClicksAndImpressions(payload);

            loadGroupedTrackedValues(payload);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }

    private void loadClicksAndImpressions(JSONObject payload) {
        for (Map.Entry<String, List<JSONObject>> apiClickEntry : clicksPerApi.entrySet()) {
            try {
                payload.putOpt(apiClickEntry.getKey() + "_" + Clicks, new JSONArray(apiClickEntry.getValue()));
            } catch (JSONException e) {
                Logd("failed to load clicks from api: " + apiClickEntry.getKey());
            }
        }
        for (Map.Entry<String, List<JSONObject>> apiImpressionEntry : impressionsPerApi.entrySet()) {
            try {
                payload.putOpt(apiImpressionEntry.getKey() + "_" + Impressions, new JSONArray(apiImpressionEntry.getValue()));
            } catch (JSONException e) {
                Logd("failed to load impressions from api: " + apiImpressionEntry.getKey());
            }
        }
        if (!clicks.isEmpty()) {
            try {
                payload.putOpt(Clicks, new JSONArray(clicks));
            } catch (JSONException e) {
                Logd("Failed to load generic clicks. Error: " + e.getMessage());
            }
        }
        if (!impressions.isEmpty()) {
            try {
                payload.putOpt(Impressions, new JSONArray(impressions));
            } catch (JSONException e) {
                Logd("Failed to load generic impressions. Error: " + e.getMessage());
            }
        }
    }

    private void loadGroupedTrackedValues(@NonNull JSONObject payload) {
        for (HashMap<String, List<?>> trackedValuesOfType : trackedValues) {
            for (Map.Entry<String, List<?>> trackedValuesOfTypeEntry : trackedValuesOfType.entrySet()) {
                try {
                    payload.putOpt(trackedValuesOfTypeEntry.getKey(), new JSONArray(trackedValuesOfTypeEntry.getValue()));
                } catch (JSONException e) {
                    Log.i(LOGTAG, "failed to load tracked values of type: " + trackedValuesOfTypeEntry.getValue().getClass() + " for key: " + trackedValuesOfTypeEntry.getKey());
                }
            }
        }
    }

    private void loadIndividuallyRecordedValues(JSONObject payload, ConcurrentHashMap<String, ?>[] allValues) {
        for (ConcurrentHashMap<String, ?> staticValuesOfCertainType : allValues) {
            for (Map.Entry<String, ?> allValuesOfCertainType : staticValuesOfCertainType.entrySet()) {
                try {
                    payload.putOpt(allValuesOfCertainType.getKey(), allValuesOfCertainType.getValue());
                } catch (JSONException ignored) {
                    Logd("failed to load values of type: " + allValuesOfCertainType.getKey());
                }
            }
        }
    }

    void trackObject(@NonNull String key, @NonNull JSONObject trackedObject, boolean grouped) {
        if (!grouped) {
            individuallyTrackedObjects.put(key, trackedObject);
        } else {
            synchronized (trackedObjects) {
                List<JSONObject> groupedTrackedObjects = trackedObjects.get(key);
                if (groupedTrackedObjects == null) {
                    groupedTrackedObjects = new LinkedList<>();
                }
                groupedTrackedObjects.add(trackedObject);
                trackedObjects.put(key, groupedTrackedObjects);
            }
        }
    }

    void trackString(@NonNull String key, @NonNull String trackedString, boolean grouped) {
        if (!grouped) {
            individuallyTrackedStrings.put(key, trackedString);
        } else {
            synchronized (trackedStrings) {
                List<String> groupedTrackedStrings = trackedStrings.get(key);
                if (groupedTrackedStrings == null) {
                    groupedTrackedStrings = new LinkedList<>();
                }
                groupedTrackedStrings.add(trackedString);
                trackedStrings.put(key, groupedTrackedStrings);
            }
        }
    }

    void trackInt(@NonNull String key, @NonNull Integer trackedInt, boolean grouped) {
        if (!grouped) {
            individuallyTrackedInts.put(key, trackedInt);
        } else {
            synchronized (trackedInts) {
                List<Integer> groupedTrackedInts = trackedInts.get(key);
                if (groupedTrackedInts == null) {
                    groupedTrackedInts = new LinkedList<>();
                }
                groupedTrackedInts.add(trackedInt);
                trackedInts.put(key, groupedTrackedInts);
            }
        }
    }

    void trackDouble(@NonNull String key, @NonNull Double trackedDouble, boolean grouped) {
        if (!grouped) {
            individuallyTrackedDoubles.put(key, trackedDouble);
        } else {
            synchronized (trackedDoubles) {
                List<Double> groupedTrackedDoubles = trackedDoubles.get(key);
                if (groupedTrackedDoubles == null) {
                    groupedTrackedDoubles = new LinkedList<>();
                }
                groupedTrackedDoubles.add(trackedDouble);
                trackedDoubles.put(key, groupedTrackedDoubles);
            }
        }
    }

    void trackArray(@NonNull String key, @NonNull JSONArray trackedArray, boolean grouped) {
        if (!grouped) {
            individuallyTrackedArrays.put(key, trackedArray);
        } else {
            synchronized (trackedArrays) {
                List<JSONArray> groupedTrackedArrays = trackedArrays.get(key);
                if (groupedTrackedArrays == null) {
                    groupedTrackedArrays = new LinkedList<>();
                }
                groupedTrackedArrays.add(trackedArray);
                trackedArrays.put(key, groupedTrackedArrays);
            }
        }
    }

    void addObject(@NonNull String key, @Nullable JSONObject staticObject) {
        if (staticObject == null) {
            staticObjects.remove(key);
        } else {
            staticObjects.put(key, staticObject);
        }
    }

    void addString(@NonNull String key, @Nullable String staticString) {
        if (staticString == null) {
            staticStrings.remove(key);
        } else {
            staticStrings.put(key, staticString);
        }
    }

    void addInt(@NonNull String key, @Nullable Integer staticInt) {
        if (staticInt == null) {
            staticInts.remove(key);
        } else {
            staticInts.put(key, staticInt);
        }
    }

    void addDouble(@NonNull String key, @Nullable Double staticDouble) {
        if (staticDouble == null) {
            staticDoubles.remove(key);
        } else {
            staticDoubles.put(key, staticDouble);
        }
    }

    void addArray(@NonNull String key, @Nullable JSONArray staticArray) {
        if (staticArray == null) {
            staticArrays.remove(key);
        } else {
            staticArrays.put(key, staticArray);
        }
    }
}
