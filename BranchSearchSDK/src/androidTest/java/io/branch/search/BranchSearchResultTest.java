package io.branch.search;

import android.support.test.runner.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import io.branch.search.util.AssetUtils;

/**
 * SearchResult Tests.
 */
@RunWith(AndroidJUnit4.class)
public class BranchSearchResultTest extends BranchTest {

    @Before
    public void setUp() throws Throwable {
        super.setUp();
        initBranch();
    }

    @Test
    public void testResultSuccess() throws Throwable {
        String response = AssetUtils.readJsonFile(getTestContext(), "success_mex_food.json");
        Assert.assertTrue(response.length() > 0);

        JSONObject jsonResponse = new JSONObject(response);

        BranchSearchRequest request = BranchSearchRequest.create("Mexican");
        BranchSearchResult result = BranchSearchResult.createFromJson(request, jsonResponse);
        Assert.assertNotNull(result);

        // Check to see if there is some expected information in the result.
        List<BranchAppResult> appResults = result.getResults();
        Assert.assertTrue(appResults.size() > 0);

        for (BranchAppResult appResult : appResults) {
            testAppResult(appResult);
        }
    }

    @Test
    public void testResultSuccess_empty1() throws Throwable {
        String response = AssetUtils.readJsonFile(getTestContext(), "success_empty1.json");
        Assert.assertTrue(response.length() > 0);

        JSONObject jsonResponse = new JSONObject(response);

        BranchSearchRequest request = BranchSearchRequest.create("Mexican");
        BranchSearchResult result = BranchSearchResult.createFromJson(request, jsonResponse);
        Assert.assertNotNull(result);

        // This has no App Results.
        List<BranchAppResult> appResults = result.getResults();
        Assert.assertEquals(0, appResults.size());
    }

    @Test
    public void testResultSuccess_empty2() throws Throwable {
        String response = AssetUtils.readJsonFile(getTestContext(), "success_empty2.json");
        Assert.assertTrue(response.length() > 0);

        JSONObject jsonResponse = new JSONObject(response);

        BranchSearchRequest request = BranchSearchRequest.create("Mexican");
        BranchSearchResult result = BranchSearchResult.createFromJson(request, jsonResponse);
        Assert.assertNotNull(result);

        // This has One App Result, but nothing inside.
        // This is not allowed anymore, so list should be empty.
        List<BranchAppResult> appResults = result.getResults();
        Assert.assertTrue(appResults.isEmpty());
    }

    @Test
    public void testResultSuccess_empty3() {
        // Parse with an empty JSONObject
        BranchSearchRequest request = BranchSearchRequest.create("Mexican");
        BranchSearchResult result = BranchSearchResult.createFromJson(request, new JSONObject());
        Assert.assertNotNull(result);
    }

    @Test
    public void testResultSuccess_null() throws Throwable {
        String response = AssetUtils.readJsonFile(getTestContext(), "success_null_everywhere.json");
        Assert.assertTrue(response.length() > 0);

        JSONObject jsonResponse = new JSONObject(response);

        BranchSearchRequest request = BranchSearchRequest.create("Mexican");
        BranchSearchResult result = BranchSearchResult.createFromJson(request, jsonResponse);
        Assert.assertNotNull(result);

        // Since results have no app id, the "not installed" filter will remove them
        List<BranchAppResult> appResults = result.getResults();
        Assert.assertTrue(appResults.isEmpty());
    }

    @Test
    public void testResultError() throws Throwable {
        String response = AssetUtils.readJsonFile(getTestContext(), "err_region.json");
        Assert.assertTrue(response.length() > 0);

        JSONObject jsonObject = new JSONObject(response);

        BranchSearchError error = new BranchSearchError(jsonObject.getJSONObject("error"));
        Assert.assertEquals(error.getErrorCode(), BranchSearchError.ERR_CODE.BAD_REQUEST_ERR);
    }

    private void testAppResult(BranchAppResult result) {
        Assert.assertNotNull(result.getAppIconUrl());
        Assert.assertNotNull(result.getAppName());
        Assert.assertNotNull(result.getPackageName());
        Assert.assertNotNull(result.getRankingHint());
        Assert.assertTrue(result.getScore() >= 0.0f);

        Assert.assertTrue(result.getDeepLinks().size() > 0);
        for (BranchLinkResult linkResult : result.getDeepLinks()) {
            testDeeplinkResult(linkResult);
        }
    }

    private void testDeeplinkResult(BranchLinkResult link) {
        Assert.assertNotNull(link.getEntityID());
        Assert.assertNotNull(link.getName());
        Assert.assertNotNull(link.getDescription());
        Assert.assertNotNull(link.getImageUrl());
        Assert.assertNotNull(link.getAppIconUrl());
        Assert.assertNotNull(link.getType());
        Assert.assertTrue(link.getScore() >= 0.0f);
        Assert.assertNotNull(link.getMetadata());
        Assert.assertNotNull(link.getRoutingMode());
        Assert.assertNotNull(link.getWebLink());
        Assert.assertNotNull(link.getDestinationPackageName());
        Assert.assertNotNull(link.getClickTrackingUrl());
        Assert.assertNotNull(link.getRankingHint());
    }
}
