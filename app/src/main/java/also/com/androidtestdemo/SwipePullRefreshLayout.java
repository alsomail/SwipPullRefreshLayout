package also.com.androidtestdemo;


import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.ImageView;

/**
 * 描述：swipRefreshLayout增加一个上拉加载更多监听，动画为原有动画镜像效果，不触发原有onTouch 和onInterceptTouchEvent方法，下拉刷新的触发由NestedScrollingParent监听触发，所以没有实现
 * 遗留问题：上下拉刷新一起触发会导致动画异常
 * 原因：动画开始时重置、动画结束时调用reset方法（其中调用的MaterialProgressDrawable中的stop方法会清除所有动画）。SwipeRefreshLayout源码中的reset
 * 方法不能直接修改，为了兼容问题不考虑反射
 * 处理：
 * 1、下拉刷新时（isRefreshing）不能上拉加载更多，防止上拉调用动画前重置动画导致下拉刷新动画异常
 * 2、下拉刷新时（isRefreshing），取消在reset中的stop调用，防止在上拉加载更多时（isPulling），触发下拉刷新操作(onRefresh)导致下拉刷新动画异常
 * 另：
 * 因为下拉刷新时（onRefresh）会触发动画重置，建议在刷新的时候取消上拉加载（setPulling(false)），关闭被卡住的上拉加载动画
 * 作者： Also
 * 日期： 2017/8/11  11:41
 * 邮箱： galsomail@gmail.com
 */

public class SwipePullRefreshLayout extends SwipeRefreshLayout {

    private static final String LOG_TAG = SwipePullRefreshLayout.class.getSimpleName();

    CircleImageView mLoadCircleView;
    MaterialProgressDrawable mPullProgress;
    // Default background for the progress spinner
    private static final int CIRCLE_BG_LIGHT = 0xFFFAFAFA;
    private int mLoadCircleViewIndex = -1;
    int mCurrentPullTargetOffsetTop;

    private int mLoadCircleDiameter;
    @VisibleForTesting
    static final int CIRCLE_DIAMETER = 40;
    @VisibleForTesting
    static final int CIRCLE_DIAMETER_LARGE = 56;

    private int mActivePointerId;
    private float mInitialPullDownY;
    private boolean mIsBeingPulled = false;
    private int mPullSlop;
    private View mTarget; // the target of the gesture
    private boolean mPulling = false;
    private boolean mReturningToPullStart = false;
    private int mLoadOriginalOffsetTop = -1;
    private static final int INVALID_POINTER = -1;

    private static final int MAX_ALPHA = 255;
    private static final int STARTING_PROGRESS_ALPHA = (int) (.3f * MAX_ALPHA);

    private static final float DRAG_RATE = .5f;
    private float mTotalPullDistance = -1;
    int mPullSpinnerOffsetEnd;
    private static final int DEFAULT_CIRCLE_TARGET = 64;
    boolean mUsingCustomStart;
    // Whether this item is scaled up rather than clipped
    boolean mScale;
    private float mInitialPullMotionY;

    private Animation mLoadAlphaStartAnimation;
    private Animation mLoadAlphaMaxAnimation;
    private Animation mLoadScaleDownToStartAnimation;
    private Animation mLoadScaleAnimation;
    private Animation mLoadScaleDownAnimation;
    // Max amount of circle that can be filled by progress during swipe gesture,
    // where 1.0 is a full circle
    private static final float MAX_PROGRESS_ANGLE = .8f;
    boolean mPullNotify;

    private static final int SCALE_DOWN_DURATION = 150;

    private static final int ALPHA_ANIMATION_DURATION = 300;

    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;

    private static final int ANIMATE_TO_START_DURATION = 200;


    private final DecelerateInterpolator mDecelerateInterpolator;
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;

    float mLoadStartingScale;
    protected int mPullFrom;
    private int mMediumAnimationDuration;
    OnLoadListener mLoadListener;
    private int mChildHeight;
    private DisplayMetrics mMetrics;

    public SwipePullRefreshLayout(Context context) {
        this(context, null);
    }

    public SwipePullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPullSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);
        createProgressView();
        setSize(MaterialProgressDrawable.LARGE);
        mMetrics = getResources().getDisplayMetrics();
