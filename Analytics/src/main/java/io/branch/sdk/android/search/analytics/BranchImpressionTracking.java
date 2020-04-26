package io.branch.sdk.android.search.analytics;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Coordinates impression tracking, including managing {@link ViewTracker}s,
 * batching results and uploading them to server.
 */
class BranchImpressionTracking {

    private static Map<View, ViewTracker> sTrackers = new WeakHashMap<>();
    private static Set<Integer> sImpressionIds = new HashSet<>();

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    static void trackImpressions(@NonNull View view, @NonNull TrackedEntity result) {
        ViewTracker tracker;
        if (sTrackers.containsKey(view)) {
            tracker = sTrackers.get(view);
        } else {
            tracker = new ViewTracker(view);
            sTrackers.put(view, tracker);
        }
        //noinspection ConstantConditions
        tracker.bindTo(result);
    }

    static boolean hasTrackedImpression(@NonNull TrackedEntity result) {
        return sImpressionIds.contains(result.getImpressionJson().hashCode());
    }

    static void recordImpression(@NonNull Context context,
                                 @NonNull TrackedEntity result,
                                 float area) {
        // Record the ID so it's not saved twice.
        boolean isNew = sImpressionIds.add(result.getImpressionJson().hashCode());
        if (!isNew) return;
        BranchAnalyticsInternal.getInstance().trackImpression(result, area);
    }
}