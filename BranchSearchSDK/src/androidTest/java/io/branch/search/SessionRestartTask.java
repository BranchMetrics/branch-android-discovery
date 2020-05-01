package io.branch.search;

import android.os.AsyncTask;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.fail;

/**
 * track data of all kinds, confirm that that it does disappear after session, confirm that it's in payload,
 * confirm empty session count works
 * Confirm demo app does not have access to getClickJson and the like
 * */

class SessionRestartTask extends AsyncTask<String, Void, Void> {
    private CountDownLatch signal;
    SessionRestartTask(CountDownLatch signal) {
        this.signal = signal;
    }

    @Override protected Void doInBackground(String... arg0) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail("InterruptedException observed in test: " + arg0[0]);
        }
        return null;
    }

    @Override protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        signal.countDown();
    }
}
