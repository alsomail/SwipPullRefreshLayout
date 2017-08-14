package also.com.androidtestdemo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述：基类,一些公共方法
 * 作者： Also
 * 日期： 7/25/16  17:47
 */
public abstract class BaseRecyclerViewAdapter<T,VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    protected Context mContext;

    protected List<T> mList;

    protected OnItemClickListener mOnitemClickListener;

    public BaseRecyclerViewAdapter(Context context) {
        mContext = context;
    }

    public void setData(List<T> list) {
        mList = list;
        notifyDataSetChanged();
    }

    public synchronized void addData(List<T> list) {
        if (mList==null)
            mList = new ArrayList();
        if (list != null) {
            mList.addAll(list);
            notifyDataSetChanged();
        }

    }

    public synchronized void addData(T object) {
        if (mList==null)
            mList = new ArrayList();
        if (object != null) {
            mList.add(object);
            notifyDataSetChanged();
        }

    }

    public void clearData() {
        if (mList != null) {
            mList.clear();
        }
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnitemClickListener=listener;
    }


    public List getData() {
        return mList;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }



    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }

    public interface OnItemClickListener {
         void onItemClick(View view, List list, int position);
    }
}
