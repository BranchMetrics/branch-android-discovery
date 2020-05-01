package io.branch.search;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.branch.sdk.android.search.analytics.BranchAnalytics;
import io.branch.search.mock.MockActivity;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.*;

public class BranchAnalyticsTest {
    private MockActivity mActivity;

    @Rule
    public ActivityTestRule<MockActivity> mActivityRule = new ActivityTestRule<>(MockActivity.class, false, false);

    @BeforeClass
    public static void init() {
        BranchAnalytics.enableLogging(true);
    }

    @Before
    public void setUp() {
        mActivity = mActivityRule.launchActivity(new Intent(Intent.ACTION_VIEW).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK));
        BranchSearch.init(mActivityRule.getActivity());
    }

    void testDataPrevailsSessionRestart(String testName, String key, boolean expectToPrevail) throws Throwable {
        getInstrumentation().getTargetContext();

        BranchAnalytics.dump();
        // TODO the code below tests the lifecycle observer, it uses blocking because our observer's
        //  onPause/onStop callbacks are delayed to account for rotation changes. The code works when
        //  the test is run on it's own but not when entire suite of tests are run together. Need to
        //  fix it. In the meantime BranchAnalytics.dump() mimicks the behavior after onPause/onStop.
//        final CountDownLatch signal = new CountDownLatch(1);
//        mActivity.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mActivity.onPause();
//                mActivity.onStop();
////                getInstrumentation().callActivityOnPause(mActivity);
////                getInstrumentation().callActivityOnStop(mActivity);
//            }
//        });
//        new SessionRestartTask(signal).execute(testName);
//        assertTrue(signal.await(2000, TimeUnit.MILLISECONDS));

        JSONObject payloadAfterRestart = BranchAnalytics.getAnalyticsData();
        if (expectToPrevail) {
            assertTrue(payloadAfterRestart.has(key));
        } else {
            assertFalse(payloadAfterRestart.has(key));
        }
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

        testDataPrevailsSessionRestart( "testTrackArray", "customArray", false);
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

        testDataPrevailsSessionRestart( "testTrackDouble", "customDouble", false);
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

        testDataPrevailsSessionRestart( "testTrackInt", "customInt", false);
    }

    @Test
    public void testTrackObject() throws Throwable {
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

}
