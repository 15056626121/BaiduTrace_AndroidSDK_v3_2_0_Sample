package com.baidu.track.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.trace.api.analysis.DrivingBehaviorRequest;
import com.baidu.trace.api.analysis.DrivingBehaviorResponse;
import com.baidu.trace.api.analysis.HarshAccelerationPoint;
import com.baidu.trace.api.analysis.HarshSteeringPoint;
import com.baidu.trace.api.analysis.OnAnalysisListener;
import com.baidu.trace.api.analysis.SpeedingInfo;
import com.baidu.trace.api.analysis.SpeedingPoint;
import com.baidu.trace.api.analysis.StayPoint;
import com.baidu.trace.api.analysis.StayPointRequest;
import com.baidu.trace.api.analysis.StayPointResponse;
import com.baidu.trace.api.analysis.ThresholdOption;
import com.baidu.trace.api.track.DistanceResponse;
import com.baidu.trace.api.track.HistoryTrackRequest;
import com.baidu.trace.api.track.HistoryTrackResponse;
import com.baidu.trace.api.track.LatestPointResponse;
import com.baidu.trace.api.track.OnTrackListener;
import com.baidu.trace.api.track.SupplementContent;
import com.baidu.trace.api.track.SupplementMode;
import com.baidu.trace.api.track.TrackPoint;
import com.baidu.trace.model.CoordType;
import com.baidu.trace.model.Point;
import com.baidu.trace.model.ProcessOption;
import com.baidu.trace.model.SortType;
import com.baidu.trace.model.StatusCodes;
import com.baidu.trace.model.TransportMode;
import com.baidu.track.R;
import com.baidu.track.TrackApplication;
import com.baidu.track.dialog.TrackAnalysisInfoLayout;
import com.baidu.track.utils.BitmapUtil;
import com.baidu.track.utils.CommonUtil;
import com.baidu.track.utils.Constants;
import com.baidu.track.utils.MapUtil;
import com.baidu.track.utils.ViewUtil;

import java.util.ArrayList;
import java.util.List;

