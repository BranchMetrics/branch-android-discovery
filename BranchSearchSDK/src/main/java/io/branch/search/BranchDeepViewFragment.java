package io.branch.search;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CircularProgressDrawable;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A dialog that can render deepviews.
 * Deepviews are renderer natively without using WebViews.
 */
public class BranchDeepViewFragment {

    public static final String TAG = "BranchDeepViewFragment";

    private static final String KEY_LINK = "link";
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();

    private static final String PLAY_STORE_APP_URL_PREFIX
            = "https://play.google.com/store/apps/details?id=";
    private static final String APP_ICON_URL_SMALL_SUFFIX = "=s90";

    @NonNull
    static DialogFragment getInstance(@NonNull BranchLinkResult link) {
        DialogFragment fragment = new Modern();
        Bundle args = new Bundle();
        args.putParcelable(KEY_LINK, link);
        fragment.setArguments(args);
        fragment.setCancelable(true);
        return fragment;
    }

    @Deprecated
    @NonNull
    static android.app.DialogFragment getLegacyInstance(@NonNull BranchLinkResult link) {
        android.app.DialogFragment fragment = new Legacy();
        Bundle args = new Bundle();
        args.putParcelable(KEY_LINK, link);
        fragment.setArguments(args);
        fragment.setCancelable(true);
        return fragment;
    }

