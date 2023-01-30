package com.baidu.track.activity;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MapViewLayoutParams;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.trace.api.analysis.DrivingBehaviorRequest;
import com.baidu.trace.api.analysis.DrivingBehaviorResponse;
import com.baidu.trace.api.analysis.HarshAccelerationPoint;
import com.baidu.trace.api.analysis.HarshBreakingPoint;
import com.baidu.trace.api.analysis.HarshSteeringPoint;
import com.baidu.trace.api.analysis.OnAnalysisListener;
import com.baidu.trace.api.analysis.SpeedingInfo;
import com.baidu.trace.api.analysis.SpeedingPoint;
import com.baidu.trace.api.analysis.StayPoint;
import com.baidu.trace.api.analysis.StayPointRequest;
import com.baidu.trace.api.analysis.StayPointResponse;
import com.baidu.trace.api.fence.FenceAlarmPushInfo;
import com.baidu.trace.api.fence.MonitoredAction;
import com.baidu.trace.api.track.DistanceResponse;
import com.baidu.trace.api.track.HistoryTrackRequest;
import com.baidu.trace.api.track.HistoryTrackResponse;
import com.baidu.trace.api.track.LatestPointResponse;
import com.baidu.trace.api.track.OnTrackListener;
import com.baidu.trace.model.OnTraceListener;
import com.baidu.trace.model.Point;
import com.baidu.trace.model.ProcessOption;
import com.baidu.trace.api.track.SupplementMode;
import com.baidu.trace.api.track.TrackPoint;
import com.baidu.trace.model.PushMessage;
import com.baidu.trace.model.SortType;
import com.baidu.trace.model.StatusCodes;
import com.baidu.trace.model.TransportMode;
import com.baidu.trace.model.CoordType;
import com.baidu.track.R;
import com.baidu.track.TrackApplication;
import com.baidu.track.dialog.DateDialog;
import com.baidu.track.dialog.TrackAnalysisDialog;
import com.baidu.track.dialog.TrackAnalysisInfoLayout;
import com.baidu.track.utils.BitmapUtil;
import com.baidu.track.utils.CommonUtil;
import com.baidu.track.utils.Constants;
import com.baidu.track.utils.MapUtil;
import com.baidu.track.utils.SharedPreferenceUtil;
import com.baidu.track.utils.ViewUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 轨迹查询
 */
public class TrackQueryActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener, BaiduMap.OnMarkerClickListener , BaiduMap.OnMapClickListener {

    private static final String TAG = "trackQuery";

    private TrackApplication trackApp = null;

    private ViewUtil viewUtil = null;

    /**
     * 地图工具
     */
    private MapUtil mapUtil = null;

    /**
     * 历史轨迹请求
     */
    private HistoryTrackRequest historyTrackRequest = new HistoryTrackRequest();

    /**
     * 轨迹监听器（用于接收历史轨迹回调）
     */
    private OnTrackListener mTrackListener = null;

    /**
     * 轨迹分析对话框
     */
    private TrackAnalysisDialog trackAnalysisDialog = null;

    /**
     * 轨迹分析详情框布局
     */
    private TrackAnalysisInfoLayout trackAnalysisInfoLayout = null;

    /**
     * 当前轨迹分析详情框对应的marker
     */
    private Marker analysisMarker = null;

    /**
     * 驾驶行为请求
     */
    private DrivingBehaviorRequest drivingBehaviorRequest = new DrivingBehaviorRequest();

    /**
     * 停留点请求
     */
    private StayPointRequest stayPointRequest = new StayPointRequest();

    /**
     * 轨迹分析监听器
     */
    private OnAnalysisListener mAnalysisListener = null;

    private static long  oneDay = 24 * 60 * 60;

    private static long  oneWeek = 7;
    /**
     * 查询轨迹的开始时间
     */
    private long startTime = CommonUtil.getCurrentTime() - oneDay;

    /**
     * 查询轨迹的结束时间
     */
    private long endTime = CommonUtil.getCurrentTime();

    /**
     * 轨迹点集合
     */
    private List<LatLng> trackPoints = new ArrayList<>();

    /**
     * 轨迹分析  超速点集合
     */
    private List<Point> speedingPoints = new ArrayList<>();

    /**
     * 轨迹分析  急加速点集合
     */
    private List<Point> harshAccelPoints = new ArrayList<>();

    /**
     * 轨迹分析  急刹车点集合
     */
    private List<Point> harshBreakingPoints = new ArrayList<>();

    /**
     * 轨迹分析  急转弯点集合
     */
    private List<Point> harshSteeringPoints = new ArrayList<>();

    /**
     * 轨迹分析  停留点集合
     */
    private List<Point> stayPoints = new ArrayList<>();

    /**
     * 轨迹分析 超速点覆盖物集合
     */
    private List<Marker> speedingMarkers = new ArrayList<>();

    /**
     * 轨迹分析 急加速点覆盖物集合
     */
    private List<Marker> harshAccelMarkers = new ArrayList<>();

    /**
     * 轨迹分析  急刹车点覆盖物集合
     */
    private List<Marker> harshBreakingMarkers = new ArrayList<>();

