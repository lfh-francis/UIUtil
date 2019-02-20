package com.d.lib.ui.view.advertswitcher;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ViewSwitcher;

import com.d.lib.common.utils.log.ULog;
import com.d.lib.ui.view.R;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * AdvertSwitcher
 */
@RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
public class AdvertSwitcher extends ViewSwitcher {
    private final static int FLAG_SCROLL = 0;

    private final static int DEFAULT_TIME_SPAN = 3000; // Default time interval
    private final static int DEFAULT_IN_ANIM_ID = R.anim.lib_ui_view_anim_advert_in;
    private final static int DEFAULT_OUT_ANIM_ID = R.anim.lib_ui_view_anim_advert_out;
    private final static int DEFAULT_INTERPOLATOR = android.R.interpolator.linear;

    private Context mContext;
    private long mTimeSpan;
    private int mInAnimId;
    private int mOutAnimId;
    private int mInterpolator;
    private int mCurIndex;
    private Adapter mAdapter;
    private Handler mHandler;

    private static class ScrollHandler extends Handler {
        private WeakReference<AdvertSwitcher> reference;

        ScrollHandler(AdvertSwitcher switcher) {
            reference = new WeakReference<>(switcher);
        }

        @Override
        public void handleMessage(Message msg) {
            AdvertSwitcher theSwitcher = reference.get();
            if (theSwitcher == null || theSwitcher.mContext == null
                    || theSwitcher.mContext instanceof Activity
                    && ((Activity) theSwitcher.mContext).isFinishing()) {
                return;
            }
            switch (msg.what) {
                case FLAG_SCROLL:
                    View view = theSwitcher.getNextView();
                    theSwitcher.mAdapter.bindView(view,
                            theSwitcher.mAdapter.getItem(theSwitcher.mCurIndex),
                            theSwitcher.mCurIndex);
                    ULog.e("Index: " + theSwitcher.mCurIndex
                            + " / " + theSwitcher.mAdapter.getItem(theSwitcher.mCurIndex).toString());
                    theSwitcher.showNext();
                    theSwitcher.mCurIndex = ++theSwitcher.mCurIndex % theSwitcher.mAdapter.getCount();
                    theSwitcher.mHandler.sendEmptyMessageDelayed(FLAG_SCROLL, theSwitcher.mTimeSpan);
                    break;
            }
        }
    }

    public AdvertSwitcher(Context context) {
        this(context, null);
    }

    public AdvertSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttribute(attrs);
        init(context);
    }

    private void initAttribute(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.lib_ui_view_AdvertSwitcher);
        mTimeSpan = typedArray.getInteger(R.styleable.lib_ui_view_AdvertSwitcher_lib_ui_view_advertswitcher_timeSpan, DEFAULT_TIME_SPAN);
        mInAnimId = typedArray.getResourceId(R.styleable.lib_ui_view_AdvertSwitcher_lib_ui_view_advertswitcher_inAnim, DEFAULT_IN_ANIM_ID);
        mOutAnimId = typedArray.getResourceId(R.styleable.lib_ui_view_AdvertSwitcher_lib_ui_view_advertswitcher_outAnim, DEFAULT_OUT_ANIM_ID);
        mInterpolator = typedArray.getResourceId(R.styleable.lib_ui_view_AdvertSwitcher_lib_ui_view_advertswitcher_interpolator, DEFAULT_INTERPOLATOR);
        typedArray.recycle();
    }

    private void init(Context context) {
        mContext = context;
        mCurIndex = 0;
        mHandler = new ScrollHandler(this);
        Animation inAnim = AnimationUtils.loadAnimation(mContext, mInAnimId);
        Animation outAnim = AnimationUtils.loadAnimation(mContext, mOutAnimId);
        inAnim.setInterpolator(mContext, mInterpolator);
        outAnim.setInterpolator(mContext, mInterpolator);
        setInAnimation(inAnim);
        setOutAnimation(outAnim);
    }

    /**
     * Start - onResume
     */
    @UiThread
    public void start() {
        if (mAdapter.getCount() <= 0) {
            return;
        }
        mHandler.sendEmptyMessage(FLAG_SCROLL);
    }

    /**
     * Stop - onPause
     */
    @UiThread
    public void stop() {
        mHandler.removeMessages(FLAG_SCROLL);
    }

    /**
     * Refresh
     */
    @UiThread
    public void refresh() {
        stop();
        mCurIndex = 0;
        removeAllViews();
        start();
    }

    public Adapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(@NonNull Adapter adapter) {
        stop();
        mCurIndex = 0;
        removeAllViews();
        mAdapter = adapter;
        setFactory(new ViewFactory() {
            @Override
            public View makeView() {
                return mAdapter.makeView();
            }
        });
    }

    public static abstract class Adapter<T> {
        protected Context mContext;
        protected List<T> mDatas;
        protected int mResId;

        public Adapter(Context context, List<T> datas, int resId) {
            mContext = context;
            mDatas = datas;
            mResId = resId;
        }

        public int getCount() {
            return mDatas != null ? mDatas.size() : 0;
        }

        public T getItem(int position) {
            return mDatas.get(position);
        }

        public abstract View makeView();

        public abstract void bindView(View view, T item, int position);
    }
}
