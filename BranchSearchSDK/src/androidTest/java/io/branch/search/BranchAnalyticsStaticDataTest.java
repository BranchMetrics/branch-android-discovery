package io.branch.search;

import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.branch.sdk.android.search.analytics.BranchAnalytics;

import static io.branch.sdk.android.search.analytics.Defines.AnalyticsWindowId;
import static io.branch.sdk.android.search.analytics.Defines.ConfigInfo;
import static io.branch.sdk.android.search.analytics.Defines.DeviceInfo;
import static io.branch.sdk.android.search.analytics.Defines.EmptySessions;
import static io.branch.search.BranchConfiguration.JSONKey.BranchKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link BranchAnalytics} tests.
 */
@RunWith(AndroidJUnit4.class)
public class BranchAnalyticsStaticDataTest extends BranchAnalyticsTest {

    @Test public void testDefaultStaticValues() {
        // device info, config info, empty session count, analytics_window_id should exist in the payload by default
        JSONObject payload = BranchAnalytics.getAnalyticsData();

        // analytics window ID
        assertEquals(payload.optString(AnalyticsWindowId), BranchAnalytics.getAnalyticsWindowId());

        // ConfigInfo exists
        JSONObject payloadConfig = payload.optJSONObject(ConfigInfo);
        assertNotNull(payloadConfig);
        // ConfigInfo contains the right branch key
        assertEquals(BranchSearch.getInstance().getBranchConfiguration().getBranchKey(), payloadConfig.optString(BranchKey.toString()));

        // DeviceInfo exists
        JSONObject payloadDeviceInfo = payload.optJSONObject(DeviceInfo);
        assertNotNull(payloadDeviceInfo);

        // empty sessions
        assertTrue(payload.has(EmptySessions));
        assertEquals(0, payload.optInt(EmptySessions));
    }

    @Test public void testAddStaticJSON() throws Throwable {
        JSONObject payload = BranchAnalytics.getAnalyticsData();
        assertFalse(payload.has("customJSON"));
        JSONObject customJSON = new JSONObject().put("customKey", "customValue");
        BranchAnalytics.addObject("customJSON", customJSON);

        payload = BranchAnalytics.getAnalyticsData();
        assertTrue(payload.has("customJSON"));
        assertEquals(customJSON, payload.optJSONObject("customJSON"));

        testDataPrevailsSessionRestart("customJSON", true);
        testRemoveAddedStaticData("customJSON", customJSON);
    }

    @Test public void testAddStaticArray() throws Throwable {
        JSONObject payload = BranchAnalytics.getAnalyticsData();
        assertFalse(payload.has("customArray"));
        JSONArray customArray = new JSONArray(new String[]{"customKey", "customValue"});
        BranchAnalytics.addArray("customArray", customArray);

        payload = BranchAnalytics.getAnalyticsData();
        assertTrue(payload.has("customArray"));
        assertEquals(customArray, payload.optJSONArray("customArray"));

        testDataPrevailsSessionRestart("customArray", true);
        testRemoveAddedStaticData("customArray", customArray);
    }

    @Test public void testAddStaticString() throws Throwable {
        JSONObject payload = BranchAnalytics.getAnalyticsData();
        String customString = "customString";
        assertFalse(payload.has(customString));
        BranchAnalytics.addString("customString", customString);

        payload = BranchAnalytics.getAnalyticsData();
        assertTrue(payload.has("customString"));
        assertEquals(customString, payload.optString("customString"));

        testDataPrevailsSessionRestart("customString", true);
        testRemoveAddedStaticData("customString", customString);
    }

    @Test public void testAddStaticInt() throws Throwable {
        JSONObject payload = BranchAnalytics.getAnalyticsData();
        int customInteger = 899;
        assertFalse(payload.has("customInteger"));
        BranchAnalytics.addInt("customInteger", customInteger);

        payload = BranchAnalytics.getAnalyticsData();
        assertTrue(payload.has("customInteger"));
        assertEquals(customInteger, payload.optInt("customInteger"));

        testDataPrevailsSessionRestart("customInteger", true);
        testRemoveAddedStaticData("customInteger", customInteger);
    }

    @Test public void testAddStaticDouble() throws Throwable {
        JSONObject payload = BranchAnalytics.getAnalyticsData();
        double customDouble = 899.7d;
        assertFalse(payload.has("customDouble"));
        BranchAnalytics.addDouble("customDouble", customDouble);

        payload = BranchAnalytics.getAnalyticsData();
        assertTrue(payload.has("customDouble"));
        assertEquals(customDouble, payload.optDouble("customDouble"), 0.0001);

        testDataPrevailsSessionRestart("customDouble", true);
        testRemoveAddedStaticData("customDouble", customDouble);
    }
}
