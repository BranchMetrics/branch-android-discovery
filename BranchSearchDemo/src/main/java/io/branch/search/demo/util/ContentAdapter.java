package io.branch.search.demo.util;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import io.branch.sdk.android.search.analytics.BranchAnalytics;
import io.branch.sdk.android.search.analytics.TrackedEntity;
import io.branch.search.BranchAppResult;
import io.branch.search.BranchLinkResult;
import io.branch.search.BranchSearchResult;


/**
 * Adapter for listing contents from both recommendations and search results
 */

public class ContentAdapter extends BaseAdapter {
    private List<Object> branchContents_;
    private final Context context_;
    private String query;

    ContentAdapter(Context context) {
        this.context_ = context;
        branchContents_ = new ArrayList<>();
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getCount() {
        return branchContents_.size();
    }

    @Override
    public Object getItem(int position) {
        return branchContents_.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = new ContentItem(context_);
        }
        Object result = branchContents_.get(position);
        if (result instanceof BranchLinkResult) {
            BranchLinkResult linkResult = (BranchLinkResult)result;
            ((ContentItem) convertView).showContent(query, linkResult);
        } else {
            // Load app header
            BranchAppResult appResult = (BranchAppResult)result;
            ((ContentItem) convertView).showAppHeader(appResult);
        }
        BranchAnalytics.trackImpressions(convertView, (TrackedEntity) result);
        return convertView;
    }

    public void updateUIWithContentSearchResult(BranchSearchResult branchSearchResult) {
        this.query = branchSearchResult.getBranchSearchRequest().getQuery();
        this.branchContents_ = getGroupedList(branchSearchResult.getResults());
        notifyDataSetChanged();
    }

    private List<Object> getGroupedList(List<BranchAppResult> searchContentList) {
        List<Object> groupedResult = new ArrayList<>();

        for (BranchAppResult appResult : searchContentList) {
           if (appResult.getDeepLinks().size() > 0) {
               groupedResult.add(appResult);
               groupedResult.addAll(appResult.getDeepLinks());
           }
        }

        return groupedResult;
    }

}
