package io.branch.sdk.android.search.analytics;

public class Defines {

    public enum AnalyticsJsonKey {

        // APIs
        Hints("hints"),
        Autosuggest("autosuggest"),
        Search("search"),

        // Generic
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

        private String key = "";

        AnalyticsJsonKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        @Override
        public String toString() {
            return key;
        }
    }
}
