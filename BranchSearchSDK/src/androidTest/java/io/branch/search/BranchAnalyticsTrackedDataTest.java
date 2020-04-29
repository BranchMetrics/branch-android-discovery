package io.branch.search;

import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.branch.sdk.android.search.analytics.BranchAnalytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link BranchAnalytics} tests.
 */
@RunWith(AndroidJUnit4.class)
public class BranchAnalyticsTrackedDataTest extends BranchAnalyticsTest {

//    @Test
//    public void testTrackJSON() throws Throwable {
//        JSONObject payload = BranchAnalytics.getAnalyticsData();
//        assertFalse(payload.has("customJSON"));
//        JSONObject customJSON = new JSONObject().put("customKey", "customValue");
//        BranchAnalytics.trackObject("customJSON", customJSON);
//
//        payload = BranchAnalytics.getAnalyticsData();
//        assertTrue(payload.has("customJSON"));
//        assertEquals(customJSON, payload.optJSONObject("customJSON"));
//
//        testDataPrevailsSessionRestart("customJSON", false);
//    }
//
//    @Test public void testTrackArray() throws Throwable {
//        JSONObject payload = BranchAnalytics.getAnalyticsData();
//        assertFalse(payload.has("customArray"));
//        JSONArray customArray = new JSONArray(new String[]{"customKey", "customValue"});
//        BranchAnalytics.trackArray("customArray", customArray);
//
//        payload = BranchAnalytics.getAnalyticsData();
//        assertTrue(payload.has("customArray"));
//        assertEquals(customArray, payload.optJSONArray("customArray"));
//
//        testDataPrevailsSessionRestart("customArray", false);
//    }
//
//    @Test public void testTrackString() throws Throwable {
//        JSONObject payload = BranchAnalytics.getAnalyticsData();
//        String customString = "customString";
//        assertFalse(payload.has(customString));
//        BranchAnalytics.trackString("customString", customString);
//
//        payload = BranchAnalytics.getAnalyticsData();
//        assertTrue(payload.has("customString"));
//        assertEquals(customString, payload.optString("customString"));
//
//        assertTrue(testDataPrevailsSessionRestart("customString", false));
//    }
//
//    @Test public void testTrackInt() throws Throwable {
//        JSONObject payload = BranchAnalytics.getAnalyticsData();
//        int customInteger = 899;
//        assertFalse(payload.has("customInteger"));
//        BranchAnalytics.trackInt("customInteger", customInteger);
//
//        payload = BranchAnalytics.getAnalyticsData();
//        assertTrue(payload.has("customInteger"));
//        assertEquals(customInteger, payload.optInt("customInteger"));
//
//        testDataPrevailsSessionRestart("customInteger", false);
//    }
//
//    @Test public void testTrackDouble() throws Throwable {
//        JSONObject payload = BranchAnalytics.getAnalyticsData();
//        double customDouble = 899.7d;
//        assertFalse(payload.has("customDouble"));
//        BranchAnalytics.trackDouble("customDouble", customDouble);
//
//        payload = BranchAnalytics.getAnalyticsData();
//        assertTrue(payload.has("customDouble"));
//        assertEquals(customDouble, payload.optDouble("customDouble"), 0.0001);
//
//        testDataPrevailsSessionRestart("customDouble", false);
//    }

}
