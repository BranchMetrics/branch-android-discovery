package io.branch.sdk.android.search.analytics;

import org.json.JSONObject;

public abstract class TrackedEntity {
    protected abstract JSONObject getImpressionJson();
    protected abstract JSONObject getClickJson();
    protected abstract String getAPI();
}
