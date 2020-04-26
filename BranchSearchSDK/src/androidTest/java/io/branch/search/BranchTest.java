package io.branch.search;

import android.app.Activity;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.branch.search.mock.MockActivity;

/**
 * Base Instrumented test, which will execute on an Android device.
 */
@RunWith(AndroidJUnit4.class)
public class BranchTest {
    private Context mContext;
    private Activity mActivity;

    @Rule
    public ActivityTestRule<MockActivity> mActivityRule =
            new ActivityTestRule<>(MockActivity.class);

    @Before
    public void setUp() throws Throwable {
        mContext = InstrumentationRegistry.getTargetContext();
        mActivity = mActivityRule.getActivity();
    }

    @After
    public void tearDown() {
        mContext = null;
        mActivity.finish();
        mActivity = null;
    }

    @Test
    public void testAppContext() {
        // Context of the app under test.
        Assert.assertNotNull(getTestContext());

        // Context of the app as an Activity
        Assert.assertNotNull(getUIContext());

        // The two Context Objects are theoretically not the same
        Assert.assertNotSame(getTestContext(), getUIContext());
    }

    @Test
    public void testPackageName() {
        // Context of the app under test.
        Context appContext = getTestContext();

        Assert.assertEquals("io.branch.search.test", appContext.getPackageName());
    }

    /**
     * Initialize Branch.
     * This will initialize Branch on the Main Thread, and assert success.
     */
    void initBranch() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BranchSearch search = BranchSearch.init(getUIContext());
                Assert.assertNotNull(search);
            }
        });
    }

    /**
     * Initialize Branch with a custom configuration.
     * Test the state of initialization by calling {@link BranchSearch#getInstance()}
     */
    void initBranch(final BranchConfiguration config) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BranchSearch.init(getUIContext(), config);
            }
        });
    }

    Context getTestContext() {
        return mContext;
    }

    private Context getUIContext() {
        return mActivity;
    }
}