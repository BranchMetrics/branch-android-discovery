package io.branch.search;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;

/**
 * URLConnection Network Handler.
 *
 * This class uses a {@link BranchExecutor} to schedule (through Android AsyncTasks) network
 * requests.
 *
 * As new requests are coming, the policy in this class is to schedule the new request, and then
 * cancel any pending request for the same type. Canceling pending requests is:
 *
 * - expensive - if the task is in the middle of network I/O, canceling can take time
 * - important - if we don't, we allow multiple requests of the same time, and this means
 *   we can't guarantee that the result callback is called in the correct order and this can
 *   cause UI issues.
 * - delicate in that we must ensure that it's not called on an UI thread.
 *
 * For this reason we do cancel running tasks but use the same {@link BranchExecutor} to schedule
 * the cancel operations. The cancel op. will free up a running thread for future requests.
 *
 * Each POST or GET request can trigger 2 (short-lived) thread requests.
 * The {@link BranchExecutor} pool size is set here to 6, which is just a maximum limit.
 * These threads are cached and only instantiated when needed.
 *
 * If we happen to need more than 6 threads at the same time, requests are enqueued by
 * {@link BranchExecutor} and will wait for currently running ops to finish, which would have
 * an impact on latency. But hopefully this should not happen too often, and some of these busy
 * threads will always be cancel requests that are executed much faster than network request.
 */
class URLConnectionNetworkHandler {

    private final BranchExecutor executor = new BranchExecutor(6);
    private final Object lock = new Object();

    private URLConnectionTask postTask;
    private URLConnectionTask getTask;
    private String channel;

    private URLConnectionNetworkHandler(@NonNull BranchSearch.Channel channel) {
        this.channel = channel.toString();
    }

    void executePost(@NonNull String url,
                     @NonNull JSONObject payload,
                     @Nullable IURLConnectionEvents callback) {
        synchronized (lock) {
            final URLConnectionTask oldTask = postTask;
            postTask = URLConnectionTask.forPost(url, channel, payload, callback);
            postTask.executeOnExecutor(executor);
            cancelTask(oldTask);
        }
    }

    void executeGet(@NonNull String url,
                    @Nullable IURLConnectionEvents callback) {
        synchronized (lock) {
            final URLConnectionTask oldTask = getTask;
            getTask = URLConnectionTask.forGet(url, channel, callback);
            getTask.executeOnExecutor(executor);
            cancelTask(oldTask);
        }
    }

    private void cancelTask(final @Nullable URLConnectionTask task) {
        if (task == null) return;
        boolean isUiThread = Thread.currentThread() == Looper.getMainLooper().getThread();
        // If we're on the UI thread, we can't / shouldn't cancel a network op on the UI thread.
        // If not, we can cancel on current thread without adding workload to the executor.
        if (isUiThread) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    task.cancel();
                }
            });
        } else {
            task.cancel();
        }
    }

    /**
     * Create an instance of the NetworkHandler.
     * @return a new URLConnectionNetworkHandler
     */
    @NonNull
    static URLConnectionNetworkHandler initialize(@NonNull BranchSearch.Channel channel) {
        return new URLConnectionNetworkHandler(channel);
    }

}
