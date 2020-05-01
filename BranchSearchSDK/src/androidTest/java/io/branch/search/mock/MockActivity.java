package io.branch.search.mock;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MockActivity extends Activity {
    private static final String TAG = "BranchAnalytics";
    public String state;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MockActivity Created");
        state = "created";
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "MockActivity Started");
        state = "started";
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "MockActivity Resumed");
        state = "resumed";
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "MockActivity Paused");
        state = "paused";
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "MockActivity Stopped");
        state = "stopped";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MockActivity Destroyed");
        state = "destroyed";
    }
}
