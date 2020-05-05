package io.branch.search;

import android.os.AsyncTask;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * URLConnectionTask tests.
 */
@RunWith(AndroidJUnit4.class)
public class URLConnectionTaskTest {

    @Test
    public void testCancel_multipleCalls() throws Throwable {
        // To reproduce the issue in PR #62, apparently we must cancel quite a few
        // requests together. Before #62, this test fails (almost always). Now it passes.
        for (int i = 0; i < 100; i++) {
            testCancel_releasesSynchronously();
        }
    }

    @Test
    public void testCancel_releasesSynchronously() throws Throwable {
        // Create a client that sleeps for 10 seconds, using an OkHttp interceptor.
        final CountDownLatch connectionStart = new CountDownLatch(1);
        URLConnectionTask.sClient = new OkHttpClient.Builder()
                .callTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        connectionStart.countDown();
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ignore) {}
                        return chain.proceed(chain.request());
                    }
                })
                .build();

        // Create task and run.
        URLConnectionTask task = URLConnectionTask.forGet("https://fakeurl.fakeurl", BranchSearch.Channel.UNKNOWN.toString(),
                null);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        // Wait for the connection to be created.
        boolean wait = connectionStart.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(wait);

        // Sleep some extra ms just to ensure that we cancel while sleeping.
        Thread.sleep(50);
        Assert.assertNotNull(task.mCall);
        Assert.assertFalse(task.mCall.isCanceled());

        // Cancel task and ensure connection was released synchronously.
        task.cancel();
        Assert.assertTrue(task.mCall.isCanceled());
    }
}
