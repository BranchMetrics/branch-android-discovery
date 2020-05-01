package io.branch.sdk.android.search.analytics;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.View;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import static io.branch.sdk.android.search.analytics.BranchAnalytics.Logd;

/**
 * Coordinates impression tracking, including managing {@link ViewTracker}s.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class BranchImpressionTracking {

    private static Map<View, ViewTracker> sTrackers = new WeakHashMap<>();
    private static Set<Integer> sImpressionIds = new HashSet<>();

    // Since we clear sImpressionIds after every session, when user exits the launcher app and returns
    // to the same search results, these old results would get recorded, before the client makes a new
    // request to update the UI. To prevent this, we keep a set of the previous session impressions.
    private static Set<Integer> previousSessionImpressionIds = new HashSet<>();

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
        int entityHash = result.getImpressionJson().toString().hashCode();
        return sImpressionIds.contains(entityHash) || previousSessionImpressionIds.contains(entityHash);
    }

    static void clearImpressions() {
        previousSessionImpressionIds = new HashSet<>(sImpressionIds);
        for (ViewTracker vt : sTrackers.values()) {
            vt.onViewDetached();
        }
        sTrackers.clear();
        sImpressionIds.clear();
    }

    static void recordImpression(@NonNull TrackedEntity result,
                                 float area) {
        // Record the ID so it's not saved twice.
        boolean isNew = sImpressionIds.add(result.getImpressionJson().toString().hashCode());
        if (!isNew) return;
        Logd("recordImpression, sImpressionIds.size = " + sImpressionIds.size());
        BranchAnalyticsInternal.getInstance().trackImpression(result, area);
    }
}