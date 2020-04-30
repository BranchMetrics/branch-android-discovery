package io.branch.search;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.branch.sdk.android.search.analytics.BranchAnalytics;
import io.branch.search.mock.MockActivity;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Base Instrumented test, which will execute on an Android device.
 */
@RunWith(AndroidJUnit4.class)
public class BranchAnalyticsTrackedDataTest extends BranchAnalyticsTest {

    @Test
    public void testTrackJSON() throws Throwable {
        JSONObject payload = BranchAnalytics.getAnalyticsData();
        assertFalse(payload.has("customJSON"));
        JSONObject customJSON = new JSONObject().put("customKey", "customValue");
        BranchAnalytics.trackObject("customJSON", customJSON);

        payload = BranchAnalytics.getAnalyticsData();
        assertTrue(payload.has("customJSON"));
        assertEquals(customJSON, payload.optJSONObject("customJSON"));

        testDataPrevailsSessionRestart("testTrackJSON", "customJSON", false);
    }

    @Test
    public void testTrackArray() throws Throwable {
        JSONObject payload = BranchAnalytics.getAnalyticsData();
        assertFalse(payload.has("customArray"));
        JSONArray customArray = new JSONArray(new String[]{"customKey", "customValue"});
        BranchAnalytics.trackArray("customArray", customArray);

        payload = BranchAnalytics.getAnalyticsData();
        assertTrue(payload.has("customArray"));
        assertEquals(customArray, payload.optJSONArray("customArray"));

        testDataPrevailsSessionRestart("testTrackArray", "customArray", false);
    }

    @Test
    public void testTrackString() throws Throwable {
        JSONObject payload = BranchAnalytics.getAnalyticsData();
        String customString = "customString";
        assertFalse(payload.has(customString));
        BranchAnalytics.trackString("customString", customString);

        payload = BranchAnalytics.getAnalyticsData();
        assertTrue(payload.has("customString"));
        assertEquals(customString, payload.optString("customString"));

        testDataPrevailsSessionRestart("testTrackString", "customString", false);
    }

    @Test
    public void testTrackDouble() throws Throwable {
        JSONObject payload = BranchAnalytics.getAnalyticsData();
        double customDouble = 899.7d;
        assertFalse(payload.has("customDouble"));
        BranchAnalytics.trackDouble("customDouble", customDouble);

        payload = BranchAnalytics.getAnalyticsData();
        assertTrue(payload.has("customDouble"));
        assertEquals(customDouble, payload.optDouble("customDouble"), 0.0001);

        testDataPrevailsSessionRestart("testTrackDouble", "customDouble", false);
    }

    @Test
    public void testTrackInt() throws Throwable {
        JSONObject payload = BranchAnalytics.getAnalyticsData();
        int customInt = 843;
        assertFalse(payload.has("customInt"));
        BranchAnalytics.trackInt("customInt", customInt);

        payload = BranchAnalytics.getAnalyticsData();
        assertTrue(payload.has("customInt"));
        assertEquals(customInt, payload.optInt("customInt"));

        testDataPrevailsSessionRestart("testTrackInt", "customInt", false);
    }
}
