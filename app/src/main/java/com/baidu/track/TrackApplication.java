package com.baidu.track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.Trace;
import com.baidu.trace.api.entity.LocRequest;
import com.baidu.trace.api.entity.OnEntityListener;
import com.baidu.trace.api.track.LatestPointRequest;
import com.baidu.trace.api.track.OnTrackListener;
import com.baidu.trace.model.BaseRequest;
import com.baidu.trace.model.OnCustomAttributeListener;
import com.baidu.trace.model.ProcessOption;
import com.baidu.track.activity.BosActivity;
import com.baidu.track.activity.CacheManageActivity;
import com.baidu.track.activity.DistanceActivity;
import com.baidu.track.activity.FAQActivity;
import com.baidu.track.activity.FenceActivity;
import com.baidu.track.activity.SearchActivity;
import com.baidu.track.activity.TracingActivity;
import com.baidu.track.activity.TrackAnalyseActivity;
import com.baidu.track.activity.TrackQueryActivity;
import com.baidu.track.model.ItemInfo;
import com.baidu.track.utils.CommonUtil;
import com.baidu.track.utils.NetUtil;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * Created by baidu on 17/1/12. https://www.jianshu.com/p/d21a65e06cdb
 */

public class TrackApplication extends Application {

    private static final String TAG = "application pine";

    private AtomicInteger mSequenceGenerator = new AtomicInteger();

    //定位请求类
    private LocRequest locRequest = null;

    private Notification notification = null;

    public Context mContext = null;

    public List<ItemInfo> itemInfos = new ArrayList<>();
    //存储辅助类
    public SharedPreferences trackConf = null;

    /**
     * 轨迹客户端
     */
    public LBSTraceClient mClient = null;

    /**
     * 轨迹服务
     */
    public Trace mTrace = null;

    /**
     * 轨迹服务ID
     */
    public long serviceId = 235633;

    /**
     * Entity标识
     */
    public String entityName = "";

    public boolean isRegisterReceiver = false;

    /**
     * 服务是否开启标识
     */
    public boolean isTraceStarted = false;

    /**
     * 采集是否开启标识
     */
    public boolean isGatherStarted = false;

    public static int screenWidth = 0;

    public static int screenHeight = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        LBSTraceClient.setAgreePrivacy(mContext, true);
        entityName = CommonUtil.getEntityName();

        Log.i(TAG, "onCreate: application start");
        // 若为创建独立进程，则不初始化成员变量
        if ("com.baidu.track:remote".equals(CommonUtil.getCurProcessName(mContext))) {
            return;
        }

        SDKInitializer.initialize(mContext);
        //初始化listview的item
        initView();
        //创建一个通知栏类
        initNotification();
        try {
            mClient = new LBSTraceClient(mContext);
        } catch (Exception e) {
            e.getMessage();
        }
        //记录我的轨迹
        mTrace = new Trace(serviceId, entityName);
        mTrace.setNotification(notification);

        trackConf = getSharedPreferences("track_conf", MODE_PRIVATE);
        locRequest = new LocRequest(serviceId);

