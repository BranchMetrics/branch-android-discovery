package io.branch.search;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.branch.sdk.android.search.analytics.BranchAnalytics;
import io.branch.search.mock.MockActivity;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.*;

/**
 * Base Instrumented test, which will execute on an Android device.
 */
@RunWith(AndroidJUnit4.class)
public class BranchAnalyticsTest {
    protected MockActivity mActivity;
    protected Context mContext;

    @Rule
    public MyCustomRule<MockActivity> mActivityRule = new MyCustomRule<>(MockActivity.class, false, false);

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mActivity = mActivityRule.launchActivity(mActivityRule.getActivityIntent());
    }

    void testDataPrevailsSessionRestart(String testName, String key, boolean expectToPrevail) throws Throwable {
        final CountDownLatch signal = new CountDownLatch(1);
        Log.d("BranchAnalytics", "testDataPrevailsSessionRestart, testName = " + testName);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("BranchAnalytics", "activity state: " + mActivity.state + ", analytics_window_id: " + BranchAnalytics.getAnalyticsWindowId());
                Log.d("BranchAnalytics", "callActivityOnPause");
                getInstrumentation().callActivityOnPause(mActivity);
                Log.d("BranchAnalytics", "activity state: " + mActivity.state + ", analytics_window_id: " + BranchAnalytics.getAnalyticsWindowId());
                Log.d("BranchAnalytics", "callActivityOnStop");
                getInstrumentation().callActivityOnStop(mActivity);
                Log.d("BranchAnalytics", "activity state: " + mActivity.state + ", analytics_window_id: " + BranchAnalytics.getAnalyticsWindowId());
            }
        });
        new SessionRestartTask(signal).execute(testName);

        assertTrue(signal.await(2000, TimeUnit.MILLISECONDS));

        JSONObject payloadAfterRestart = BranchAnalytics.getAnalyticsData();
        if (expectToPrevail) {
            assertTrue(payloadAfterRestart.has(key));
        } else {
            assertFalse(payloadAfterRestart.has(key));
        }
    }

    /**
     * track data of all kinds, confirm that that it does disappear after session, confirm that it's in payload,
     * confirm empty session count works
     * Confirm demo app does not have access to getClickJson and the like
     * */

    private class SessionRestartTask extends AsyncTask<String, Void, Void> {
        private CountDownLatch signal;
        SessionRestartTask(CountDownLatch signal) {
            this.signal = signal;
        }

        @Override protected Void doInBackground(String... arg0) {
            Log.d("BranchAnalytics", "doInBackground start, time = " + System.currentTimeMillis());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail("InterruptedException observed in test: " + arg0[0]);
            }
            Log.d("BranchAnalytics", "doInBackground end, time = " + System.currentTimeMillis());
            return null;
        }

        @Override protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            signal.countDown();
        }
    }

    @After
    public void tearDown() {
        Log.i("BranchAnalytics", "tearDown");
    }
}
