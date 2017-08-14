package also.com.androidtestdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;

/**
 * 描述：
 * 作者： Also
 * 日期： 2017/8/8  16:42
 * 邮箱： galsomail@gmail.com
 */

public class DemoAdapter extends BaseRecyclerViewAdapter<Object,BaseRecycleViewHolder> {

    private final SimpleDateFormat mformatter;

    public DemoAdapter(Context context) {
        super(context);
        mformatter = new SimpleDateFormat("yyyy-MM-dd");
    }

    @Override
    public int getItemCount() {
        return 25;
    }


    @Override
    public BaseRecycleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new BaseRecycleViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_fans,parent,false));
    }

    @Override
    public void onBindViewHolder(BaseRecycleViewHolder holder, int position) {
        holder.getTextView(R.id.tv_nickName).setText("谷阿莫呀");
        holder.getTextView(R.id.tv_addTime).setText(mformatter.format(System.currentTimeMillis()));
//        if (position % 2 == 0) {
//            ImageLoader.load(mContext, holder.getImageView(R.id.iv_img), R.drawable.icon_wechat);
//        }else{
//            ImageLoader.load(mContext, holder.getImageView(R.id.iv_img), R.drawable.icon_alipay);
//        }
    }
}
