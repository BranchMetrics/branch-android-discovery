package io.branch.sdk.android.search.analytics;

import android.support.annotation.NonNull;

public class Defines {

    public enum AnalyticsJsonKey {
        // API keys
        AnalyticsWindowId("analytics_window_id"),

        // API keys
        Hints("hints"),
        Autosuggest("autosuggest"),
        Search("search"),

        // array keys
        Clicks("clicks"),
        Impressions("impressions"),

        // value keys
        BranchKey("branch_key"),
        DeviceInfo("device_info"),
        ConfigInfo("config_info"),
        EmptySessions("empty_sessions"),
        RequestId("request_id"),
        ResultId("result_id"),
        EntityId("entity_id"),
        Timestamp("timestamp"),
        Hint("hint"),
        Autosuggestion("autosuggestion"),
        Rank("rank"),
        ClickType("click_type"),
        VirtualRequest("virtual_request"),
        Area("area"),
        StartTime("start_time"),
        RoundTripTime("round_trip_time"),
        StatusCode("status_code");

        @NonNull private String key = "";

        AnalyticsJsonKey(@NonNull String key) {
            this.key = key;
        }

        @NonNull public String getKey() {
            return key;
        }

        @Override @NonNull public String toString() {
            return key;
        }
    }
}