        if (mClient != null) {
            //SDK每采集一次轨迹 便会自动回调onTrackAttributeCallback()，获取属性值并写入当前轨迹点的属性字段中，自定义属性数据上传
            mClient.setOnCustomAttributeListener(new OnCustomAttributeListener() {
                @Override
                public Map<String, String> onTrackAttributeCallback() {
                    Map<String, String> map = new HashMap<>();
                    map.put("key1", "value1");
                    map.put("key2", "value2");
                    return map;
                }

                @Override
                public Map<String, String> onTrackAttributeCallback(long locTime) {
                    System.out.println("onTrackAttributeCallback, locTime : " + locTime);
                    Map<String, String> map = new HashMap<>();
                    map.put("key1", "value1");
                    map.put("key2", "value2");
                    return map;
                }
            });
        }
        //清除缓存的trace
        clearTraceStatus();
    }

    /**
     * 获取当前位置
     */
    public void getCurrentLocation(OnEntityListener entityListener, OnTrackListener trackListener) {
        if (mClient == null) {
            return;
        }
        // 网络连接正常，开启服务及采集，则查询纠偏后实时位置；否则进行实时定位
        if (NetUtil.isNetworkAvailable(mContext)
                && trackConf.contains("is_trace_started")
                && trackConf.contains("is_gather_started")
                && trackConf.getBoolean("is_trace_started", false)
                && trackConf.getBoolean("is_gather_started", false)) {
            LatestPointRequest request = new LatestPointRequest(getTag(), serviceId, entityName);
            //算法类，降噪纠偏
            ProcessOption processOption = new ProcessOption();
            processOption.setNeedDenoise(true);
            //获取定位点去噪精度 查询缓存距离时，对缓存的GPS轨迹点进行去噪 取值=0时，则不过滤；当取值大于0的整数时，则过滤掉radius大于设定值的轨迹点
            processOption.setRadiusThreshold(100);
            request.setProcessOption(processOption);
            mClient.queryLatestPoint(request, trackListener);
        } else {
            mClient.queryRealTimeLoc(locRequest, entityListener);
        }
    }

    private void initView() {
        ItemInfo tracing = new ItemInfo(R.mipmap.icon_tracing, R.string.tracing_title, R.string.tracing_desc,
                TracingActivity.class);
        ItemInfo trackQuery = new ItemInfo(R.mipmap.icon_track_query, R.string.track_query_title,
                R.string.track_query_desc, TrackQueryActivity.class);
        ItemInfo fence = new ItemInfo(R.mipmap.icon_fence, R.string.fence_title,
                R.string.fence_desc, FenceActivity.class);
        ItemInfo bos = new ItemInfo(R.mipmap.icon_bos, R.string.bos_title, R.string.bos_desc, BosActivity.class);
        ItemInfo cacheManage = new ItemInfo(R.mipmap.icon_cache_manage,
                R.string.cache_manage_title, R.string.cache_manage_desc, CacheManageActivity.class);
        ItemInfo search = new ItemInfo(R.mipmap.icon_area_search, R.string.search_title,
                R.string.search_desc, SearchActivity.class);
        ItemInfo analyse = new ItemInfo(R.mipmap.icon_analysis,
                R.string.track_analyse_title, R.string.track_analyse_desc, TrackAnalyseActivity.class);
        ItemInfo mileage = new ItemInfo(R.mipmap.icon_distance,
                R.string.mileage_compensation_title, R.string.mileage_compensation_desc, DistanceActivity.class);
        ItemInfo faq = new ItemInfo(R.mipmap.icon_fag, R.string.fag_title, R.string.faq_desc, FAQActivity.class);
        itemInfos.add(tracing);
        itemInfos.add(trackQuery);
        itemInfos.add(fence);
        itemInfos.add(bos);
        itemInfos.add(cacheManage);
        itemInfos.add(search);
        itemInfos.add(analyse);
        itemInfos.add(mileage);
        itemInfos.add(faq);

        getScreenSize();
    }

    @TargetApi(16)
    private void initNotification() {
        Notification.Builder builder = new Notification.Builder(this);
        Intent notificationIntent = new Intent(this, TracingActivity.class);

        Bitmap icon = BitmapFactory.decodeResource(this.getResources(),
                R.mipmap.icon_tracing);

        //NotificationManager是一个Android系统服务，用于管理和运行所有通知。
        //NotificationManager因为是系统服务，所以不能被实例化，为了把Notification传给它，可以用getSystemService()方法获取一个NotificationManager的引用。
        //在需要通知用户时再调用notify()方法将Notification对象传给它。
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // 设置PendingIntent
        builder.setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0))
                .setLargeIcon(icon)  // 设置下拉列表中的图标(大图标)
                .setContentTitle("百度鹰眼") // 设置下拉列表里的标题
                .setSmallIcon(R.mipmap.icon_tracing) // 设置状态栏内的小图标
                .setContentText("服务正在运行...") // 设置上下文内容
                .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && null != notificationManager) {
            NotificationChannel notificationChannel =
                    new NotificationChannel("trace", "trace_channel",
                            NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(notificationChannel);

            builder.setChannelId("trace"); // Android O版本之后需要设置该通知的channelId
        }

        notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
    }

    /**
     * 获取屏幕尺寸
     */
    private void getScreenSize() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenHeight = dm.heightPixels;
        screenWidth = dm.widthPixels;
    }

    /**
     * 清除Trace状态：初始化app时，判断上次是正常停止服务还是强制杀死进程，根据trackConf中是否有is_trace_started字段进行判断。
     * <p>
     * 停止服务成功后，会将该字段清除；若未清除，表明为非正常停止服务。
     */
    private void clearTraceStatus() {
        if (trackConf.contains("is_trace_started") || trackConf.contains("is_gather_started")) {
            SharedPreferences.Editor editor = trackConf.edit();
            editor.remove("is_trace_started");
            editor.remove("is_gather_started");
            editor.apply();
        }
    }

    /**
     * 初始化请求公共参数
     *
     * @param request
     */
    public void initRequest(BaseRequest request) {
        request.setTag(getTag());
        request.setServiceId(serviceId);
    }

    /**
     * 获取请求标识
     *
     * @return
     */
    public int getTag() {
        return mSequenceGenerator.incrementAndGet();
    }

    public void clear() {
        itemInfos.clear();
    }

}
