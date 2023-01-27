package com.baidu.track.activity;

import android.os.Bundle;
import android.view.View;

import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolygonOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.district.DistrictResult;
import com.baidu.mapapi.search.district.DistrictSearch;
import com.baidu.mapapi.search.district.DistrictSearchOption;
import com.baidu.mapapi.search.district.OnGetDistricSearchResultListener;
import com.baidu.trace.api.entity.AroundSearchRequest;
import com.baidu.trace.api.entity.AroundSearchResponse;
import com.baidu.trace.api.entity.BoundSearchRequest;
import com.baidu.trace.api.entity.BoundSearchResponse;
import com.baidu.trace.api.entity.DistrictSearchRequest;
import com.baidu.trace.api.entity.DistrictSearchResponse;
import com.baidu.trace.api.entity.EntityInfo;
import com.baidu.trace.api.entity.FilterCondition;
import com.baidu.trace.api.entity.OnEntityListener;
import com.baidu.trace.api.entity.ReturnType;
import com.baidu.trace.model.CoordType;
import com.baidu.trace.model.LatLng;
import com.baidu.track.R;
import com.baidu.track.TrackApplication;
import com.baidu.track.utils.MapUtil;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends BaseActivity implements View.OnClickListener, OnGetDistricSearchResultListener {

    private TrackApplication trackApp = null;
    private MapUtil mMapUtil = null;
    private FilterCondition filterCondition;
    private BitmapDescriptor mBitmapCar = BitmapDescriptorFactory.fromResource(R.drawable.car);

    // 请求标识
    private int tag = 5;
    // 设置活跃时间
    private long activeTime = System.currentTimeMillis() / 1000 - 5 * 60;
    // 圆心坐标类型
    private CoordType coordTypeInput = CoordType.bd09ll;
    // 返回结果坐标类型
    private CoordType coordTypeOutput = CoordType.bd09ll;
    // 中心点
    private LatLng center = new LatLng(40.0569, 116.307553);
    // 左下角
    private LatLng lowerLeft = new LatLng(40.026293, 116.283389);
    // 左上角
    private LatLng upperLeft = new LatLng(40.076646, 116.283389);
    // 右上角
    private LatLng upperRight = new LatLng(40.076646, 116.326298);
    // 右下角
    private LatLng lowerRight = new LatLng(40.026293, 116.326298);
    // 检索半径
    private double radius = 1000;
    // 分页索引
    private int pageIndex = 1;
    // 分页大小
    private int pageSize = 100;
    // 行政区域地址
    private String districtStr = "北京市海淀区";
    // 行政区域检索
    private DistrictSearch mDistrictSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setOptionsButtonInVisible();
        setTitle(R.string.search_title);

        initView();
    }

    private void initView() {
        trackApp = (TrackApplication) getApplicationContext();
        mMapUtil = MapUtil.getInstance();
        mMapUtil.init((MapView) findViewById(R.id.mapView));
        findViewById(R.id.nearby_search_btn).setOnClickListener(this);
        findViewById(R.id.bound_search_btn).setOnClickListener(this);
        findViewById(R.id.district_search_btn).setOnClickListener(this);

        // 过滤条件
        filterCondition = new FilterCondition();
        // 查找当前时间5分钟之前有定位信息上传的entity
        filterCondition.setInActiveTime(activeTime);
        // 初始化检索对象
        mDistrictSearch = DistrictSearch.newInstance();
        mDistrictSearch.setOnDistrictSearchListener(this);
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_search;
    }

    List<com.baidu.mapapi.model.LatLng> districtLocations;

    @Override
    public void onClick(View v) {
        if (trackApp.mClient == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.nearby_search_btn:
                // 根据圆心半径和筛选条件进行搜索
                AroundSearchRequest aroundSearchRequest = new AroundSearchRequest(tag, trackApp.serviceId,
                        center, radius, coordTypeInput, filterCondition, coordTypeOutput, pageIndex, pageSize);
                OnEntityListener onEntityAroundListener = new OnEntityListener() {
                    @Override
                    public void onAroundSearchCallback(AroundSearchResponse aroundSearchResponse) {
                        super.onAroundSearchCallback(aroundSearchResponse);
                        // 周边搜索回调接口
                        if (aroundSearchResponse == null || aroundSearchResponse.getEntities() == null) {
                            return;
                        }
                        List<com.baidu.mapapi.model.LatLng> aroundLocations = new ArrayList<>();
                        List<EntityInfo> entities = aroundSearchResponse.getEntities();
                        for (EntityInfo entityInfo : entities) {
                            LatLng location = entityInfo.getLatestLocation().getLocation();
                            if (location != null) {
                                com.baidu.mapapi.model.LatLng latLng = mMapUtil.convertTrace2Map(location);
                                aroundLocations.add(latLng);
                            }
                        }

                        showArea(aroundLocations, 0);
                    }
                };
                trackApp.mClient.aroundSearchEntity(aroundSearchRequest, onEntityAroundListener);
                break;
            case R.id.bound_search_btn:
                // 创建矩形检索实例
                BoundSearchRequest boundSearchRequest = new BoundSearchRequest(tag, trackApp.serviceId, lowerLeft,
                        upperRight, coordTypeInput, filterCondition, coordTypeOutput, pageIndex, pageSize);
                OnEntityListener onEntityBoundListener = new OnEntityListener() {
                    @Override
                    public void onBoundSearchCallback(BoundSearchResponse boundSearchResponse) {
                        super.onBoundSearchCallback(boundSearchResponse);
                        // 矩形搜索回调接口
                        if (boundSearchResponse == null || boundSearchResponse.getEntities() == null) {
                            return;
                        }
                        List<com.baidu.mapapi.model.LatLng> boundLocations = new ArrayList<>();
                        List<EntityInfo> entities = boundSearchResponse.getEntities();
                        for (EntityInfo entityInfo : entities) {
                            LatLng location = entityInfo.getLatestLocation().getLocation();
                            com.baidu.mapapi.model.LatLng latLng = mMapUtil.convertTrace2Map(location);
                            boundLocations.add(latLng);
                        }

                        showArea(boundLocations, 1);
                    }
                };
                trackApp.mClient.boundSearchEntity(boundSearchRequest, onEntityBoundListener);
                break;
            case R.id.district_search_btn:
                // 创建行政区检索实例
                DistrictSearchRequest districtSearchRequest = new DistrictSearchRequest(tag, trackApp.serviceId,
                        filterCondition, coordTypeOutput, districtStr, ReturnType.all, pageIndex, pageSize);
                OnEntityListener onEntityDistrictListener = new OnEntityListener() {
                    @Override
                    public void onDistrictSearchCallback(DistrictSearchResponse districtSearchResponse) {
                        super.onDistrictSearchCallback(districtSearchResponse);
                        // 行政区搜索回调接口
                        if (districtSearchResponse == null || districtSearchResponse.getEntities() == null) {
                            return;
                        }
                        districtLocations = new ArrayList<>();
                        List<EntityInfo> entities = districtSearchResponse.getEntities();
                        for (EntityInfo entityInfo : entities) {
                            LatLng location = entityInfo.getLatestLocation().getLocation();
                            if (location != null) {
                                com.baidu.mapapi.model.LatLng latLng = mMapUtil.convertTrace2Map(location);
                                districtLocations.add(latLng);
                            }
                        }
                        mDistrictSearch.searchDistrict(new DistrictSearchOption().cityName("北京市").districtName("海淀区"));
                    }
                };
                trackApp.mClient.districtSearchEntity(districtSearchRequest, onEntityDistrictListener);

                break;
            default:
                break;
        }
    }

    // 展示周边检索结果 showType区分绘制方式 0周边 1矩形
    public void showArea(List<com.baidu.mapapi.model.LatLng> list, int showType) {
        if (list == null || list.size() <= 0) {
            return;
        }
        switch (showType) {
            case 0:
                mMapUtil.baiduMap.clear();
                List<OverlayOptions> nearbyOptions = new ArrayList<>();
                for (com.baidu.mapapi.model.LatLng latLng : list) {
                    MarkerOptions marker = new MarkerOptions().position(latLng).icon(mBitmapCar);
                    nearbyOptions.add(marker);
                }
                mMapUtil.baiduMap.addOverlays(nearbyOptions);
                com.baidu.mapapi.model.LatLng centerModel = mMapUtil.convertTrace2Map(center);
                OverlayOptions ooCircle = new CircleOptions().fillColor(0x66FFE1FF)
                        .center(centerModel)
                        .stroke(new Stroke(3, 0x88FF00FF))
                        .radius(1000);

                mMapUtil.baiduMap.addOverlay(ooCircle);
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(centerModel).zoom(16.0f);
                // 更新地图
                mMapUtil.baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                break;
            case 1:
                mMapUtil.baiduMap.clear();
                List<OverlayOptions> boundsOptions = new ArrayList<>();
                for (com.baidu.mapapi.model.LatLng latLng : list) {
                    MarkerOptions marker = new MarkerOptions().position(latLng).icon(mBitmapCar);
                    boundsOptions.add(marker);
                }
                mMapUtil.baiduMap.addOverlays(boundsOptions);

                List<com.baidu.mapapi.model.LatLng> pointsListA = new ArrayList<com.baidu.mapapi.model.LatLng>();
                pointsListA.add(mMapUtil.convertTrace2Map(lowerLeft));
                pointsListA.add(mMapUtil.convertTrace2Map(lowerRight));
                pointsListA.add(mMapUtil.convertTrace2Map(upperRight));
                pointsListA.add(mMapUtil.convertTrace2Map(upperLeft));
                LatLngBounds.Builder bounds = new LatLngBounds.Builder();
                LatLngBounds latLngBounds = bounds.include(mMapUtil.convertTrace2Map(lowerLeft))
                        .include(mMapUtil.convertTrace2Map(upperRight)).build();
                OverlayOptions ooPolygon = new PolygonOptions().fillColor(0x66FFE1FF)
                        .stroke(new Stroke(3, 0x88FF00FF))
                        .points(pointsListA);
                mMapUtil.baiduMap.addOverlay(ooPolygon);
                MapStatus.Builder builder1 = new MapStatus.Builder();
                builder1.target(latLngBounds.getCenter()).zoom(14.5f);
                // 更新地图
                mMapUtil.baiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder1.build()));
                break;
        }
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
        mBitmapCar.recycle();
        // 清空地图所有的覆盖物
        mDistrictSearch.destroy();
        mMapUtil.clear();
    }

    @Override
    public void onGetDistrictResult(DistrictResult districtResult) {
        mMapUtil.baiduMap.clear();
        if (districtResult != null && districtResult.error == SearchResult.ERRORNO.NO_ERROR) {
            List<List<com.baidu.mapapi.model.LatLng>> polyLines = districtResult.getPolylines();
            if (polyLines == null) {
                return;
            }
            if (districtLocations == null) {
                return;
            }
            List<OverlayOptions> boundsOptions = new ArrayList<>();
            for (com.baidu.mapapi.model.LatLng latLng : districtLocations) {
                MarkerOptions marker = new MarkerOptions().position(latLng).icon(mBitmapCar);
                boundsOptions.add(marker);
            }
            mMapUtil.baiduMap.addOverlays(boundsOptions);

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (List<com.baidu.mapapi.model.LatLng> polyline : polyLines) {
                OverlayOptions ooPolyline = new PolylineOptions()
                        .width(10).points(polyline).dottedLine(true).color(0xAA00FF00);
                mMapUtil.baiduMap.addOverlay(ooPolyline);
                OverlayOptions ooPolygon = new PolygonOptions().points(polyline).stroke(new Stroke(3, 0xFFFF00FF))
                        .fillColor(0x66FFE1FF);
                mMapUtil.baiduMap.addOverlay(ooPolygon);
                for (com.baidu.mapapi.model.LatLng latLng : polyline) {
                    builder.include(latLng);
                }
            }
            mMapUtil.baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLngBounds(builder.build()));
        }
    }
}
