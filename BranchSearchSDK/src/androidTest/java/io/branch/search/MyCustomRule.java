package io.branch.search;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;

import io.branch.sdk.android.search.analytics.BranchAnalytics;
import io.branch.search.mock.MockActivity;

import static android.content.Intent.ACTION_PICK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class MyCustomRule<A extends MockActivity> extends ActivityTestRule<A> {
    MyCustomRule(Class<A> activityClass, boolean initialTouchMode, boolean launchActivity) {
        super(activityClass, initialTouchMode, launchActivity);
    }

    @Override
    protected void beforeActivityLaunched() {
        super.beforeActivityLaunched();
        Log.d("BranchAnalytics.MyCustomRule", "beforeActivityLaunched(), analytics_window_id = " + BranchAnalytics.getAnalyticsWindowId());
        // Maybe prepare some mock service calls
        // Maybe override some depency injection modules with mocks
    }

    @Override
    protected Intent getActivityIntent() {
        return new Intent(ACTION_PICK).setFlags(FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK);
    }

    @Override
    protected void afterActivityLaunched() {
        super.afterActivityLaunched();
        Log.d("BranchAnalytics.MyCustomRule", "afterActivityLaunched(), analytics_window_id = " + BranchAnalytics.getAnalyticsWindowId());
        // maybe you want to do something here
    }

    @Override
    protected void afterActivityFinished() {

        // Clean up mocks
    }
}