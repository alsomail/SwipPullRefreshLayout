package also.com.androidtestdemo;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 公共RecycleView.Holder，避免每次都需要写ViewHolder
 */
public class BaseRecycleViewHolder extends RecyclerView.ViewHolder {
    private View mConvertView;
    private SparseArray<View> mView;

    public BaseRecycleViewHolder(View itemView) {
        super(itemView);
        mView = new SparseArray<>();
        mConvertView = itemView;
    }

    public View getConvertView() {
        return mConvertView;
    }

    /**
     * 通过id获取控件
     */
    public <T extends View> T getView(int viewId) {
        View view = mView.get(viewId);
        if (view == null) {
            view = mConvertView.findViewById(viewId);
            mView.put(viewId, view);
        }
        return (T) view;
    }

    public TextView getTextView(int viewId) {
        return getView(viewId);
    }

    public ImageView getImageView(int viewId) {
        return getView(viewId);
    }

}
