package com.baidu.track.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import androidx.core.content.res.ResourcesCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.baidu.track.R;
import com.baidu.track.TrackApplication;
import com.baidu.track.model.ItemInfo;
import com.baidu.track.utils.BitmapUtil;
import com.baidu.track.utils.CommonUtil;
import com.baidu.track.utils.Constants;
import com.baidu.track.utils.SharedPreferenceUtil;

import java.util.List;

public class MainActivity extends BaseActivity {

    private TrackApplication trackApp;

    private ListView mListView = null;

    private MyAdapter myAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setOptionsButtonInVisible();
        init();
        //初始化地图上使用的点的坐标显示
        BitmapUtil.init();
    }

    private void init() {
        trackApp = (TrackApplication) getApplicationContext();
        mListView = (ListView) findViewById(R.id.list);
        myAdapter = new MyAdapter(trackApp.itemInfos);
        mListView.setAdapter(myAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View v, int index, long arg3) {
                Intent intent = new Intent(MainActivity.this, trackApp.itemInfos.get(index).clazz);
                startActivity(intent);
            }
        });

        boolean isContainsPermissionKey = SharedPreferenceUtil.contains(this, Constants.PERMISSIONS_DESC_KEY);
        if (!isContainsPermissionKey) {
            showDialog();
        } else {
            boolean isAccessPermission = SharedPreferenceUtil
                    .getBoolean(this, Constants.PERMISSIONS_DESC_KEY, false);
            if (!isAccessPermission) {
                showDialog();
            }
        }
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_main;
    }

    public class MyAdapter extends BaseAdapter
            implements AbsListView.OnScrollListener {

        private List<ItemInfo> itemInfos = null;

        public class ViewHolder {
            /**
             * 标题图标
             */
            ImageView titleIcon;
            /**
             * 标题
             */
            TextView title;
            /**
             * 说明
             */
            TextView desc;
        }

        public MyAdapter(List<ItemInfo> itemInfos) {
            this.itemInfos = itemInfos;
        }

        @Override
        public void onScrollStateChanged(AbsListView absListView, int i) {

        }

        @Override
        public void onScroll(AbsListView absListView, int i, int i1, int i2) {

        }

        @Override
        public int getCount() {
            return itemInfos.size();
        }

        @Override
        public Object getItem(int position) {
            return itemInfos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (null != convertView) {
                viewHolder = (ViewHolder) convertView.getTag();
            } else {
                convertView = View.inflate(MainActivity.this, R.layout.item_list, null);
                viewHolder = new ViewHolder();

                viewHolder.titleIcon = (ImageView) convertView
                        .findViewById(R.id.iv_all_title);
                viewHolder.title = (TextView) convertView
                        .findViewById(R.id.tv_all_title);
                viewHolder.desc = (TextView) convertView
                        .findViewById(R.id.tv_all_desc);
                convertView.setTag(viewHolder);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                viewHolder.titleIcon.setBackground(ResourcesCompat.getDrawable(getResources(),
                        itemInfos.get(position).titleIconId, null));
            } else {
                viewHolder.titleIcon.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(),
                        itemInfos.get(position).titleIconId, null));
            }
            viewHolder.title.setText(itemInfos.get(position).titleId);
            viewHolder.desc.setText(itemInfos.get(position).descId);
            return convertView;
        }
    }

    /**
     * 显示提示信息
     */
    private void showDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示: ");
        TextView textView = new TextView(this);
        textView.setText(R.string.privacy_permission_desc);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setPadding( 50 , 30 , 50 , 10 );
        builder.setView(textView);

        builder.setPositiveButton("同意", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferenceUtil
                        .putBoolean(MainActivity.this, Constants.PERMISSIONS_DESC_KEY, true);
                dialog.cancel();
            }
        });

        builder.setNegativeButton("不同意", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferenceUtil
                        .putBoolean(MainActivity.this, Constants.PERMISSIONS_DESC_KEY, false);
                dialog.cancel();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CommonUtil.saveCurrentLocation(trackApp);
        if (trackApp.mClient != null) {
            if (trackApp.trackConf.contains("is_trace_started")
                    && trackApp.trackConf.getBoolean("is_trace_started", true)) {
                // 退出app停止轨迹服务时，不再接收回调，将OnTraceListener置空
                trackApp.mClient.setOnTraceListener(null);
                trackApp.mClient.stopTrace(trackApp.mTrace, null);
            } else {
                trackApp.mClient.clear();
            }
        }
        trackApp.isTraceStarted = false;
        trackApp.isGatherStarted = false;
        SharedPreferences.Editor editor = trackApp.trackConf.edit();
        editor.remove("is_trace_started");
        editor.remove("is_gather_started");
        editor.apply();

        BitmapUtil.clear();
    }
}
