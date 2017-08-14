package also.com.androidtestdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * 描述：
 * 作者： Also
 * 日期： 2017/8/1  11:35
 * 邮箱： galsomail@gmail.com
 */

public class RecyclerItemDecoration extends RecyclerView.ItemDecoration {

    private int mSpace;
    private Paint mPaint;

    /**
     * @param space 分割线宽度
     * @param color 分割线颜色
     */
    public RecyclerItemDecoration(Context context, int space, int color) {
        this.mSpace = space;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(context.getResources().getColor(color));
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(space );
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (parent.getLayoutManager() != null) {
            if (parent.getLayoutManager() instanceof LinearLayoutManager && !(parent.getLayoutManager()
                    instanceof
                    GridLayoutManager)) {
                if (((LinearLayoutManager) parent.getLayoutManager()).getOrientation() == LinearLayoutManager
                        .HORIZONTAL) {
                    outRect.set(mSpace, 0, mSpace, 0);
                } else {
                    outRect.set(0, 0, 0, mSpace);
                }
            }
        } else {
            outRect.set(mSpace, mSpace, mSpace, mSpace);
        }
    }


    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);
        if (parent.getLayoutManager() != null) {
            if (parent.getLayoutManager() instanceof LinearLayoutManager && !(parent.getLayoutManager()
                    instanceof
                    GridLayoutManager)) {
                if (((LinearLayoutManager) parent.getLayoutManager()).getOrientation() == LinearLayoutManager
                        .HORIZONTAL) {
                } else {
                    drawLinearVerticalBottomNoLast(c, parent);
                }
            }
        } else {
            drawGridViewNoStroke(c, parent);
        }
    }


    /**
     * 为gridView布局设置无边框分隔（井字形）
     *
     * @param canvas
     * @param parent
     */
    private void drawGridViewNoStroke(Canvas canvas, RecyclerView parent) {
        //如果是第一列第一个，绘制右边和下面的线条
        int childCount = parent.getChildCount();
        int spanCount = ((GridLayoutManager) parent.getLayoutManager()).getSpanCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            //top、bottom、left、right分别是分割线的上下左右坐标
            //getBottom/getMeasuredHeight/rightMargin/child.getPaddingBottom() 需要继续探究代表是什么
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();

            //绘制右侧分割线，最右边一列不需要分割线
            if ((i + 1) % spanCount != 0) {
                //top取item的高度，每一排的高度一样，第一排为0
                int top = child.getMeasuredHeight() * (i / spanCount);
                //bottom取item的高度，每一排的高度一样，第一排为item高度
                int bottom = child.getMeasuredHeight() * ((i + spanCount) / spanCount);
                //left取item的最右侧坐标
                int left = child.getRight() + layoutParams.rightMargin;
                //right取右侧坐标+分割线宽度
                int right = left + mSpace;
                canvas.drawRect(left, top, right, bottom, mPaint);
            }

            //绘制底部分割线，最下面一排不需要分割线
            if (i < childCount - spanCount) {
                int top = child.getBottom() + layoutParams.bottomMargin;
                int bottom = top + mSpace;
                int left = child.getLeft();
                int right = child.getRight();
                canvas.drawRect(left, top, right, bottom, mPaint);
            }

        }
    }

    /**
     * 垂直的LinearLayout 中 item 下部 分割线,最后一个不会只
     *
     * @param canvas
     * @param parent
     */
    private void drawLinearVerticalBottomNoLast(Canvas canvas, RecyclerView parent) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            //top、bottom、left、right分别是分割线的上下左右坐标
            //getBottom/getMeasuredHeight/rightMargin/child.getPaddingBottom() 需要继续探究代表是什么
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();

            //绘制底部分割线
            if (i!=childCount-1) {
                int top = child.getBottom() + layoutParams.bottomMargin;
                int bottom = top + mSpace;
                int left = child.getLeft();
                int right = child.getRight();
                canvas.drawRect(left, top, right, bottom, mPaint);
            }
        }
    }

}
