package also.com.androidtestdemo;

import android.os.Bundle;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvContent;
    private DemoAdapter mAdapter;
    private SwipePullRefreshLayout viewById;

    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    viewById.setRefreshing(false);
                    break;
                case 1:
                    viewById.setPulling(false);
                    break;
            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rvContent = (RecyclerView) findViewById(R.id.rv_content);
        viewById = (SwipePullRefreshLayout) findViewById(R.id.sprl);
        initView();

    }


    private void initView() {
        rvContent.setLayoutManager(new LinearLayoutManager(this));
        rvContent.addItemDecoration(new RecyclerItemDecoration(this,1,R.color.gray_e5e5e5));
        mAdapter = new DemoAdapter(this);
        mAdapter = new DemoAdapter(this);
        rvContent.setAdapter(mAdapter);
        viewById.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i("SwipePullRefreshLayout", "刷新");
                viewById.setPulling(false);
                handler.sendEmptyMessageDelayed(0, 3000);
            }
        });
        viewById.setOnPullListener(new SwipePullRefreshLayout.OnLoadListener() {
            @Override
            public void onLoad() {
                Log.i("SwipePullRefreshLayout", "加载");
                viewById.setRefreshing(false);
                handler.sendEmptyMessageDelayed(1, 3000);
            }
        });
    }


}