//        mLoadOriginalOffsetTop = mCurrentPullTargetOffsetTop = 0;

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        final View child = mTarget;
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        int circleWidth = mLoadCircleView.getMeasuredWidth();
        int circleHeight = mLoadCircleView.getMeasuredHeight();
        if (mLoadOriginalOffsetTop == -1) {
            //初始化原始位置
            mLoadOriginalOffsetTop = mCurrentPullTargetOffsetTop = mChildHeight = childHeight;

            mPullSpinnerOffsetEnd = mLoadOriginalOffsetTop - (int) (DEFAULT_CIRCLE_TARGET * mMetrics.density);
            mTotalPullDistance = (int) (DEFAULT_CIRCLE_TARGET * mMetrics.density);

        }
        mLoadCircleView.layout((width / 2 - circleWidth / 2), mCurrentPullTargetOffsetTop,
                (width / 2 + circleWidth / 2), mCurrentPullTargetOffsetTop + circleHeight);

    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        mLoadCircleView.measure(MeasureSpec.makeMeasureSpec(mLoadCircleDiameter, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mLoadCircleDiameter, MeasureSpec.EXACTLY));
        mLoadCircleViewIndex = -1;
        // Get the index of the circleview.
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mLoadCircleView) {
                mLoadCircleViewIndex = index;
                break;
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = ev.getActionMasked();
        int pointerIndex;

        if (mReturningToPullStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToPullStart = false;
        }

        //父控件正在滚动暂时未写mNestedScrollInProgress
        if (!isEnabled() || mReturningToPullStart || canChildScrollDown() || mPulling) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                setTargetOffsetTopAndBottom(mLoadOriginalOffsetTop - mLoadCircleView.getTop());
                mActivePointerId = ev.getPointerId(0);
                mIsBeingPulled = false;

                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                mInitialPullDownY = ev.getY(pointerIndex);
                break;
            case MotionEvent.ACTION_MOVE: {
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }
                final float y = ev.getY(pointerIndex);
                startPull(y);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingPulled = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingPulled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        int pointerIndex = -1;

        if (mReturningToPullStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToPullStart = false;
        }

        //父控件正在滚动暂时未写mNestedScrollInProgress
        if (!isEnabled() || mReturningToPullStart || canChildScrollDown() || mPulling) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsBeingPulled = false;
                break;
            case MotionEvent.ACTION_MOVE: {
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = ev.getY(pointerIndex);
                startPull(y);

                if (mIsBeingPulled) {
                    final float overscrollTop = (y - mInitialPullMotionY) * DRAG_RATE;
                    if (overscrollTop < 0) {
                        movePullSpinner(overscrollTop);
                    } else {
                        return false;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                pointerIndex = ev.getActionIndex();
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG,
                            "Got ACTION_POINTER_DOWN event but have an invalid action index.");
                    return false;
                }
                mActivePointerId = ev.getPointerId(pointerIndex);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
            case MotionEvent.ACTION_UP: {
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    return false;
                }
                if (mIsBeingPulled) {
                    final float y = ev.getY(pointerIndex);
                    final float overscrollTop = (y - mInitialPullMotionY) * DRAG_RATE;
                    mIsBeingPulled = false;
                    finishSpinner(overscrollTop);
                }
                mActivePointerId = INVALID_POINTER;
                return false;
            }
            case MotionEvent.ACTION_CANCEL:
                return false;
        }


        return false;
    }


    private void movePullSpinner(float overscrollTop) {
        mPullProgress.showArrow(true);
        float originalDragPercent = overscrollTop / mTotalPullDistance;

        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
        float adjustedPercent = (float) Math.max(dragPercent - .4, 0) * 5 / 3;
        float extraOS = Math.abs(overscrollTop) - mTotalPullDistance;
//        float slingshotDist = mUsingCustomStart ? mSpinnerOffsetEnd - mOriginalOffsetTop
//                : mPullSpinnerOffsetEnd;
        float slingshotDist = mTotalPullDistance;
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2)
                / slingshotDist);
        float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                (tensionSlingshotPercent / 4), 2)) * 2f;
        float extraMove = (slingshotDist) * tensionPercent * 2;

        int targetY = mLoadOriginalOffsetTop - (int) ((slingshotDist * dragPercent) + extraMove);
        // where 1.0f is a full circle
        if (mLoadCircleView.getVisibility() != View.VISIBLE) {
            mLoadCircleView.setVisibility(View.VISIBLE);
        }
        if (!mScale) {
            mLoadCircleView.setScaleX(1f);
            mLoadCircleView.setScaleY(1f);
        }

        if (mScale) {
            setAnimationProgress(Math.min(1f, overscrollTop / mTotalPullDistance));
        }
        if (Math.abs(overscrollTop) < mTotalPullDistance) {
            if (mPullProgress.getAlpha() > STARTING_PROGRESS_ALPHA
                    && !isLoadAnimationRunning(mLoadAlphaStartAnimation)) {
                // Animate the alpha
                startLoadProgressAlphaStartAnimation();
            }
        } else {
            if (mPullProgress.getAlpha() < MAX_ALPHA && !isLoadAnimationRunning(mLoadAlphaMaxAnimation)) {
                // Animate the alpha
                startLoadProgressAlphaMaxAnimation();
            }
        }
        float strokeStart = adjustedPercent * .8f;
        mPullProgress.setStartEndTrim(0f, Math.min(MAX_PROGRESS_ANGLE, strokeStart));
        mPullProgress.setArrowScale(Math.min(1f, adjustedPercent));

        float rotation = (-0.25f + .4f * adjustedPercent + tensionPercent * 2) * .5f;
        mPullProgress.setProgressRotation(rotation);
        setTargetOffsetTopAndBottom(targetY - mCurrentPullTargetOffsetTop);
    }


    private void finishSpinner(float overscrollTop) {
        if (Math.abs(overscrollTop) > mTotalPullDistance) {
            setPulling(true, true /* notify */);
        } else {
            // cancel refresh
            mPulling = false;
            mPullProgress.setStartEndTrim(0f, 0f);
            Animation.AnimationListener listener = null;
            if (!mScale) {
                listener = new Animation.AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (!mScale) {
                            startLoadScaleDownAnimation(null);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                };
            }
            loadAnimateOffsetToStartPosition(mCurrentPullTargetOffsetTop, listener);
            mPullProgress.showArrow(false);
        }
    }


    /*************************动画 start *******************************/

    private boolean isLoadAnimationRunning(Animation animation) {
        return animation != null && animation.hasStarted() && !animation.hasEnded();
    }

    private void startLoadProgressAlphaStartAnimation() {
        mLoadAlphaStartAnimation = startLoadAlphaAnimation(mPullProgress.getAlpha(), STARTING_PROGRESS_ALPHA);
    }

    private void startLoadProgressAlphaMaxAnimation() {
        mLoadAlphaMaxAnimation = startLoadAlphaAnimation(mPullProgress.getAlpha(), MAX_ALPHA);
    }

    private Animation startLoadAlphaAnimation(final int startingAlpha, final int endingAlpha) {
        Animation alpha = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                mPullProgress.setAlpha(
                        (int) (startingAlpha + ((endingAlpha - startingAlpha) * interpolatedTime)));
            }
        };
        alpha.setDuration(ALPHA_ANIMATION_DURATION);
        // Clear out the previous animation listeners.
        mLoadCircleView.setAnimationListener(null);
        mLoadCircleView.clearAnimation();
        mLoadCircleView.startAnimation(alpha);
        return alpha;
    }

    private void loadAnimateOffsetToCorrectPosition(int from, Animation.AnimationListener listener) {
        mPullFrom = from;
        mLoadAnimateToCorrectPosition.reset();
        mLoadAnimateToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
        mLoadAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        if (listener != null) {
            mLoadCircleView.setAnimationListener(listener);
        }
        mLoadCircleView.clearAnimation();
        mLoadCircleView.startAnimation(mLoadAnimateToCorrectPosition);
    }

    private void loadAnimateOffsetToStartPosition(int from, Animation.AnimationListener listener) {
        if (mScale) {
            // Scale the item back down
            startLoadScaleDownReturnToStartAnimation(from, listener);
        } else {
            mPullFrom = from;
            mLoadAnimateToStartPosition.reset();
            mLoadAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
            mLoadAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
            if (listener != null) {
                mLoadCircleView.setAnimationListener(listener);
            }
            mLoadCircleView.clearAnimation();
            mLoadCircleView.startAnimation(mLoadAnimateToStartPosition);
        }
    }

    void startLoadScaleDownAnimation(Animation.AnimationListener listener) {
        mLoadScaleDownAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(1 - interpolatedTime);
            }
        };
        mLoadScaleDownAnimation.setDuration(SCALE_DOWN_DURATION);
        mLoadCircleView.setAnimationListener(listener);
        mLoadCircleView.clearAnimation();
        mLoadCircleView.startAnimation(mLoadScaleDownAnimation);
    }

    private final Animation mLoadAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            int endTarget = 0;
