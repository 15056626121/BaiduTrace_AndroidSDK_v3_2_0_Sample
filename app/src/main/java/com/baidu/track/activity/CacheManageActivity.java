package com.baidu.track.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.baidu.trace.api.track.ClearCacheTrackRequest;
import com.baidu.trace.api.track.ClearCacheTrackResponse;
import com.baidu.trace.api.track.OnTrackListener;
import com.baidu.trace.api.track.QueryCacheTrackRequest;
import com.baidu.trace.api.track.QueryCacheTrackResponse;
import com.baidu.track.R;
import com.baidu.track.TrackApplication;
import com.baidu.track.dialog.DateDialog;
import com.baidu.track.utils.CommonUtil;
import com.baidu.track.utils.ViewUtil;

/**
 * 缓存管理
 */
public class CacheManageActivity extends BaseActivity {

    private TrackApplication trackApp = null;
    private OnTrackListener trackListener = null;
    private ViewUtil viewUtil = null;
    private DateDialog dateDialog = null;
    private DateDialog.Callback startTimeCallback = null;
    private DateDialog.Callback endTimeCallback = null;
    private long startTime = CommonUtil.getCurrentTime();
    private long endTime = CommonUtil.getCurrentTime();
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
    private Button startTimeBtn;
    private Button endTimeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.cache_manage_title);
        setOptionsButtonInVisible();
        initView();
        viewUtil = new ViewUtil();
        trackApp = (TrackApplication) getApplicationContext();
        trackListener = new OnTrackListener() {
            @Override
            public void onQueryCacheTrackCallback(QueryCacheTrackResponse response) {
                viewUtil.showToast(CacheManageActivity.this, response.toString());
            }

            @Override
            public void onClearCacheTrackCallback(ClearCacheTrackResponse response) {
                viewUtil.showToast(CacheManageActivity.this, response.toString());
            }
        };

    }

    public void initView() {
        startTimeBtn = (Button) findViewById(R.id.start_time);
        endTimeBtn = (Button) findViewById(R.id.end_time);

        StringBuilder startTimeBuilder = new StringBuilder();
        startTimeBuilder.append(getResources().getString(R.string.start_time));
        startTimeBuilder.append(simpleDateFormat.format(System.currentTimeMillis()));
        startTimeBtn.setText(startTimeBuilder.toString());

        StringBuilder endTimeBuilder = new StringBuilder();
        endTimeBuilder.append(getResources().getString(R.string.end_time));
        endTimeBuilder.append(simpleDateFormat.format(System.currentTimeMillis()));
        endTimeBtn.setText(endTimeBuilder.toString());
    }

    /**
     * 查询缓存轨迹
     *
     * @param v
     */
    public void onQueryCacheTrack(View v) {
        if (trackApp.mClient == null) {
            return;
        }
        QueryCacheTrackRequest request = new QueryCacheTrackRequest(trackApp.getTag(), trackApp.serviceId,
                trackApp.entityName);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        trackApp.mClient.queryCacheTrack(request, trackListener);
    }


    public void onStartTime(View v) {
        if (null == startTimeCallback) {
            startTimeCallback = new DateDialog.Callback() {
                @Override
                public void onDateCallback(long timeStamp) {
                    startTime = timeStamp;
                    StringBuilder startTimeBuilder = new StringBuilder();
                    startTimeBuilder.append(getResources().getString(R.string.start_time));
                    startTimeBuilder.append(simpleDateFormat.format(timeStamp * 1000));
                    startTimeBtn.setText(startTimeBuilder.toString());
                }
            };
        }
        if (null == dateDialog) {
            dateDialog = new DateDialog(this, startTimeCallback);
        } else {
            dateDialog.setCallback(startTimeCallback);
        }
        dateDialog.show();
    }

    public void onEndTime(View v) {
        if (null == endTimeCallback) {
            endTimeCallback = new DateDialog.Callback() {
                @Override
                public void onDateCallback(long timeStamp) {
                    endTime = timeStamp;
                    StringBuilder endTimeBuilder = new StringBuilder();
                    endTimeBuilder.append(getResources().getString(R.string.end_time));
                    endTimeBuilder.append(simpleDateFormat.format(timeStamp * 1000));
                    endTimeBtn.setText(endTimeBuilder.toString());
                }
            };
        }
        if (null == dateDialog) {
            dateDialog = new DateDialog(this, endTimeCallback);
        } else {
            dateDialog.setCallback(endTimeCallback);
        }
        dateDialog.show();
    }

    /**
     * 清除缓存轨迹
     *
     * @param v
     */
    public void onClearCacheTrack(View v) {
        if (trackApp.mClient == null) {
            return;
        }
        List<String> entityNames = new ArrayList<>();
        entityNames.add(trackApp.entityName);
        ClearCacheTrackRequest request = new ClearCacheTrackRequest(trackApp.getTag(), trackApp.serviceId, entityNames);
        trackApp.mClient.clearCacheTrack(request, trackListener);
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_cache_manage;
    }

}