    public static class Modern extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return createDialog(getContext());
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflateHierarchy(inflater, container);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setUpHierarchy(view, getArguments(), new Runnable() {
                @Override
                public void run() {
                    dismiss();
                }
            });
        }
    }

    @SuppressWarnings("deprecation")
    public static class Legacy extends android.app.DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return createDialog(getActivity());
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflateHierarchy(inflater, container);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setUpHierarchy(view, getArguments(), new Runnable() {
                @Override
                public void run() {
                    dismiss();
                }
            });
        }
    }

    private static Dialog createDialog(@NonNull Context context) {
        // We want to have all the default attributes based on the current runtime Context
        // (light vs. dark, rounded corners, window background, ...) but override a few ones
        // that are defined in the R.style.BranchDeepViewFragment.
        // First, find a good base context: (this is how the Dialog class does it)
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.dialogTheme, value, true);
        context = new ContextThemeWrapper(context, value.resourceId);
        // Now wrap it with our style:
        context = new ContextThemeWrapper(context, R.style.BranchDeepViewFragment);
        // Finally create the dialog:
        Dialog dialog = new Dialog(context, 0);
        // Apply our background:
        Drawable background = ContextCompat.getDrawable(context, R.drawable.branch_deepview_background);
        //noinspection ConstantConditions
        dialog.getWindow().setBackgroundDrawable(background);
        return dialog;
    }

    @NonNull
    private static View inflateHierarchy(@NonNull LayoutInflater inflater,
                                         @Nullable ViewGroup container) {
        return inflater.inflate(R.layout.branch_deepview, container, false);
    }

    private static void setUpHierarchy(@NonNull final View root,
                                       @NonNull Bundle args,
                                       @NonNull final Runnable dismissRunnable) {
        final BranchLinkResult link = args.getParcelable(KEY_LINK);
        if (link == null) return; // can't happen

        // App name
        TextView appName = root.findViewById(R.id.branch_deepview_app_name);
        if (appName != null) loadText(appName, link.getAppName());

        // App logo
        ImageView appIcon = root.findViewById(R.id.branch_deepview_app_icon);
        if (appIcon != null) {
            loadImage(appIcon, link.getAppIconUrl(), R.dimen.branch_deepview_app_icon_corners);
        }

        // Title
        TextView title = root.findViewById(R.id.branch_deepview_title);
        if (title != null) loadText(title, link.getName());

        // Description
        TextView description = root.findViewById(R.id.branch_deepview_description);
        if (description != null) loadText(description, link.getDescription());

        // Extra text
        TextView extra = root.findViewById(R.id.branch_deepview_extra);
        if (extra != null) loadText(extra, link.deepview_extra_text);

        // Image
        ImageView image = root.findViewById(R.id.branch_deepview_image);
        if (image != null) {
            String url = link.getImageUrl();
            if (url != null
                    && url.equals(link.getAppIconUrl())
                    && url.endsWith(APP_ICON_URL_SMALL_SUFFIX)) {
                // Remove the =s90 at the end of our app icon urls. This makes them 90x90
                // which is not suitable for fullscreen images.
                url = url.substring(0, url.length() - APP_ICON_URL_SMALL_SUFFIX.length());
            }
            loadImage(image, url, R.dimen.branch_deepview_image_corners);
        }

        // Button
        Button button = root.findViewById(R.id.branch_deepview_button);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = PLAY_STORE_APP_URL_PREFIX + link.getDestinationPackageName();
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    root.getContext().startActivity(intent);
                    dismissRunnable.run();
                }
            });
        } else {
            throw new IllegalStateException("Call to action button is missing!");
        }

        // Close button
        View close = root.findViewById(R.id.branch_deepview_close);
        if (close != null) {
            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismissRunnable.run();
                }
            });
        }
    }

    private static void loadText(@NonNull TextView textView, @Nullable String text) {
        if (TextUtils.isEmpty(text)) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(text);
        }
    }

    private static void loadImage(@NonNull final ImageView imageView,
                                  @Nullable String url,
                                  @DimenRes final int cornersRes) {
        Context context = imageView.getContext();
        final Resources resources = context.getResources();
        CircularProgressDrawable progress = new CircularProgressDrawable(context);
        progress.setArrowEnabled(false);
        progress.setCenterRadius(resources.getDimension(R.dimen.branch_deepview_loading_radius));
        progress.setStrokeWidth(resources.getDimension(R.dimen.branch_deepview_loading_stroke));
        progress.setColorSchemeColors(ContextCompat.getColor(context, R.color.branch_deepview_loading));
        progress.start();
        imageView.setImageDrawable(progress);
        HttpUrl httpUrl = url == null ? null : HttpUrl.parse(url);
        if (httpUrl == null) {
            imageView.setVisibility(View.GONE);
        } else {
            Request request = new Request.Builder().url(httpUrl).build();
            CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    imageView.post(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setVisibility(View.GONE);
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    InputStream stream = null;
                    try {
                        // Body is not null as per docs
                        //noinspection ConstantConditions
                        stream = response.body().byteStream();
                        final Bitmap bitmap = BitmapFactory.decodeStream(stream);
                        if (bitmap == null) { // Go to the 'catch' block.
                            throw new NullPointerException();
                        }
                        imageView.post(new Runnable() {
                            @Override
                            public void run() {
                                if (cornersRes != 0) {
                                    float corners = resources.getDimension(cornersRes);
                                    imageView.setImageDrawable(
                                            new RoundedCornersDrawable(bitmap, corners));
                                } else {
                                    imageView.setImageBitmap(bitmap);
                                }
                            }
                        });
                    } catch (Exception e) {
                        imageView.post(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setVisibility(View.GONE);
                            }
                        });
                    } finally {
                        if (stream != null) {
                            stream.close();
                        }
                        response.close();
                    }
                }
            });
        }
    }

    private static class RoundedCornersDrawable extends Drawable {
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path mPath = new Path();
        private final float mCorners;
        private final Bitmap mBitmap;
        private final RectF mRect = new RectF();

        private RoundedCornersDrawable(@NonNull Bitmap bitmap, float corners) {
            mBitmap = bitmap;
            mPaint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            mCorners = corners;
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            mRect.left = bounds.left;
            mRect.top = bounds.top;
            mRect.right = bounds.right;
            mRect.bottom = bounds.bottom;
        }

        @SuppressLint("CanvasSize")
        @Override
        public void draw(@NonNull Canvas canvas) {
            float cornerX = mCorners * (float) getIntrinsicWidth() / canvas.getWidth();
            float cornerY = mCorners * (float) getIntrinsicHeight() / canvas.getHeight();
            float corner = Math.max(cornerX, cornerY);
            mPath.rewind();
            mPath.addRoundRect(mRect, corner, corner, Path.Direction.CW);
            canvas.drawPath(mPath, mPaint);
        }

        @Override
        public int getIntrinsicHeight() {
            return mBitmap.getHeight();
        }

        @Override
        public int getIntrinsicWidth() {
            return mBitmap.getWidth();
        }
    }

    // Custom views that are helpful to ensure consistency of dialog height
    // among different results, displays, setups and font sizes.

    private static int getPercentMeasureSpec(@NonNull Resources resources,
                                             int heightMeasureSpec,
                                             float fraction) {
        int size = View.MeasureSpec.getSize(heightMeasureSpec);
        int mode = View.MeasureSpec.getMode(heightMeasureSpec);
        int target = (int) (fraction * resources.getDisplayMetrics().heightPixels);
        if (mode == View.MeasureSpec.AT_MOST) {
            target = Math.min(target, size);
        } else if (mode == View.MeasureSpec.EXACTLY) {
            target = size;
        }
        return View.MeasureSpec.makeMeasureSpec(target, View.MeasureSpec.EXACTLY);
    }

    /** An {@link ImageView} with height set to a fraction of display height. */
    public static class PercentImageView extends ImageView {
        public PercentImageView(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int spec = getPercentMeasureSpec(getResources(), heightMeasureSpec, 0.2F);
            super.onMeasure(widthMeasureSpec, spec);
        }
    }

    /** An {@link ScrollView} with height set to a fraction of display height. */
    public static class PercentScrollView extends ScrollView {
        public PercentScrollView(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int spec = getPercentMeasureSpec(getResources(), heightMeasureSpec, 0.45F);
            super.onMeasure(widthMeasureSpec, spec);
        }
    }
}