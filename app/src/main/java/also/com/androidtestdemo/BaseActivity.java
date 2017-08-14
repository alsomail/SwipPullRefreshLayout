package also.com.androidtestdemo;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Jzp on 2017/4/26.
 */
public abstract class BaseActivity extends AppCompatActivity {
    protected Context mContext;
    protected boolean isPause;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(initLayRes());
        this.mContext = this;
        initView();
        initPresenter();
        initData();
    }

    protected abstract int initLayRes();

    protected abstract void initView();

    protected abstract void initPresenter();

    /**
     * 初始化视图后加载数据
     */
    public abstract void initData();


    /**
     * 是否Pause
     * @return
     */
    public boolean isPause() {
        return isPause;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPause=false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPause=true;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


}
