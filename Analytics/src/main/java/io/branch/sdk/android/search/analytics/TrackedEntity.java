package io.branch.sdk.android.search.analytics;

import org.json.JSONObject;

public abstract class TrackedEntity {
    /**
     * Return JSONObject representing an 'impression' on this TrackedEntity or null to disable impression
     * tracking. Do not include timestamps or other data that would uniquely define impressions between
     * frames. Timestamps and percentage of the view's visible area are safely added by the module at
     * the time of tracking the first impression.
     */
    protected abstract JSONObject getImpressionJson();
    /**
     * Return JSONObject representing an 'click' on this TrackedEntity or null to disable click tracking.
     */
    protected abstract JSONObject getClickJson();
    /**
     * Return String to group clicks and impressions per API of the TrackedEntity (e.g. getAPI() returns
     * 'search', the impressions of this TrackedEntity will be stored in 'search_clicks' array at the
     * top level of the payload, rather than in the default 'clicks' array. Return null to disable grouping.
     */
    protected abstract String getAPI();
}
