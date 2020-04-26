package io.branch.search.demo.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.AppCompatRatingBar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import io.branch.search.BranchAppResult;
import io.branch.search.BranchLinkResult;
import io.branch.search.demo.R;

/**
 * View for a content result item
 */
public class ContentItem extends LinearLayout implements View.OnClickListener, View.OnLongClickListener {

    private Context context = null;
    private TextView titleText_, descTxt_;
    private ImageView imgView_, imageView_Apps;
    private RelativeLayout contentItem_;
    private TextView headerItemTextView_;
    private TextView searchItemView_;
    private View appsTitleCover;
    private String reviewFormat;

    private static final int regularContentImgSize = 80;


    public ContentItem(Context context) {
        super(context);
        init(context);
    }

    public ContentItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ContentItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context activity) {
        this.context = activity;
        //noinspection deprecation
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(activity).inflate(R.layout.feed_item, null, false);
        titleText_ = view.findViewById(R.id.title_txt);
        descTxt_ = view.findViewById(R.id.description_txt);
        imgView_ = view.findViewById(R.id.content_img);
        imageView_Apps = view.findViewById(R.id.content_img_apps);
        contentItem_ = view.findViewById(R.id.content_item);
        headerItemTextView_ = view.findViewById(R.id.header_item_textview);
        searchItemView_ = view.findViewById(R.id.search_item_view);
        appsTitleCover = view.findViewById(R.id.app_title_cover);
        reviewFormat = getResources().getString(R.string.review_count);
        addView(view);

    }

    public void showContent(String query, BranchLinkResult contentResult) {
        if (ContentDisplayHelper.isMoreSearchInSpecificAppItem(contentResult)) {
            contentItem_.setVisibility(GONE);
            searchItemView_.setVisibility(VISIBLE);
            searchItemView_.setOnClickListener(this);
            searchItemView_.setOnLongClickListener(this);
            searchItemView_.setTag(contentResult);
            searchItemView_.setText(ContentDisplayHelper.getFormattedSearchTitle(getContext(), query, contentResult));

        } else {
            contentItem_.setVisibility(VISIBLE);
            searchItemView_.setVisibility(GONE);

            imgView_.setVisibility(INVISIBLE);
            imageView_Apps.setVisibility(VISIBLE);
            appsTitleCover.setVisibility(VISIBLE);
            ContentDisplayHelper.loadContentImage(context, contentResult, imageView_Apps, regularContentImgSize);

            titleText_.setText(contentResult.getName());
            descTxt_.setText(contentResult.getDescription());
            contentItem_.setTag(contentResult);
            descTxt_.setVisibility(View.VISIBLE);

//            findViewById(R.id.rating_bar).setVisibility(GONE);
//            findViewById(R.id.review_txt).setVisibility(GONE);

            ContentDisplayReviewHelper reviewHelper = new ContentDisplayReviewHelper(contentResult);
            if (reviewHelper.isRatingAvailable()) {
                AppCompatRatingBar ratingBar = findViewById(R.id.rating_bar);
                ratingBar.getProgressDrawable().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_ATOP);
                ratingBar.setMax(reviewHelper.getRatingScale());
                ratingBar.setStepSize(0.1f);
                ratingBar.setRating((float) reviewHelper.getRating());
                ratingBar.setVisibility(VISIBLE);
            } else {
                findViewById(R.id.rating_bar).setVisibility(GONE);
            }


            // If there are reviews available, try to get the review count.
            if (reviewHelper.isRatingAvailable() && reviewHelper.isReviewAvailable()) {
                TextView ratingTxt = findViewById(R.id.review_txt);
                String ratingStr = String.format(reviewFormat, reviewHelper.getReviewCount());
                ratingTxt.setText(ratingStr);
                ratingTxt.setVisibility(VISIBLE);
            } else {
                findViewById(R.id.review_txt).setVisibility(GONE);
            }

//            ControlContentItemTextWrap(0, 0);
//            ControlContentItemTextWrap(contentResult.getRating(), contentResult.getReviewCount());
        }

        contentItem_.setOnClickListener(this);
        contentItem_.setOnLongClickListener(this);
        headerItemTextView_.setVisibility(View.GONE);
    }

    public void showAppHeader(BranchAppResult appResult) {
        final String title = appResult.getAppName();

        contentItem_.setVisibility(View.GONE);
        searchItemView_.setVisibility(View.GONE);
        headerItemTextView_.setPadding(0, DimensionMatrix.HEADER_VIEW_TOP_MARGIN, 0, DimensionMatrix.HEADER_VIEW_TOP_MARGIN);
        headerItemTextView_.setText(title);

        Glide.with(this)
                .asBitmap()
                .load(appResult.getAppIconUrl())
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        SpannableStringBuilder searchSpannableBuilder = new SpannableStringBuilder("\t\t" + title);
                        Drawable searchImg = new BitmapDrawable(getResources(), resource);
                        searchImg.setBounds(0, 0, DimensionMatrix.APP_SMALL_ICON_SIZE, DimensionMatrix.APP_SMALL_ICON_SIZE);
                        searchSpannableBuilder.setSpan(new BranchCustomImageSpan(searchImg, BranchCustomImageSpan.ALIGN_CENTER), 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                        headerItemTextView_.setText(searchSpannableBuilder);

                    }
                });

        headerItemTextView_.setVisibility(View.VISIBLE);

        this.setOnClickListener(this);
        this.setOnLongClickListener(this);
        this.setTag(appResult);
    }


    @Override
    public void onClick(View v) {
        Object result = v.getTag();
        if (result instanceof BranchLinkResult) {
            BranchLinkResult linkResult = (BranchLinkResult)result;
            linkResult.openContent(getContext(), true);
        } else {
            // Load app header
            BranchAppResult appResult = (BranchAppResult)result;
            appResult.openSearchDeepLink(getContext(), true);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        Object result = v.getTag();
        if (result instanceof BranchLinkResult) {
            BranchLinkResult linkResult = (BranchLinkResult)result;
            linkResult.openDeepView(((FragmentActivity) getContext()).getSupportFragmentManager());
            return true;
        }
        return false;
    }

    private void ControlContentItemTextWrap(float rating, int reviewCnt) {
        int stdTitleLineCnt = 1;
        int stdDescLineCnt = 1;
        if (rating == 0 && reviewCnt == 0) {
            stdDescLineCnt += 1;
        }
        titleText_.setMaxLines(stdTitleLineCnt);
        descTxt_.setMaxLines(stdDescLineCnt);
    }

}