public class TrackAnalyseActivity extends BaseActivity implements
        BaiduMap.OnMarkerClickListener, View.OnClickListener {

    private TrackApplication trackApp = null;
    private MapUtil mMapUtil = null;

    // 页面TextView
    private TextView tvSpeeding;
    private TextView tvRapidShift;
    private TextView tvSharpTurn;
    private TextView tvStay;
    private TextView tvSpeedingStr;
    private TextView tvRapidShiftStr;
    private TextView tvSharpTurnStr;
    private TextView tvStayStr;

    /**
     * 查询轨迹的开始时间
     */
    private long startTime = CommonUtil.getCurrentTime();

    /**
     * 查询轨迹的结束时间
     */
    private long endTime = CommonUtil.getCurrentTime();

    /**
     * 历史轨迹请求
     */
    HistoryTrackRequest historyTrackRequest = new HistoryTrackRequest();

    /**
     * 创建纠偏选项实例
     */
    ProcessOption processOption = new ProcessOption();

    /**
     * 轨迹排序规则
     */
    private SortType sortType = SortType.asc;

    int pageIndex = 1;

    /**
     * 轨迹分析监听器
     */
    private OnAnalysisListener mAnalysisListener = null;

    /**
     * 轨迹监听器（用于接收历史轨迹回调）
     */
    private OnTrackListener mTrackListener = null;

    /**
     * 轨迹点集合
     */
    private List<com.baidu.mapapi.model.LatLng> trackPoints = new ArrayList<>();

    /**
     * 轨迹点集合
     */
    private List<List<com.baidu.mapapi.model.LatLng>> trackPointsList = new ArrayList<>();

    /**
     * 轨迹查询纠偏点集合
     */
    private List<List<com.baidu.mapapi.model.LatLng>> correctPointsList = new ArrayList<>();

    /**
     * 轨迹分析  超速点集合
     */
    private List<Point> speedingPoints = new ArrayList<>();

    /**
     * 轨迹分析  急加速点集合
     */
    private List<Point> harshAccelPoints = new ArrayList<>();

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
     * 轨迹分析  急转弯点覆盖物集合
     */
    private List<Marker> harshSteeringMarkers = new ArrayList<>();

    /**
     * 轨迹分析  停留点覆盖物集合
     */
    private List<Marker> stayPointMarkers = new ArrayList<>();

    /**
     * 轨迹分析详情框布局
     */
    private TrackAnalysisInfoLayout trackAnalysisInfoLayout = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.track_analyse_title);
        setOnClickListener(this);
        initView();
    }

    private void initView() {
        trackApp = (TrackApplication) getApplicationContext();
        mMapUtil = MapUtil.getInstance();
        mMapUtil.init((MapView) findViewById(R.id.mapView));
        mMapUtil.baiduMap.setOnMarkerClickListener(this);
        tvSpeeding = findViewById(R.id.tv_speeding);
        tvRapidShift = findViewById(R.id.tv_rapid_shift);
        tvSharpTurn = findViewById(R.id.tv_sharp_turn);
        tvStay = findViewById(R.id.tv_stay);
        tvSpeedingStr = findViewById(R.id.tv_speeding_str);
        tvRapidShiftStr = findViewById(R.id.tv_rapid_shift_str);
        tvSharpTurnStr = findViewById(R.id.tv_sharp_turn_str);
        tvStayStr = findViewById(R.id.tv_stay_str);

        trackAnalysisInfoLayout = new TrackAnalysisInfoLayout(TrackAnalyseActivity.this, mMapUtil.baiduMap);

        initListener();

        // 历史轨迹请求
//        queryHistoryTrack();
    }

    private void initListener() {

        // 历史轨迹回调
        mTrackListener = new OnTrackListener() {
            @Override
            public void onHistoryTrackCallback(HistoryTrackResponse response) {
                int total = response.getTotal();
                if (StatusCodes.SUCCESS != response.getStatus()) {
                    Toast.makeText(TrackAnalyseActivity.this, response.getMessage(), Toast.LENGTH_SHORT).show();
                } else if (0 == total) {
                    Toast.makeText(TrackAnalyseActivity.this,
                            getString(R.string.no_track_data), Toast.LENGTH_SHORT).show();
                } else {
                    List<TrackPoint> points = response.getTrackPoints();
                    if (null != points) {
                        TrackPoint tempTrack = null;
                        boolean ifChange = true;
                        List<com.baidu.mapapi.model.LatLng> tempPoint = null;
                        List<com.baidu.mapapi.model.LatLng> correctTempPoint = null;
                        for (TrackPoint trackPoint : points) {
                            if (ifChange || tempPoint == null) {
                                tempPoint = new ArrayList<>();
                            }

                            if (CommonUtil.isZeroPoint(trackPoint.getLocation().getLatitude(),
                                    trackPoint.getLocation().getLongitude())) {
                                continue;
                            }

                            trackPoints.add(MapUtil.convertTrace2Map(trackPoint.getLocation()));
                            if (tempTrack == null) {
                                tempPoint.add(MapUtil.convertTrace2Map(trackPoint.getLocation()));
                            } else {
                                if (trackPoint.getSupplement() == 2) {
                                    if (correctTempPoint == null) {
                                        correctTempPoint = new ArrayList<>();
                                    }
                                    correctTempPoint.add(MapUtil.convertTrace2Map(trackPoint.getLocation()));
                                } else {
                                    ifChange = mMapUtil.locTimeMinutes(
                                            tempTrack.getLocTime(), trackPoint.getLocTime());

                                    if (ifChange) {
                                        trackPointsList.add(tempPoint);
                                    } else {
                                        if (Math.abs(trackPoint.getLocation().getLatitude() -
                                                tempTrack.getLocation().getLatitude()) > 0.000001
                                                && Math.abs(trackPoint.getLocation().getLongitude() -
                                                tempTrack.getLocation().getLongitude()) > 0.000001) {
                                            tempPoint.add(MapUtil.convertTrace2Map(trackPoint.getLocation()));
                                        }
                                    }

                                    if (correctTempPoint != null && correctTempPoint.size() > 1) {
                                        correctPointsList.add(correctTempPoint);
                                        correctTempPoint = null;
                                    }
                                }

                            }

                            tempTrack = trackPoint;
                        }

                        if (tempPoint != null && tempPoint.size() > 1) {
                            trackPointsList.add(tempPoint);
                        }

                        if (correctTempPoint != null && correctTempPoint.size() > 1) {
                            correctPointsList.add(correctTempPoint);
                        }
                    }
                }

                if (total > Constants.PAGE_SIZE * pageIndex) {
                    historyTrackRequest.setPageIndex(++pageIndex);
                    queryHistoryTrack();
                } else {
                    if (correctPointsList != null && correctPointsList.size() > 0) {
                        for (List<com.baidu.mapapi.model.LatLng> correctPoints : correctPointsList) {
                            mMapUtil.drawPolyline(correctPoints);
                        }
                    }

                    if (trackPointsList != null && trackPointsList.size() > 0) {
                        for (List<com.baidu.mapapi.model.LatLng> tPoints : trackPointsList) {
                            mMapUtil.drawHistoryTrackOrMarker(tPoints);
                        }
                    }

                    if (trackPoints != null && trackPoints.size() > 0) {
                        mMapUtil.startAndEndMarker(trackPoints, sortType);
                        mMapUtil.animateMapStatus(trackPoints);
                    }
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

        // 初始化停留点监听
        mAnalysisListener = new OnAnalysisListener() {
            @Override
            public void onStayPointCallback(StayPointResponse response) {
                // 停留点分析回调
                if (StatusCodes.SUCCESS != response.getStatus()) {
                    Toast.makeText(TrackAnalyseActivity.this, response.getMessage(), Toast.LENGTH_SHORT).show();
                    tvStay.setText("0");
                    tvStayStr.setTextColor(getResources().getColor(R.color.black));
                    return;
                }
                tvStay.setText(response.getStayPointNum() + "");
                if (response.getStayPointNum() > 0) {
                    tvStayStr.setTextColor(getResources().getColor(R.color.blue));
                } else {
                    tvStayStr.setTextColor(getResources().getColor(R.color.black));
                }
                if (0 == response.getStayPointNum()) {
                    return;
                }

                stayPoints.addAll(response.getStayPoints());
                handleOverlays(stayPointMarkers, stayPoints);

            }

            @Override
            public void onDrivingBehaviorCallback(DrivingBehaviorResponse response) {
                // 驾驶行为分析回调
                if (StatusCodes.SUCCESS != response.getStatus()) {
                    Toast.makeText(TrackAnalyseActivity.this, response.getMessage(), Toast.LENGTH_SHORT).show();
                    tvSpeeding.setText("0");
                    tvRapidShift.setText("0");
                    tvSharpTurn.setText("0");
                    tvSpeedingStr.setTextColor(getResources().getColor(R.color.black));
                    tvRapidShiftStr.setTextColor(getResources().getColor(R.color.black));
                    tvSharpTurnStr.setTextColor(getResources().getColor(R.color.black));
                    return;
                }

                tvSpeeding.setText(response.getSpeedingNum() + "");
                tvRapidShift.setText(response.getHarshAccelerationNum() + "");
                tvSharpTurn.setText(response.getHarshSteeringNum() + "");

                if (response.getSpeedingNum() > 0) {
                    tvSpeedingStr.setTextColor(getResources().getColor(R.color.blue));
                } else {
                    tvSpeedingStr.setTextColor(getResources().getColor(R.color.black));
                }
                if (response.getHarshAccelerationNum() > 0) {
                    tvRapidShiftStr.setTextColor(getResources().getColor(R.color.blue));
                } else {
                    tvRapidShiftStr.setTextColor(getResources().getColor(R.color.black));
                }
                if (response.getHarshSteeringNum() > 0) {
                    tvSharpTurnStr.setTextColor(getResources().getColor(R.color.blue));
                } else {
                    tvSharpTurnStr.setTextColor(getResources().getColor(R.color.black));
                }

                if (0 == response.getSpeedingNum() && 0 == response.getHarshAccelerationNum()
                        && 0 == response.getHarshBreakingNum() && 0 == response.getHarshSteeringNum()) {
                    return;
                }

                List<SpeedingInfo> speedingInfos = response.getSpeedings();
                for (SpeedingInfo speedingInfo : speedingInfos) {
                    List<SpeedingPoint> points = speedingInfo.getPoints();
                    speedingPoints.add(points.get(0));
                }
                harshAccelPoints.addAll(response.getHarshAccelerationPoints());
                harshSteeringPoints.addAll(response.getHarshSteeringPoints());

                handleOverlays(speedingMarkers, speedingPoints);
                handleOverlays(harshAccelMarkers, harshAccelPoints);
                handleOverlays(harshSteeringMarkers, harshSteeringPoints);

            }
        };
    }

    /**
     * 轨迹分析覆盖物点击事件
     *
     * @param marker
     * @return
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        Bundle bundle = marker.getExtraInfo();
        // 如果bundle为空或者marker不可见，则过滤点击事件
        if (null == bundle) {
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

        // 创建InfoWindow , 传入 view， 地理坐标， y 轴偏移量
        InfoWindow trackAnalysisInfoWindow = new InfoWindow(trackAnalysisInfoLayout.mView, marker.getPosition(), -47);
        // 显示InfoWindow
        mMapUtil.baiduMap.showInfoWindow(trackAnalysisInfoWindow);

        return false;
    }

    /**
     * 清除驾驶行为分析覆盖物
     */
    public void clearAnalysisOverlay() {
        clearOverlays(speedingMarkers);
        clearOverlays(harshAccelMarkers);
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

    private void clearAnalysisList() {
        if (null != speedingPoints) {
            speedingPoints.clear();
        }
        if (null != harshAccelPoints) {
            harshAccelPoints.clear();
        }
        if (null != harshSteeringPoints) {
            harshSteeringPoints.clear();
        }
    }

    public void queryHistoryTrack() {
        if (trackApp.mClient == null) {
            return;
        }
        // 创建停留点查询请求实例
        StayPointRequest stayPointRequest = new StayPointRequest();
        trackApp.initRequest(stayPointRequest);
        stayPointRequest.setEntityName(trackApp.entityName);
        stayPointRequest.setStartTime(startTime);
        stayPointRequest.setEndTime(endTime);
        stayPointRequest.setProcessOption(processOption);
        stayPointRequest.setStayTime(Constants.STAY_TIME);
        stayPointRequest.setStayRadius(20);
        stayPointRequest.setCoordTypeOutput(CoordType.bd09ll);
        // 停留点查询
        trackApp.mClient.queryStayPoint(stayPointRequest, mAnalysisListener);

        // 设置限速值
        ThresholdOption thresholdOption = new ThresholdOption();
        // 创建行为查询请求实例
        DrivingBehaviorRequest drivingBehaviorRequest = new DrivingBehaviorRequest(trackApp.getTag(),
                trackApp.serviceId, trackApp.entityName, startTime, endTime,
                thresholdOption, processOption, CoordType.bd09ll);
        // 行为查询
        trackApp.mClient.queryDrivingBehavior(drivingBehaviorRequest, mAnalysisListener);

        // 创建历史轨迹请求实例
        trackApp.initRequest(historyTrackRequest);
        historyTrackRequest.setEntityName(trackApp.entityName);
        historyTrackRequest.setStartTime(startTime);
        historyTrackRequest.setEndTime(endTime);
        historyTrackRequest.setPageIndex(pageIndex);
        historyTrackRequest.setPageSize(Constants.PAGE_SIZE);
        trackApp.mClient.queryHistoryTrack(historyTrackRequest, mTrackListener);
    }

    /**
     * 处理轨迹分析覆盖物
     *
     * @param markers
     * @param points
     */
    private void handleOverlays(List<Marker> markers, List<? extends Point> points) {
        if (null == markers || null == points) {
            return;
        }
        for (Point point : points) {

            BitmapDescriptor bitmap = null;
            Bundle bundle = new Bundle();

            if (point instanceof SpeedingPoint) {
                SpeedingPoint speedingPoint = (SpeedingPoint) point;
                bundle.putInt("type", R.id.chk_speeding);
                bundle.putDouble("actualSpeed", speedingPoint.getActualSpeed());
                bundle.putDouble("limitSpeed", speedingPoint.getLimitSpeed());
                bitmap = BitmapUtil.bmCs;
            } else if (point instanceof HarshAccelerationPoint) {
                HarshAccelerationPoint accelPoint = (HarshAccelerationPoint) point;
                bundle.putInt("type", R.id.chk_harsh_accel);
                bundle.putDouble("acceleration", accelPoint.getAcceleration());
                bundle.putDouble("initialSpeed", accelPoint.getInitialSpeed());
                bundle.putDouble("endSpeed", accelPoint.getEndSpeed());
                bitmap = BitmapUtil.bmJsc;
            } else if (point instanceof HarshSteeringPoint) {
                HarshSteeringPoint steeringPoint = (HarshSteeringPoint) point;
                bundle.putInt("type", R.id.chk_harsh_steering);
                bundle.putDouble("centripetalAcceleration", steeringPoint.getCentripetalAcceleration());
                bundle.putString("turnType", steeringPoint.getTurnType().name());
                bundle.putDouble("turnSpeed", steeringPoint.getTurnSpeed());
                bitmap = BitmapUtil.bmJzw;
            } else if (point instanceof StayPoint) {
                StayPoint stayPoint = (StayPoint) point;
                bundle.putInt("type", R.id.chk_stay_point);
                bundle.putLong("startTime", stayPoint.getStartTime());
                bundle.putLong("endTime", stayPoint.getEndTime());
                bundle.putInt("duration", stayPoint.getDuration());
                bitmap = BitmapUtil.bmStay;
            }

            OverlayOptions overlayOptions = new MarkerOptions()
                    .position(MapUtil.convertTrace2Map(point.getLocation()))
                    .icon(bitmap).zIndex(9).draggable(true);

            Marker marker = (Marker) mMapUtil.baiduMap.addOverlay(overlayOptions);
            marker.setExtraInfo(bundle);
            markers.add(marker);
        }

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

        mMapUtil.baiduMap.clear();
        trackPoints.clear();
        trackPointsList.clear();
        correctPointsList.clear();
        pageIndex = 1;

        if (data.hasExtra("startTime")) {
            startTime = data.getLongExtra("startTime", CommonUtil.getCurrentTime());
        }
        if (data.hasExtra("endTime")) {
            endTime = data.getLongExtra("endTime", CommonUtil.getCurrentTime());
        }

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

    @Override
    protected int getContentViewId() {
        return R.layout.activity_track_analyse;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapUtil.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapUtil.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != trackAnalysisInfoLayout) {
            trackAnalysisInfoLayout = null;
        }
        if (null != trackPointsList) {
            trackPointsList.clear();
        }
        if (null != stayPoints) {
            stayPoints.clear();
        }
        if (null != trackPoints) {
            trackPoints.clear();
        }
        clearAnalysisList();
        trackPoints = null;
        trackPointsList = null;
        speedingPoints = null;
        harshAccelPoints = null;
        harshSteeringPoints = null;
        stayPoints = null;

        clearAnalysisOverlay();
        speedingMarkers = null;
        harshAccelMarkers = null;
        stayPointMarkers = null;

        mMapUtil.clear();
    }
}