    /**
     * 轨迹分析  急转弯点覆盖物集合
     */
    private List<Marker> harshSteeringMarkers = new ArrayList<>();

    /**
     * 轨迹分析  停留点覆盖物集合
     */
    private List<Marker> stayPointMarkers = new ArrayList<>();

    /**
     * 是否查询超速点
     */
    private boolean isSpeeding = false;

    /**
     * 是否查询急加速点
     */
    private boolean isHarshAccel = false;

    /**
     * 是否查询急刹车点
     */
    private boolean isHarshBreaking = false;

    /**
     * 是否查询急转弯点
     */
    private boolean isHarshSteering = false;

    /**
     * 是否查询停留点
     */
    private boolean isStayPoint = false;

    /**
     * 轨迹排序规则
     */
    private SortType sortType = SortType.asc;

    private int pageIndex = 1;

    /**
     * 轨迹分析上一次请求时间
     */
    private long lastQueryTime = 0;

    /**
     * 查询结果回调信息展示
     */
    private TextView mHistoryTrackView;

    private DateDialog.Callback startTimeCallback = null;
    private DateDialog.Callback endTimeCallback = null;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
    private DateDialog dateDialog = null;
    private Button startTimeBtn = null;
    private Button endTimeBtn = null;
    private EditText imeiEdit = null;
    private String supplementMode;
    /**
     * 轨迹服务监听器
     */
    private OnTraceListener traceListener = null;

    private NotificationManager notificationManager = null;
    private int notifyId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏设置button
        setOptionsButtonInVisible();
        setTitle(R.string.track_query_title);
        setOnClickListener(this);
        trackApp = (TrackApplication) getApplicationContext();
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        boolean isAccessPermission = SharedPreferenceUtil.getBoolean(TrackQueryActivity.this,
                Constants.PERMISSIONS_DESC_KEY, false);
        if (!isAccessPermission) {
            Toast.makeText(TrackQueryActivity.this, "需要同意隐私条款后才可以采集", Toast.LENGTH_LONG).show();
            return;
        }
        if (trackApp.mClient == null) {
            Toast.makeText(TrackQueryActivity.this,
                    getResources().getString(R.string.privacy_policy_desc), Toast.LENGTH_LONG).show();
            return;
        }
        //必须先开启trace
        trackApp.mClient.startTrace(trackApp.mTrace, traceListener);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        viewUtil = new ViewUtil();
        mapUtil = MapUtil.getInstance();
        mapUtil.init((MapView) findViewById(R.id.track_query_mapView));
        mapUtil.baiduMap.setOnMarkerClickListener(this);
        mapUtil.baiduMap.setOnMapClickListener(this);
        mapUtil.setCenter(trackApp);
        trackAnalysisInfoLayout = new TrackAnalysisInfoLayout(this, mapUtil.baiduMap);

        startTimeBtn = (Button) findViewById(R.id.start_time);
        endTimeBtn = (Button) findViewById(R.id.end_time);
        StringBuilder startTimeBuilder = new StringBuilder();
        startTimeBuilder.append(getResources().getString(R.string.start_time));
        startTimeBuilder.append(simpleDateFormat.format(startTime * 1000));
        startTimeBtn.setText(startTimeBuilder.toString());

        StringBuilder endTimeBuilder = new StringBuilder();
        endTimeBuilder.append(getResources().getString(R.string.end_time));
        endTimeBuilder.append(simpleDateFormat.format(endTime * 1000));
        endTimeBtn.setText(endTimeBuilder.toString());

