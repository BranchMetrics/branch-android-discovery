package io.branch.search;

import android.app.Instrumentation;
import android.arch.lifecycle.Lifecycle;
import android.os.Handler;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import io.branch.sdk.android.search.analytics.BranchAnalytics;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.*;

/**
 * {@link BranchAnalytics} tests.
 */
@RunWith(AndroidJUnit4.class)
public class BranchAnalyticsTest extends BranchTest {
    private Instrumentation instrumentation;

    @Before public void setUp() throws Throwable {
        super.setUp();
        initBranch();
        instrumentation = getInstrumentation();
    }

    void testDataPrevailsSessionRestart(String key, boolean expectToPrevail) throws Throwable {
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

//    void testDataPrevailsSessionRestart(final String key, final boolean expectToPrevail) throws Throwable {
//        mActivityRule.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                instrumentation.callActivityOnPause(mActivity);
//                instrumentation.callActivityOnStop(mActivity);
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        instrumentation.callActivityOnRestart(mActivity);
//                        instrumentation.callActivityOnStart(mActivity);
//
//                        JSONObject payloadAfterRestart = BranchAnalytics.getAnalyticsData();
//                        if (expectToPrevail) {
//                            assertTrue(payloadAfterRestart.has(key));
//                        } else {
//                            assertFalse(payloadAfterRestart.has(key));
//                        }
//                    }
//                }, 700);
//
//            }
//        });
//    }

    void testRemoveAddedStaticData(String key, Object jsonCompliantObject) {
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

    /**
     * track data of all kinds, confirm that that it does disappear after session, confirm that it's in payload,
     * confirm empty session count works
     * Confirm demo app does not have access to getClickJson and the like
     * */

    @After public void tearDown() {
        super.tearDown();
//        instrumentation = null;
    }
}
