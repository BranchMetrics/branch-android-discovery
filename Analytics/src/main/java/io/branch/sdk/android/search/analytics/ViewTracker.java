package io.branch.sdk.android.search.analytics;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;

import static io.branch.sdk.android.search.analytics.BranchAnalytics.Logd;

/**
 * This class is responsible for checking {@link TrackedEntity} impressions on view.
 * See {@link BranchImpressionTracking}.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class ViewTracker {

    // Do not check if we have already checked < CHECK_TIME_MILLIS ago.
    private static final long CHECK_TIME_MILLIS = 80;

    // Do not count as impression if the visible area is less than 50% of the full view area.
    private static final float CHECK_AREA_MIN_FRACTION = 0.5F;

    private TrackedEntity mResult = null;
    private final View mView;
    private boolean mAttached = false;
    private boolean mHasImpression = false;
    private long mLastCheckMillis = 0L;
    private final ViewTreeListener mListener = new ViewTreeListener(this);

    ViewTracker(@NonNull View view) {
        mView = view;
        // No one will remove this attach state listener, but we don't need to.
        // This tracker is bound to the view and dies with it. No other tracker
        // will be bound to this view instance.
        mView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                onViewAttached();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                onViewDetached();
            }
        });
        if (ViewCompat.isAttachedToWindow(mView)) {
            onViewAttached();
        }
    }

    void bindTo(@NonNull TrackedEntity result) {
        mResult = result;
        mHasImpression = BranchImpressionTracking.hasTrackedImpression(result);
        Logd("ViewTracker.bindTo, TrackedEntity = [" + result.getImpressionJson() + "], mHasImpression: " + mHasImpression);
    }

    private void onViewAttached() {
        if (!mAttached) {
            // Now the ViewTreeObserver is the real one.
            mView.getViewTreeObserver().addOnScrollChangedListener(mListener);
            mView.getViewTreeObserver().addOnGlobalLayoutListener(mListener);
            mAttached = true;
        }
    }

    void onViewDetached() {
        if (mAttached) {
            // Tear down.
            mView.getViewTreeObserver().removeOnScrollChangedListener(mListener);
            mView.getViewTreeObserver().removeOnGlobalLayoutListener(mListener);
            mAttached = false;
        }
    }

    private final Rect mTempRect1 = new Rect();
    private final Rect mTempRect2 = new Rect();
    private final int[] mTempArray1 = new int[2];
    private final int[] mTempArray2 = new int[2];

    private void checkImpression() {
        long now = System.currentTimeMillis();
        if (!mHasImpression
                && mResult != null
                && mView.isShown()
                && ViewCompat.isLaidOut(mView)
                && now > mLastCheckMillis + CHECK_TIME_MILLIS) {
            mLastCheckMillis = now;

            // mRect1: Coordinates of the visible part of the view, in WINDOW coordinates.
            // mRect2: Coordinates of the visible part of the window, in DISPLAY coordinates.
            if (!mView.getGlobalVisibleRect(mTempRect1)) return;
            mView.getWindowVisibleDisplayFrame(mTempRect2);

            // There can be a big difference between the two coordinate systems, for example
            // when in multi-window mode. So before comparing the two rects, we must offset
            // the view rect by window left and top. To get these, we need to use a trick:
            mView.getLocationInWindow(mTempArray1);
            mView.getLocationOnScreen(mTempArray2);
            int windowLeft = mTempArray2[0] - mTempArray1[0];
            int windowTop = mTempArray2[1] - mTempArray1[1];
            mTempRect1.offset(windowLeft, windowTop);
            /* Logd("Checking impression for [" + mResult.getName() + "]"
                    + " viewHeight:" + mView.getHeight() + " viewVisibleHeight:" + mTempRect1.height() + " viewVisibleTop:" + mTempRect1.top
                    + " windowVisibleHeight:" + mTempRect2.height() + " windowVisibleTop:" + mTempRect2.top + " windowTop:" + windowTop); */

            // Intersect the two. This is needed to take the keyboard into account.
            boolean maybe = mTempRect1.intersect(mTempRect2);
            if (maybe) {
                float fullArea = (float) mView.getWidth() * mView.getHeight();
                float visibleArea = (float) mTempRect1.width() * mTempRect1.height();
                float percentage = visibleArea / fullArea;
                if (percentage > CHECK_AREA_MIN_FRACTION) {
                    mHasImpression = true;
                    BranchImpressionTracking.recordImpression(mResult, percentage);
                     Logd("Got impression for [" + mResult.getImpressionJson() + "]");
                } else {
//                     Logd("Missed impression for [" + mResult.getImpressionJson() + "]. Percentage: " + percentage
//                            + ". viewWidth:" + mView.getWidth() + " viewHeight:" + mView.getHeight()
//                            + ". visibleWidth:" + mTempRect1.width() + " visibleHeight:" + mTempRect1.height());
                }
            }
        }
    }

    /**
     * The purpose of this static class is to be extra safe against leaks.
     * This listener will be added to the global ViewTreeObserver which lives within the root view
     * of the whole app (ViewRootImpl) that has a longer lifecycle than the View we're tracking.
     *
     * We address this by unregistering this listener when the child View is detached, which should
     * cover ALL cases. However, to be extra safe, we also use this static class combined with
     * a WeakReference to the main tracker.
     */
    private static class ViewTreeListener implements
            ViewTreeObserver.OnScrollChangedListener,
            ViewTreeObserver.OnGlobalLayoutListener {
        private final WeakReference<ViewTracker> mTracker;

        private ViewTreeListener(@NonNull ViewTracker tracker) {
            mTracker = new WeakReference<>(tracker);
        }

        @Override
        public void onScrollChanged() {
            ViewTracker tracker = mTracker.get();
            if (tracker != null) tracker.checkImpression();
        }

        @Override
        public void onGlobalLayout() {
            ViewTracker tracker = mTracker.get();
            if (tracker != null) tracker.checkImpression();
        }
    }
}