//            if (!mUsingCustomStart) {
//                endTarget = mPullSpinnerOffsetEnd - Math.abs(mLoadOriginalOffsetTop);
//            } else {
            //因为是正向坐标，位置直接指定
            endTarget = mPullSpinnerOffsetEnd;
//            }
            targetTop = (mPullFrom + (int) ((endTarget - mPullFrom) * interpolatedTime));
            int offset = targetTop - mLoadCircleView.getTop();
            setTargetOffsetTopAndBottom(offset);
            mPullProgress.setArrowScale(1 - interpolatedTime);
        }
    };

    private final Animation mLoadAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);
        }
    };

    private void startLoadScaleDownReturnToStartAnimation(int from,
                                                          Animation.AnimationListener listener) {
        mPullFrom = from;
        mLoadStartingScale = mLoadCircleView.getScaleX();
        mLoadScaleDownToStartAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                float targetScale = (mLoadStartingScale + (-mLoadStartingScale * interpolatedTime));
                setAnimationProgress(targetScale);
                moveToStart(interpolatedTime);
            }
        };
        mLoadScaleDownToStartAnimation.setDuration(SCALE_DOWN_DURATION);
        if (listener != null) {
            mLoadCircleView.setAnimationListener(listener);
        }
        mLoadCircleView.clearAnimation();
        mLoadCircleView.startAnimation(mLoadScaleDownToStartAnimation);
    }

    private void startLoadScaleUpAnimation(Animation.AnimationListener listener) {
        mLoadCircleView.setVisibility(View.VISIBLE);
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            // Pre API 11, alpha is used in place of scale up to show the
            // progress circle appearing.
            // Don't adjust the alpha during appearance otherwise.
            mPullProgress.setAlpha(MAX_ALPHA);
        }
        mLoadScaleAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(interpolatedTime);
            }
        };
        mLoadScaleAnimation.setDuration(mMediumAnimationDuration);
        if (listener != null) {
            mLoadCircleView.setAnimationListener(listener);
        }
        mLoadCircleView.clearAnimation();
        mLoadCircleView.startAnimation(mLoadScaleAnimation);
    }

    private Animation.AnimationListener mPullListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @SuppressLint("NewApi")
        @Override
        public void onAnimationEnd(Animation animation) {
            if (mPulling) {
                // Make sure the progress view is fully visible
                mPullProgress.setAlpha(MAX_ALPHA);
                mPullProgress.start();
                if (mPullNotify) {
                    if (mLoadListener != null) {
                        mLoadListener.onLoad();
                    }
                }
                mCurrentPullTargetOffsetTop = mLoadCircleView.getTop();
            } else {
                reset();
            }
        }
    };

    @Override
    public void setRefreshing(boolean refreshing) {

        super.setRefreshing(refreshing);
    }

    /*************************动画 end*******************************/


    void reset() {
        mLoadCircleView.clearAnimation();
        if (!isRefreshing())
            mPullProgress.stop();
        mLoadCircleView.setVisibility(View.GONE);
        setColorViewAlpha(MAX_ALPHA);
        // Return the circle to its start position
        if (mScale) {
            setAnimationProgress(0 /* animation complete and view is hidden */);
        } else {
            setTargetOffsetTopAndBottom(mLoadOriginalOffsetTop - mCurrentPullTargetOffsetTop);
        }
        mCurrentPullTargetOffsetTop = mLoadCircleView.getTop();
    }

    private void setColorViewAlpha(int targetAlpha) {
        mLoadCircleView.getBackground().setAlpha(targetAlpha);
        mPullProgress.setAlpha(targetAlpha);
    }

    void moveToStart(float interpolatedTime) {
        int targetTop = 0;
        targetTop = (mPullFrom + (int) ((mLoadOriginalOffsetTop - mPullFrom) * interpolatedTime));
        int offset = targetTop - mLoadCircleView.getTop();
        setTargetOffsetTopAndBottom(offset);
    }

    /**
     * Pre API 11, this does an alpha animation.
     *
     * @param progress
     */
    void setAnimationProgress(float progress) {
        mLoadCircleView.setScaleX(progress);
        mLoadCircleView.setScaleY(progress);
    }


    private void startPull(float y) {
        final float yDiff = mInitialPullDownY - y;
        if (yDiff > mPullSlop && !mIsBeingPulled && !isRefreshing()) {
            mInitialPullMotionY = mInitialPullDownY - mPullSlop;
            mIsBeingPulled = true;
            mPullProgress.setAlpha(STARTING_PROGRESS_ALPHA);
        }
    }


    void setTargetOffsetTopAndBottom(int offset) {
        mLoadCircleView.bringToFront();

        ViewCompat.offsetTopAndBottom(mLoadCircleView, offset);
        mCurrentPullTargetOffsetTop = mLoadCircleView.getTop();
    }


    /**
     * 设置加载控件大小
     *
     * @param size
     */
    public void setSize(int size) {
        super.setSize(size);
        if (size != MaterialProgressDrawable.LARGE && size != MaterialProgressDrawable.DEFAULT) {
            return;
        }
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        if (size == MaterialProgressDrawable.LARGE) {
            mLoadCircleDiameter = (int) (CIRCLE_DIAMETER_LARGE * metrics.density);
        } else {
            mLoadCircleDiameter = (int) (CIRCLE_DIAMETER * metrics.density);
        }
        // force the bounds of the progress circle inside the circle view to
        // update by setting it to null before updating its size and then
        // re-setting it
        mLoadCircleView.setImageDrawable(null);
        mPullProgress.updateSizes(size);
        mLoadCircleView.setImageDrawable(mPullProgress);
    }


    /**
     * 创建加载控件
     */
    private void createProgressView() {
        mLoadCircleView = new CircleImageView(getContext(), CIRCLE_BG_LIGHT);
        mPullProgress = new MaterialProgressDrawable(getContext(), this);
        mPullProgress.setBackgroundColor(CIRCLE_BG_LIGHT);
        mLoadCircleView.setImageDrawable(mPullProgress);
        mLoadCircleView.setVisibility(View.GONE);
        addView(mLoadCircleView);
    }


    /**
     * 是否正在加载更多
     *
     * @return
     */
    public boolean isPulling() {
        return mPulling;
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setPulling(boolean refreshing) {
        if (refreshing && mPulling != refreshing) {
            // scale and show
            mPulling = refreshing;
            int endTarget = 0;
//            if (!mUsingCustomStart) {
//                endTarget = mPullSpinnerOffsetEnd + mLoadOriginalOffsetTop;
//            } else {
            endTarget = mPullSpinnerOffsetEnd;
//            }
            setTargetOffsetTopAndBottom(endTarget - mCurrentPullTargetOffsetTop);
            mPullNotify = false;
            startLoadScaleUpAnimation(mPullListener);
        } else {
            setPulling(refreshing, false /* notify */);
        }
    }


    private void setPulling(boolean pulling, final boolean notify) {
        if (mPulling != pulling) {
            mPullNotify = notify;
            ensureTarget();
            mPulling = pulling;
            if (mPulling) {
                loadAnimateOffsetToCorrectPosition(mCurrentPullTargetOffsetTop, mPullListener);
            } else {
                startLoadScaleDownAnimation(mPullListener);
            }
        }
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnPullListener(OnLoadListener listener) {
        mLoadListener = listener;
    }


    /**
     * 判断是否可以上拉
     *
     * @return
     */
    public boolean canChildScrollDown() {

        return mTarget.canScrollVertically(1);
    }


    /**
     * 获得目标滚动控件
     */
    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!(child instanceof ImageView)) {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    /*******************动画拓展以后再写************/
//    /**
//     * The refresh indicator starting and resting position is always positioned
//     * near the top of the refreshing content. This position is a consistent
//     * location, but can be adjusted in either direction based on whether or not
//     * there is a toolbar or actionbar present.
//     * <p>
//     * <strong>Note:</strong> Calling this will reset the position of the refresh indicator to
//     * <code>start</code>.
//     * </p>
//     *
//     * @param scale Set to true if there is no view at a higher z-order than where the progress
//     *              spinner is set to appear. Setting it to true will cause indicator to be scaled
//     *              up rather than clipped.
//     * @param start The offset in pixels from the top of this view at which the
//     *              progress spinner should appear.
//     * @param end The offset in pixels from the top of this view at which the
//     *            progress spinner should come to rest after a successful swipe
//     *            gesture.
//     */
//    public void setProgressViewOffset(boolean scale, int start, int end) {
//        mScale = scale;
//        mLoadOriginalOffsetTop = start;
//        mSpinnerOffsetEnd = end;
//        mUsingCustomStart = true;
//        reset();
//        mRefreshing = false;
//    }
//

    /**********************************/

    public interface OnLoadListener {
        /**
         * Called when a swipe gesture triggers a refresh.
         */
        void onLoad();
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = ev.getActionIndex();
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

}