        imeiEdit = (EditText) findViewById(R.id.IMEI) ;
        initListener();
    }

    /**
     * 轨迹分析
     *
     * @param v
     */
    public void onTrackAnalysis(View v) {
        if (null != mapUtil.mapView) {
            mapUtil.mapView.removeView(mHistoryTrackView);
            mapUtil.baiduMap.setViewPadding(0, 0, 0, 0);
        }
        if (null == trackAnalysisDialog) {
            trackAnalysisDialog = new TrackAnalysisDialog(this);
        }
        // 显示窗口
        trackAnalysisDialog.showAtLocation(v, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        // 处理PopupWindow在Android N系统上的兼容性问题
        if (Build.VERSION.SDK_INT < 24) {
            trackAnalysisDialog.update(trackAnalysisDialog.getWidth(), trackAnalysisDialog.getHeight());
        }
        if (CommonUtil.getCurrentTime() - lastQueryTime > Constants.ANALYSIS_QUERY_INTERVAL) {
            lastQueryTime = CommonUtil.getCurrentTime();
            speedingPoints.clear();
            harshAccelPoints.clear();
            harshBreakingPoints.clear();
            stayPoints.clear();
            queryDrivingBehavior();
            queryStayPoint();
        }

    }

    public void onStartTime(View v) {
        if (null == startTimeCallback) {
            //设置选择时间区间
            startTimeCallback = new DateDialog.Callback() {
                @Override
                public void onDateCallback(long timeStamp) {
                    TrackQueryActivity.this.startTime = timeStamp;
                    if ((endTime - startTime > oneWeek * oneDay) || (endTime - startTime < 0)) {
                        endTime = startTime + oneWeek * oneDay;

                        StringBuilder startTimeBuilder = new StringBuilder();
                        startTimeBuilder.append(getResources().getString(R.string.end_time));
                        startTimeBuilder.append(simpleDateFormat.format(endTime * 1000));
                        endTimeBtn.setText(startTimeBuilder.toString());
                        Log.i(TAG, "onDateCallback: the max query interval time limit in on week");
                    }
                    
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
                    TrackQueryActivity.this.endTime = timeStamp;

                    if ((endTime - startTime > oneWeek * oneDay) || (endTime - startTime < 0)) {
                        startTime = endTime - oneWeek * oneDay;

                        StringBuilder startTimeBuilder = new StringBuilder();
                        startTimeBuilder.append(getResources().getString(R.string.start_time));
                        startTimeBuilder.append(simpleDateFormat.format( startTime * 1000));
                        startTimeBtn.setText(startTimeBuilder.toString());
                        Log.i(TAG, "onDateCallback: the max query interval time limit in on week");
                    }

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
     * 输入edit控件，获取IMEI作为参数值进行查询
     * */
    public void onInputIMEI(View v) {
        trackApp.entityName = imeiEdit.getText().toString();
        Log.i(TAG, "onInputIMEI: " + trackApp.entityName );
    }

    /**
     * 点击onBeep按钮，通过MQTT使得飞机蜂鸣
     * */
    public void onBeep(View v) {
        Log.i(TAG, "onBeep: MQTT control beep");
    }

    /**
     * 点击查询按钮，触发一次查询
     * */
    public void onQueryTrack(View v) {
        /*查询轨迹后处理*/
        ProcessOption processOption = new ProcessOption();

        /*定位精度过滤：设置定位精度过滤阈值，用于过滤掉定位精度较差的轨迹点*/
        /*定位精度大于 DEFAULT_RADIUS_THRESHOLD 米的过滤掉*/
        processOption.setRadiusThreshold(Constants.DEFAULT_RADIUS_THRESHOLD);

        /*交通方式：可指名轨迹对应交通方式，鹰眼纠偏模块将根据不同的交通方式采用不同的轨迹纠偏处理*/
        processOption.setTransportMode(TransportMode.auto);

        /*可设置是否需要去除角度、速度和方向异常的轨迹点*/
        processOption.setNeedDenoise(false);

        /*设置是否进行抽稀处理，对冗余的轨迹点进行抽稀去除处理，如一条直线上除起终点外的多个轨迹点*/
        processOption.setNeedVacuate(true);

        /*设置是否将轨迹点绑定至路网道路*/
        processOption.setNeedMapMatch(false);
        historyTrackRequest.setProcessOption(processOption);

        /*设置低速阈值, 单位：千米/小时 isProcessed=true时，则返回结果中将增加low_speed_distance字段，表示速度低于该值的里程 */
        historyTrackRequest.setLowSpeedThreshold(Constants.DEFAULT_RADIUS_THRESHOLD);

        /*设置里程补偿方式，默认不补充*/
        historyTrackRequest.setSupplementMode(SupplementMode.no_supplement);

        /*设置返回结果的排序规则，默认升序排序*/
        historyTrackRequest.setSortType(SortType.asc);

        /*设置返回结果的坐标类型，默认为百度经纬度 该字段用于控制返回结果中坐标的类型*/
        historyTrackRequest.setCoordTypeOutput(CoordType.bd09ll);

        /*设置是否返回纠偏后轨迹，默认不纠偏 true：打开轨迹纠偏，返回纠偏后轨迹; false：关闭轨迹纠偏，返回原始轨迹*/
        historyTrackRequest.setProcessed(true);

        queryHistoryTrack();
    }

    /**
     * 轨迹查询设置回调
     *
     * @param historyTrackRequestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int historyTrackRequestCode, int resultCode, Intent data) {
        if (null == data) {
            return;
        }

        trackPoints.clear();
        pageIndex = 1;

        if (data.hasExtra("startTime")) {
            startTime = data.getLongExtra("startTime", CommonUtil.getCurrentTime());
        }
        if (data.hasExtra("endTime")) {
            endTime = data.getLongExtra("endTime", CommonUtil.getCurrentTime());
        }

        ProcessOption processOption = new ProcessOption();
        if (data.hasExtra("radius")) {
            processOption.setRadiusThreshold(data.getIntExtra("radius", Constants.DEFAULT_RADIUS_THRESHOLD));
        }
        if (data.hasExtra("transportMode")) {
            processOption.setTransportMode(TransportMode.valueOf(data.getStringExtra("transportMode")));
        }
        if (data.hasExtra("denoise")) {
            processOption.setNeedDenoise(data.getBooleanExtra("denoise", true));
        }
        if (data.hasExtra("vacuate")) {
            processOption.setNeedVacuate(data.getBooleanExtra("vacuate", true));
        }
        if (data.hasExtra("mapmatch")) {
            processOption.setNeedMapMatch(data.getBooleanExtra("mapmatch", true));
        }
        historyTrackRequest.setProcessOption(processOption);

        if (data.hasExtra("lowspeedthreshold")) {
            historyTrackRequest.setLowSpeedThreshold(data.getIntExtra("lowspeedthreshold",
                    Constants.DEFAULT_RADIUS_THRESHOLD));
        }
        if (data.hasExtra("supplementMode")) {
            historyTrackRequest.setSupplementMode(SupplementMode.valueOf(data.getStringExtra("supplementMode")));
        }
        if (data.hasExtra("sortType")) {
            sortType = SortType.valueOf(data.getStringExtra("sortType"));
            historyTrackRequest.setSortType(sortType);
        }
        if (data.hasExtra("coordTypeOutput")) {
            historyTrackRequest.setCoordTypeOutput(CoordType.valueOf(data.getStringExtra("coordTypeOutput")));
        }
        if (data.hasExtra("processed")) {
            historyTrackRequest.setProcessed(data.getBooleanExtra("processed", true));
        }

        queryHistoryTrack();
    }

    /**
     * 查询历史轨迹
     */
    private void queryHistoryTrack() {
        if (trackApp.mClient == null) {
            return;
        }
        pageIndex = 1;

        trackApp.initRequest(historyTrackRequest);
        historyTrackRequest.setEntityName(trackApp.entityName);
        historyTrackRequest.setPageIndex(pageIndex);
        historyTrackRequest.setPageSize(Constants.PAGE_SIZE);
        long startTimetmp = 0;
        long endTimetmp = 0;
        int  count = 0;
        for (long i = startTime; i < endTime; i += (24 * 60 * 60)) {
            trackPoints.clear();

            count ++;
            startTimetmp = i;
            endTimetmp = startTimetmp + (24 * 60 * 60);

            historyTrackRequest.setStartTime(startTimetmp);
            historyTrackRequest.setEndTime(endTimetmp);
            trackApp.mClient.queryHistoryTrack(historyTrackRequest, mTrackListener);
            Log.i(TAG, "queryHistoryTrack: index: " + count + " entity: " + historyTrackRequest.getEntityName()
                    + " serviceId:" + trackApp.serviceId + " startTime:" + simpleDateFormat.format(startTimetmp * 1000)
                    + " endTime: " + simpleDateFormat.format(endTimetmp * 1000));
            if (count > 10) {
                Log.i(TAG, "queryHistoryTrack: query time too long");
                break;
            }
        }
    }

    /**
     * 查询驾驶行为
     */
    private void queryDrivingBehavior() {
        if (trackApp.mClient == null) {
            return;
        }
        trackApp.initRequest(drivingBehaviorRequest);
        drivingBehaviorRequest.setEntityName(trackApp.entityName);
        drivingBehaviorRequest.setStartTime(startTime);
        drivingBehaviorRequest.setEndTime(endTime);
        trackApp.mClient.queryDrivingBehavior(drivingBehaviorRequest, mAnalysisListener);
    }

    /**
     * 查询停留点
     */
    private void queryStayPoint() {
        if (trackApp.mClient == null) {
            return;
        }
        trackApp.initRequest(stayPointRequest);
        stayPointRequest.setEntityName(trackApp.entityName);
        stayPointRequest.setStartTime(startTime);
        stayPointRequest.setEndTime(endTime);
        stayPointRequest.setStayTime(Constants.STAY_TIME);
        trackApp.mClient.queryStayPoint(stayPointRequest, mAnalysisListener);
    }

    /**
     * 轨迹分析对话框 选项点击事件
     *
     * @param compoundButton
     * @param isChecked
     */
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

        switch (compoundButton.getId()) {
            case R.id.chk_speeding:
                isSpeeding = isChecked;
                handleMarker(speedingMarkers, isSpeeding);
                break;

            case R.id.chk_harsh_breaking:
                isHarshBreaking = isChecked;
                handleMarker(harshBreakingMarkers, isHarshBreaking);
                break;

            case R.id.chk_harsh_accel:
                isHarshAccel = isChecked;
                handleMarker(harshAccelMarkers, isHarshAccel);
                break;

            case R.id.chk_harsh_steering:
                isHarshSteering = isChecked;
                handleMarker(harshSteeringMarkers, isHarshSteering);
                break;

            case R.id.chk_stay_point:
                isStayPoint = isChecked;
                handleMarker(stayPointMarkers, isStayPoint);
                break;

            default:
                break;
        }
    }

    /**
     * 按钮点击事件
     *
     * @param view
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            // 轨迹查询选项
            case R.id.btn_activity_options:
                ViewUtil.startActivityForResult(this, TrackQueryOptionsActivity.class, Constants.REQUEST_CODE);
                break;

            default:
                break;
        }
    }

    /**
     * 轨迹分析覆盖物点击事件
     *
     * @param marker
     *
     * @return
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        Bundle bundle = marker.getExtraInfo();
        // 如果bundle为空或者marker不可见，则过滤点击事件
        if (null == bundle || !marker.isVisible()) {
            return false;
        }
        int type = bundle.getInt("type");
        switch (type) {
            case R.id.chk_speeding:
                trackAnalysisInfoLayout.titleText.setText(R.string.track_analysis_speeding_title);
                trackAnalysisInfoLayout.key1.setText(R.string.actual_speed);
                trackAnalysisInfoLayout.value1.setText(String.valueOf(bundle.getDouble("actualSpeed")));
                trackAnalysisInfoLayout.key2.setText(R.string.limit_speed);
                trackAnalysisInfoLayout.value2.setText(String.valueOf(bundle.getDouble("limitSpeed")));
                break;

            case R.id.chk_harsh_accel:
                trackAnalysisInfoLayout.titleText.setText(R.string.track_analysis_accel_title);
                trackAnalysisInfoLayout.key1.setText(R.string.acceleration);
                trackAnalysisInfoLayout.value1.setText(String.valueOf(bundle.getDouble("acceleration")));
                trackAnalysisInfoLayout.key2.setText(R.string.initial_speed_2);
                trackAnalysisInfoLayout.value2.setText(String.valueOf(bundle.getDouble("initialSpeed")));
                trackAnalysisInfoLayout.key3.setText(R.string.end_speed_2);
                trackAnalysisInfoLayout.value3.setText(String.valueOf(bundle.getDouble("endSpeed")));
                break;

            case R.id.chk_harsh_breaking:
                trackAnalysisInfoLayout.titleText.setText(R.string.track_analysis_breaking_title);
                trackAnalysisInfoLayout.key1.setText(R.string.acceleration);
                trackAnalysisInfoLayout.value1.setText(String.valueOf(bundle.getDouble("acceleration")));
                trackAnalysisInfoLayout.key2.setText(R.string.initial_speed_1);
                trackAnalysisInfoLayout.value2.setText(String.valueOf(bundle.getDouble("initialSpeed")));
                trackAnalysisInfoLayout.key3.setText(R.string.end_speed_1);
                trackAnalysisInfoLayout.value3.setText(String.valueOf(bundle.getDouble("endSpeed")));
                break;

            case R.id.chk_harsh_steering:
                trackAnalysisInfoLayout.titleText.setText(R.string.track_analysis_steering_title);
                trackAnalysisInfoLayout.key1.setText(R.string.centripetal_acceleration);
                trackAnalysisInfoLayout.value1.setText(String.valueOf(bundle.getDouble("centripetalAcceleration")));
                trackAnalysisInfoLayout.key2.setText(R.string.turn_type);
                trackAnalysisInfoLayout.value2.setText(String.valueOf(bundle.getDouble("turnType")));
                trackAnalysisInfoLayout.key3.setText(R.string.turn_speed);
                trackAnalysisInfoLayout.value3.setText(String.valueOf(bundle.getDouble("turnSpeed")));
                break;

            case R.id.chk_stay_point:
                trackAnalysisInfoLayout.titleText.setText(R.string.track_analysis_stay_title);
                trackAnalysisInfoLayout.key1.setText(R.string.stay_start_time);
                trackAnalysisInfoLayout.value1.setText(CommonUtil.formatTime(bundle.getLong("startTime") * 1000));
                trackAnalysisInfoLayout.key2.setText(R.string.stay_end_time);
                trackAnalysisInfoLayout.value2.setText(CommonUtil.formatTime(bundle.getLong("endTime") * 1000));
                trackAnalysisInfoLayout.key3.setText(R.string.stay_duration);
                trackAnalysisInfoLayout.value3.setText(CommonUtil.formatSecond(bundle.getInt("duration")));
                break;

            default:
                break;
        }
        //  保存当前操作的marker
        analysisMarker = marker;

        //创建InfoWindow , 传入 view， 地理坐标， y 轴偏移量
        InfoWindow trackAnalysisInfoWindow = new InfoWindow(trackAnalysisInfoLayout.mView, marker.getPosition(), -47);
        //显示InfoWindow
        mapUtil.baiduMap.showInfoWindow(trackAnalysisInfoWindow);

        return false;
    }

    private void clearAnalysisList() {
        if (null != speedingPoints) {
            speedingPoints.clear();
        }
        if (null != harshAccelPoints) {
            harshAccelPoints.clear();
        }
        if (null != harshBreakingPoints) {
            harshBreakingPoints.clear();
        }
        if (null != harshSteeringPoints) {
            harshSteeringPoints.clear();
        }
    }

    private void initListener() {
        traceListener = new OnTraceListener() {

            /**
             * 绑定服务回调接口
             * @param errorNo  状态码
             * @param message 消息
             *                <p>
             *                <pre>0：成功 </pre>
             *                <pre>1：失败</pre>
             */
            @Override
            public void onBindServiceCallback(int errorNo, String message) {
                viewUtil.showToast(TrackQueryActivity.this,
                        String.format("onBindServiceCallback, errorNo:%d, message:%s ", errorNo, message));
            }

            /**
             * 开启服务回调接口
             * @param errorNo 状态码
             * @param message 消息
             *                <p>
             *                <pre>0：成功 </pre>
             *                <pre>10000：请求发送失败</pre>
             *                <pre>10001：服务开启失败</pre>
             *                <pre>10002：参数错误</pre>
             *                <pre>10003：网络连接失败</pre>
             *                <pre>10004：网络未开启</pre>
             *                <pre>10005：服务正在开启</pre>
             *                <pre>10006：服务已开启</pre>
             */
            @Override
            public void onStartTraceCallback(int errorNo, String message) {
                if (StatusCodes.SUCCESS == errorNo || StatusCodes.START_TRACE_NETWORK_CONNECT_FAILED <= errorNo) {
                    trackApp.isTraceStarted = true;
                    SharedPreferences.Editor editor = trackApp.trackConf.edit();
                    editor.putBoolean("is_trace_started", true);
                    editor.apply();
                }
                viewUtil.showToast(TrackQueryActivity.this,
                        String.format("onStartTraceCallback, errorNo:%d, message:%s ", errorNo, message));
            }

            /**
             * 停止服务回调接口
             * @param errorNo 状态码
             * @param message 消息
             *                <p>
             *                <pre>0：成功</pre>
             *                <pre>11000：请求发送失败</pre>
             *                <pre>11001：服务停止失败</pre>
             *                <pre>11002：服务未开启</pre>
             *                <pre>11003：服务正在停止</pre>
             */
            @Override
            public void onStopTraceCallback(int errorNo, String message) {
                if (StatusCodes.SUCCESS == errorNo || StatusCodes.CACHE_TRACK_NOT_UPLOAD == errorNo) {
                    trackApp.isTraceStarted = false;
                    trackApp.isGatherStarted = false;
                    // 停止成功后，直接移除is_trace_started记录（便于区分用户没有停止服务，直接杀死进程的情况）
                    SharedPreferences.Editor editor = trackApp.trackConf.edit();
                    editor.remove("is_trace_started");
                    editor.remove("is_gather_started");
                    editor.apply();
                }
                viewUtil.showToast(TrackQueryActivity.this,
                        String.format("onStopTraceCallback, errorNo:%d, message:%s ", errorNo, message));
            }

            /**
             * 开启采集回调接口
             * @param errorNo 状态码
             * @param message 消息
             *                <p>
             *                <pre>0：成功</pre>
             *                <pre>12000：请求发送失败</pre>
             *                <pre>12001：采集开启失败</pre>
             *                <pre>12002：服务未开启</pre>
             */
            @Override
            public void onStartGatherCallback(int errorNo, String message) {
                if (StatusCodes.SUCCESS == errorNo || StatusCodes.GATHER_STARTED == errorNo) {
                    trackApp.isGatherStarted = true;
                    SharedPreferences.Editor editor = trackApp.trackConf.edit();
                    editor.putBoolean("is_gather_started", true);
                    editor.apply();
                }
                viewUtil.showToast(TrackQueryActivity.this,
                        String.format("onStartGatherCallback, errorNo:%d, message:%s ", errorNo, message));
            }

            /**
             * 停止采集回调接口
             * @param errorNo 状态码
             * @param message 消息
             *                <p>
             *                <pre>0：成功</pre>
             *                <pre>13000：请求发送失败</pre>
             *                <pre>13001：采集停止失败</pre>
             *                <pre>13002：服务未开启</pre>
             */
            @Override
            public void onStopGatherCallback(int errorNo, String message) {
                if (StatusCodes.SUCCESS == errorNo || StatusCodes.GATHER_STOPPED == errorNo) {
                    trackApp.isGatherStarted = false;
                    SharedPreferences.Editor editor = trackApp.trackConf.edit();
                    editor.remove("is_gather_started");
                    editor.apply();
                    //setGatherBtnStyle();
                }
                viewUtil.showToast(TrackQueryActivity.this,
                        String.format("onStopGatherCallback, errorNo:%d, message:%s ", errorNo, message));
            }

            /**
             * 推送消息回调接口
             *
             * @param messageType 状态码
             * @param pushMessage 消息
             *                  <p>
             *                  <pre>0x01：配置下发</pre>
             *                  <pre>0x02：语音消息</pre>
             *                  <pre>0x03：服务端围栏报警消息</pre>
             *                  <pre>0x04：本地围栏报警消息</pre>
             *                  <pre>0x05~0x40：系统预留</pre>
             *                  <pre>0x41~0xFF：开发者自定义</pre>
             */
            @Override
            public void onPushCallback(byte messageType, PushMessage pushMessage) {
                if (messageType < 0x03 || messageType > 0x04) {
                    viewUtil.showToast(TrackQueryActivity.this, pushMessage.getMessage());
                    return;
                }
                FenceAlarmPushInfo alarmPushInfo = pushMessage.getFenceAlarmPushInfo();
                if (null == alarmPushInfo) {
                    viewUtil.showToast(TrackQueryActivity.this,
                            String.format("onPushCallback, messageType:%d, messageContent:%s ", messageType,
                                    pushMessage));
                    return;
                }
                StringBuffer alarmInfo = new StringBuffer();
                alarmInfo.append("您于")
                        .append(CommonUtil.getHMS(alarmPushInfo.getCurrentPoint().getLocTime() * 1000))
                        .append(alarmPushInfo.getMonitoredAction() == MonitoredAction.enter ? "进入" : "离开")
                        .append(messageType == 0x03 ? "云端" : "本地")
                        .append("围栏：").append(alarmPushInfo.getFenceName());

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    Notification notification = new Notification.Builder(trackApp)
                            .setContentTitle(getResources().getString(R.string.alarm_push_title))
                            .setContentText(alarmInfo.toString())
                            .setSmallIcon(R.mipmap.icon_app)
                            .setWhen(System.currentTimeMillis()).build();
                    notificationManager.notify(notifyId++, notification);
                }
            }

            @Override
            public void onInitBOSCallback(int errorNo, String message) {
                viewUtil.showToast(TrackQueryActivity.this,
                        String.format("onInitBOSCallback, errorNo:%d, message:%s ", errorNo, message));
            }

            @Override
            public void onTraceDataUploadCallBack(int status, String message, int length, int time) {
                viewUtil.showToast(TrackQueryActivity.this,
                        String.format("onTraceDataUploadCallBack, status:%d, message:%s, length:%d, time:%d ",
                                status, message, length, time));
            }
        };

        mTrackListener = new OnTrackListener() {
            @Override
            public void onHistoryTrackCallback(HistoryTrackResponse response) {
                int total = response.getTotal();
                Log.i(TAG, "onHistoryTrackCallback: total track: " + total);
                StringBuffer sb = new StringBuffer(256);
                if (StatusCodes.SUCCESS != response.getStatus()) {
                    viewUtil.showToast(TrackQueryActivity.this, response.getMessage());
                } else if (0 == total) {
                    viewUtil.showToast(TrackQueryActivity.this, getString(R.string.no_track_data));
                    return;
                } else {
                    List<TrackPoint> points = response.getTrackPoints();
                    if (null != points) {
                        for (TrackPoint trackPoint : points) {
                            if (!CommonUtil.isZeroPoint(trackPoint.getLocation().getLatitude(),
                                    trackPoint.getLocation().getLongitude())) {
                                trackPoints.add(MapUtil.convertTrace2Map(trackPoint.getLocation()));
                            }
                        }
                    }
                    sb.append("总里程：");
                    sb.append(response.getDistance());
                    sb.append("米");
                    sb.append("\n收费里程：");
                    sb.append(response.getTollDistance());
                    sb.append("米");
                    sb.append("\n低速里程：");
                    sb.append(response.getLowSpeedDistance());
                    sb.append("米");
                    addView(mapUtil.mapView);
                    mHistoryTrackView.setText(sb.toString());
                }

                if (total > Constants.PAGE_SIZE * pageIndex) {
                    historyTrackRequest.setPageIndex(++pageIndex);
                    queryHistoryTrack();
                } else {
                    mapUtil.drawHistoryTrack(trackPoints, sortType);
                }
            }

            @Override
            public void onDistanceCallback(DistanceResponse response) {
                super.onDistanceCallback(response);
            }

            @Override
            public void onLatestPointCallback(LatestPointResponse response) {
                super.onLatestPointCallback(response);
            }
        };

        mAnalysisListener = new OnAnalysisListener() {
            @Override
            public void onStayPointCallback(StayPointResponse response) {
                if (StatusCodes.SUCCESS != response.getStatus()) {
                    lastQueryTime = 0;
                    viewUtil.showToast(TrackQueryActivity.this, response.getMessage());
                    return;
                }
                if (0 == response.getStayPointNum()) {
                    return;
                }
                stayPoints.addAll(response.getStayPoints());
                handleOverlays(stayPointMarkers, stayPoints, isStayPoint);
            }

            @Override
            public void onDrivingBehaviorCallback(DrivingBehaviorResponse response) {
                if (StatusCodes.SUCCESS != response.getStatus()) {
                    lastQueryTime = 0;
                    viewUtil.showToast(TrackQueryActivity.this, response.getMessage());
                    return;
                }

                if (0 == response.getSpeedingNum() && 0 == response.getHarshAccelerationNum()
                        && 0 == response.getHarshBreakingNum() && 0 == response.getHarshSteeringNum()) {
                    return;
                }

                clearAnalysisList();
                clearAnalysisOverlay();

                List<SpeedingInfo> speedingInfos = response.getSpeedings();
                for (SpeedingInfo info : speedingInfos) {
                    speedingPoints.addAll(info.getPoints());
                }
                harshAccelPoints.addAll(response.getHarshAccelerationPoints());
                harshBreakingPoints.addAll(response.getHarshBreakingPoints());
                harshSteeringPoints.addAll(response.getHarshSteeringPoints());

                handleOverlays(speedingMarkers, speedingPoints, isSpeeding);
                handleOverlays(harshAccelMarkers, harshAccelPoints, isHarshAccel);
                handleOverlays(harshBreakingMarkers, harshBreakingPoints, isHarshBreaking);
                handleOverlays(harshSteeringMarkers, harshSteeringPoints, isHarshSteering);
            }
        };
    }

    /**
     * 处理轨迹分析覆盖物
     *
     * @param markers
     * @param points
     * @param isVisible
     */
    private void handleOverlays(List<Marker> markers, List<? extends com.baidu.trace.model.Point> points, boolean
            isVisible) {
        if (null == markers || null == points) {
            return;
        }
        for (com.baidu.trace.model.Point point : points) {
            OverlayOptions overlayOptions = new MarkerOptions()
                    .position(MapUtil.convertTrace2Map(point.getLocation()))
                    .icon(BitmapUtil.bmGcoding).zIndex(9).draggable(true);
            Marker marker = (Marker) mapUtil.baiduMap.addOverlay(overlayOptions);
            Bundle bundle = new Bundle();

            if (point instanceof SpeedingPoint) {
                SpeedingPoint speedingPoint = (SpeedingPoint) point;
                bundle.putInt("type", R.id.chk_speeding);
                bundle.putDouble("actualSpeed", speedingPoint.getActualSpeed());
                bundle.putDouble("limitSpeed", speedingPoint.getLimitSpeed());

            } else if (point instanceof HarshAccelerationPoint) {
                HarshAccelerationPoint accelPoint = (HarshAccelerationPoint) point;
                bundle.putInt("type", R.id.chk_harsh_accel);
                bundle.putDouble("acceleration", accelPoint.getAcceleration());
                bundle.putDouble("initialSpeed", accelPoint.getInitialSpeed());
                bundle.putDouble("endSpeed", accelPoint.getEndSpeed());

            } else if (point instanceof HarshBreakingPoint) {
                HarshBreakingPoint breakingPoint = (HarshBreakingPoint) point;
                bundle.putInt("type", R.id.chk_harsh_breaking);
                bundle.putDouble("acceleration", breakingPoint.getAcceleration());
                bundle.putDouble("initialSpeed", breakingPoint.getInitialSpeed());
                bundle.putDouble("endSpeed", breakingPoint.getEndSpeed());

            } else if (point instanceof HarshSteeringPoint) {
                HarshSteeringPoint steeringPoint = (HarshSteeringPoint) point;
                bundle.putInt("type", R.id.chk_harsh_steering);
                bundle.putDouble("centripetalAcceleration", steeringPoint.getCentripetalAcceleration());
                bundle.putString("turnType", steeringPoint.getTurnType().name());
                bundle.putDouble("turnSpeed", steeringPoint.getTurnSpeed());

            } else if (point instanceof StayPoint) {
                StayPoint stayPoint = (StayPoint) point;
                bundle.putInt("type", R.id.chk_stay_point);
                bundle.putLong("startTime", stayPoint.getStartTime());
                bundle.putLong("endTime", stayPoint.getEndTime());
                bundle.putInt("duration", stayPoint.getDuration());
            }
            marker.setExtraInfo(bundle);
            markers.add(marker);
        }

        handleMarker(markers, isVisible);
    }

    /**
     * 处理marker
     *
     * @param markers
     * @param isVisible
     */
    private void handleMarker(List<Marker> markers, boolean isVisible) {
        if (null == markers || markers.isEmpty()) {
            return;
        }
        for (Marker marker : markers) {
            marker.setVisible(isVisible);
        }

        if (markers.contains(analysisMarker)) {
            mapUtil.baiduMap.hideInfoWindow();
        }

    }

    /**
     * 添加view用于展示查询里程相关信息。
     *
     * @param mapView MapView
     */
    private void addView(MapView mapView) {
        mHistoryTrackView = new TextView(this);
        mHistoryTrackView.setTextSize(15.0f);
        mHistoryTrackView.setTextColor(Color.BLACK);
        mHistoryTrackView.setBackgroundColor(Color.parseColor("#AAA9A9A9"));
        mHistoryTrackView.setMovementMethod(ScrollingMovementMethod.getInstance());

        MapViewLayoutParams.Builder builder = new MapViewLayoutParams.Builder();
        builder.layoutMode(MapViewLayoutParams.ELayoutMode.absoluteMode);
        builder.width(mapView.getWidth());
        builder.height(200);
        builder.point(new android.graphics.Point(0, mapView.getHeight()));
        builder.align(MapViewLayoutParams.ALIGN_LEFT, MapViewLayoutParams.ALIGN_BOTTOM);
        mapUtil.baiduMap.setViewPadding(0, 0, 0, 200);
        mapView.addView(mHistoryTrackView, builder.build());
    }

    /**
     * 清除驾驶行为分析覆盖物
     */
    public void clearAnalysisOverlay() {
        clearOverlays(speedingMarkers);
        clearOverlays(harshAccelMarkers);
        clearOverlays(harshBreakingMarkers);
        clearOverlays(stayPointMarkers);
    }

    private void clearOverlays(List<Marker> markers) {
        if (null == markers) {
            return;
        }
        for (Marker marker : markers) {
            marker.remove();
        }
        markers.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapUtil.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapUtil.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        trackApp.mClient.stopTrace(trackApp.mTrace, traceListener);

        if (null != trackAnalysisInfoLayout) {
            trackAnalysisInfoLayout = null;
        }
        if (null != trackAnalysisDialog) {
            trackAnalysisDialog.dismiss();
            trackAnalysisDialog = null;
        }
        if (null != trackPoints) {
            trackPoints.clear();
        }
        if (null != stayPoints) {
            stayPoints.clear();
        }
        clearAnalysisList();
        trackPoints = null;
        speedingPoints = null;
        harshAccelPoints = null;
        harshSteeringPoints = null;
        stayPoints = null;

        clearAnalysisOverlay();
        speedingMarkers = null;
        harshAccelMarkers = null;
        harshBreakingMarkers = null;
        stayPointMarkers = null;

        mapUtil.clear();

    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_trackquery;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        mapUtil.mapView.removeView(mHistoryTrackView);
        mapUtil.baiduMap.setViewPadding(0, 0, 0, 0);
    }

    @Override
    public void onMapPoiClick(MapPoi mapPoi) {

    }
}