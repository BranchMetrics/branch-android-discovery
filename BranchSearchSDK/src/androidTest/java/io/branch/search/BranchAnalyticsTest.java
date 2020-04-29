package io.branch.search;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.branch.sdk.android.search.analytics.BranchAnalytics;
import io.branch.search.mock.MockActivity;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static io.branch.sdk.android.search.analytics.Defines.*;
import static io.branch.search.BranchConfiguration.JSONKey.BranchKey;

import static org.junit.Assert.*;

/**
 * {@link BranchAutoSuggestRequest} tests.
 */
@RunWith(AndroidJUnit4.class)
public class BranchAnalyticsTest extends BranchTest {
//    private MockActivity mockActivity;
    private Instrumentation instrumentation;

    @Before public void setUp() throws Throwable {
        super.setUp();
        initBranch();
        instrumentation = getInstrumentation();
    }

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

        testAddedStaticDataPrevailsSessionRestart("customJSON");
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

        testAddedStaticDataPrevailsSessionRestart("customArray");
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

        testAddedStaticDataPrevailsSessionRestart("customString");
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

        testAddedStaticDataPrevailsSessionRestart("customInteger");
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

        testAddedStaticDataPrevailsSessionRestart("customDouble");
        testRemoveAddedStaticData("customDouble", customDouble);
    }

    private void testAddedStaticDataPrevailsSessionRestart(String key) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                instrumentation.callActivityOnPause(mActivity);
                instrumentation.callActivityOnStop(mActivity);
                instrumentation.callActivityOnRestart(mActivity);
            }
        });
        JSONObject payloadAfterRestart = BranchAnalytics.getAnalyticsData();
        payloadAfterRestart.has(key);
    }

    private void testRemoveAddedStaticData(String key, Object jsonCompliantObject) {
        assertTrue(BranchAnalytics.getAnalyticsData().has(key));

        if (jsonCompliantObject instanceof JSONObject) {
            BranchAnalytics.addObject(key, null);
        } else if (jsonCompliantObject instanceof JSONArray) {
            BranchAnalytics.addArray(key, null);
        } else if (jsonCompliantObject instanceof Integer) {
            BranchAnalytics.addInt(key, null);
        } else if (jsonCompliantObject instanceof Double) {
            BranchAnalytics.addDouble(key, null);
        } else if (jsonCompliantObject instanceof String) {
            BranchAnalytics.addString(key, null);
        }

        assertFalse(BranchAnalytics.getAnalyticsData().has(key));

        if (jsonCompliantObject instanceof JSONObject) {
            BranchAnalytics.addObject(key, (JSONObject) jsonCompliantObject);
        } else if (jsonCompliantObject instanceof JSONArray) {
            BranchAnalytics.addArray(key, (JSONArray) jsonCompliantObject);
        } else if (jsonCompliantObject instanceof Integer) {
            BranchAnalytics.addInt(key, (Integer) jsonCompliantObject);
        } else if (jsonCompliantObject instanceof Double) {
            BranchAnalytics.addDouble(key, (Double) jsonCompliantObject);
        } else if (jsonCompliantObject instanceof String) {
            BranchAnalytics.addString(key, (String) jsonCompliantObject);
        }

        assertTrue(BranchAnalytics.getAnalyticsData().has(key));

        BranchAnalytics.clearStaticValues();
        assertFalse(BranchAnalytics.getAnalyticsData().has(key));
    }
//
//    @Test public void testAddStaticJSON() {
//
//    }

    /**
     * addStaticData, confirm that it there , confirm that it does not disappear after session, confirm individual key deletion, confirm clearStaticData works
     * track data of all kinds, confirm that that it does disappear after session, confirm that it's in payload,
     * confirm empty session count works
     * Confirm demo app does not have access to getClickJson and the like
     * */

    @After public void tearDown() {
        super.tearDown();
        instrumentation = null;
    }
}
