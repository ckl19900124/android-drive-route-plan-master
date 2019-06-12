package com.amap.driveroute;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.OnInfoWindowClickListener;
import com.amap.api.maps.AMap.OnMapClickListener;
import com.amap.api.maps.AMap.OnMarkerClickListener;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.model.AMapCalcRouteResult;
import com.amap.api.navi.model.AMapLaneInfo;
import com.amap.api.navi.model.AMapModelCross;
import com.amap.api.navi.model.AMapNaviCameraInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviInfo;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviPath;
import com.amap.api.navi.model.AMapNaviRouteNotifyData;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.AMapServiceAreaInfo;
import com.amap.api.navi.model.AimLessModeCongestionInfo;
import com.amap.api.navi.model.AimLessModeStat;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.navi.view.RouteOverLay;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.RouteSearch.DriveRouteQuery;
import com.amap.api.services.route.RouteSearch.OnRouteSearchListener;
import com.amap.api.services.route.WalkRouteResult;
import com.amap.driveroute.navigation.Bean.StrategyBean;
import com.amap.driveroute.navigation.CalculateRouteActivity;
import com.amap.driveroute.navigation.RouteNaviActivity;
import com.amap.driveroute.overlay.DrivingRouteOverlay;
import com.amap.driveroute.util.AMapUtil;
import com.amap.driveroute.util.ToastUtil;
import com.autonavi.tbt.TrafficFacilityInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class DriveRouteActivity extends Activity implements OnMapClickListener,
        OnMarkerClickListener, OnInfoWindowClickListener, OnRouteSearchListener, OnClickListener, AMap.OnMapLoadedListener, AMapNaviListener {
    private AMap aMap;
    private MapView mapView;
    private Context mContext;
    private RouteSearch mRouteSearch;
    private DriveRouteResult mDriveRouteResult;
    private LatLonPoint mStartPoint = new LatLonPoint(22.5017396959, 113.8878061060);//起点，39.942295,116.335891  22.5017396959,113.8878061060
    private LatLonPoint mEndPoint = new LatLonPoint(22.5156061011, 114.0699110860);//终点，39.995576,116.481288  22.5156061011,114.0699110860

    private final int ROUTE_TYPE_DRIVE = 2;

    private RelativeLayout mBottomLayout;
    private TextView mRotueTimeDes, mRouteDetailDes;
    private ImageView mTrafficView;
    private ProgressDialog progDialog = null;// 搜索时进度条
    private List<Mapinfo> list,jlist,lists;
    private ArrayList<Marker> markerArrayList;
    private AMapNavi mAMapNavi;
    private StrategyBean mStrategyBean;


    private List<LatLonPoint> startList = new ArrayList<LatLonPoint>();
    /**
     * 途径点坐标集合
     */
    private List<LatLonPoint> wayList = new ArrayList<LatLonPoint>();
    /**
     * 终点坐标集合［建议就一个终点］
     */
    private List<LatLonPoint> endList = new ArrayList<LatLonPoint>();

    /**
     * 保存当前算好的路线
     */
    private SparseArray<RouteOverLay> routeOverlays = new SparseArray<RouteOverLay>();
    private ArrayList<LatLonPoint> list1;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.route_activity);

        mContext = this.getApplicationContext();
        mapView = (MapView) findViewById(R.id.route_map);
        mapView.onCreate(bundle);// 此方法必须重写

        mTrafficView = (ImageView) findViewById(R.id.map_traffic);
        mTrafficView.setOnClickListener(this);

        initdate();
        init();
    }

    private void initdate() {

        //（模拟数据）得到途经点集合
        list = new ArrayList<>();
        list.add(new Mapinfo("4", 22.5017396959, 113.8878061060, "前海自贸大厦", "广东省深圳市南山区南山街道前海自贸大厦"));
        list.add(new Mapinfo("6", 22.5371144818, 113.8979167572, "前海湾地铁站", "广东省深圳市南山区南头街道前海湾(地铁站)"));
        list.add(new Mapinfo("10", 22.6097694261, 114.0294007641, "深圳北站", "广东省深圳市龙华区民治街道深圳北站"));
        list.add(new Mapinfo("12", 22.5156061011, 114.0699110860, "福田站", "广东省深圳市福田区福田街道福田站"));
        list.add(new Mapinfo("17", 22.5156061011, 114.0699110860, "福田口岸", "广东省深圳市福田区福田街道福田口岸"));

        initdata();
    }

    /**
     * 根据途经点到起点的距离进行排序
     */

    public void initdata() {

        jlist = new ArrayList<>();
        for (int i = 0; i < this.list.size(); i++) {

            Mapinfo mapInfo = new Mapinfo();
            float[] results = new float[1];
            Location.distanceBetween(39.854648, 116.367852, list.get(i).getLat(), list.get(i).getLng(), results);

            mapInfo.setJuli(results[0]);
            mapInfo.setUid(list.get(i).getUid());
            jlist.add(mapInfo);


        }
        double temp;
        String str;
        for (int i = jlist.size() - 1; i >= 0; i--) {
            for (int j = 0; j < i; j++) {
                if (jlist.get(j).getJuli() > jlist.get(j + 1).getJuli()) {
                    temp = jlist.get(j).getJuli();
                    str = jlist.get(j).getUid();
                    jlist.get(j).setJuli(jlist.get(j + 1).getJuli());
                    jlist.get(j).setUid(jlist.get(j + 1).getUid());
                    jlist.get(j + 1).setJuli(temp);
                    jlist.get(j + 1).setUid(str);

                }
            }
        }
        lists = new ArrayList<>();
        for (int i = 0; i < jlist.size(); i++) {
            String id = jlist.get(i).getUid();
            for (int j = 0; j < this.list.size(); j++) {
                if (id.equals(this.list.get(j).getUid())) {
                    lists.add(this.list.get(j));
                }
            }
        }

    }

    /**
     * 初始化AMap对象
     */
    private void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setTrafficEnabled(true);
        }
        registerListener();
        mRouteSearch = new RouteSearch(this);
        mRouteSearch.setRouteSearchListener(this);
        mBottomLayout = (RelativeLayout) findViewById(R.id.bottom_layout);
        mRotueTimeDes = (TextView) findViewById(R.id.firstline);
        mRouteDetailDes = (TextView) findViewById(R.id.secondline);

        //获取AMapNavi实例
        mAMapNavi = AMapNavi.getInstance(getApplicationContext());
        //添加监听回调，用于处理算路成功
        mAMapNavi.addAMapNaviListener(this);

        initNavi();
    }

    /**
     * 导航初始化
     */
    private void initNavi() {
        mStrategyBean = new StrategyBean(false, false, false, false);
        startList.add(mStartPoint);
        endList.add(mEndPoint);

    }
    /**
     * 注册监听
     */
    private void registerListener() {
        aMap.setOnMapLoadedListener(DriveRouteActivity.this);
        aMap.setOnMapClickListener(DriveRouteActivity.this);
        aMap.setOnMarkerClickListener(DriveRouteActivity.this);
        aMap.setOnInfoWindowClickListener(DriveRouteActivity.this);

    }

    @Override
    public void onInfoWindowClick(Marker arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onMarkerClick(Marker arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onMapClick(LatLng arg0) {
        // TODO Auto-generated method stub
    }

    /**
     * 开始搜索路径规划方案
     */
    public void searchRouteResult(int routeType, int mode) {
        if (mStartPoint == null) {
            ToastUtil.show(mContext, "定位中，稍后再试...");
            return;
        }
        if (mEndPoint == null) {
            ToastUtil.show(mContext, "终点未设置");
        }
        showProgressDialog();

        //得到途经点坐标
        list1 = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            list1.add(new LatLonPoint(list.get(i).getLat(), list.get(i).getLng()));

        }
        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(
                mStartPoint, mEndPoint);
        if (routeType == ROUTE_TYPE_DRIVE) {// 驾车路径规划
            DriveRouteQuery query = new DriveRouteQuery(fromAndTo, mode, list1,
                    null, "");// 第一个参数表示路径规划的起点和终点，第二个参数表示驾车模式，第三个参数表示途经点，第四个参数表示避让区域，第五个参数表示避让道路
            mRouteSearch.calculateDriveRouteAsyn(query);// 异步路径规划驾车模式查询
        }
    }

    @Override
    public void onBusRouteSearched(BusRouteResult result, int errorCode) {

    }

    @Override
    public void onDriveRouteSearched(DriveRouteResult result, int errorCode) {
        dissmissProgressDialog();
        aMap.clear();// 清理地图上的所有覆盖物
        if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getPaths() != null) {
                if (result.getPaths().size() > 0) {
                    mDriveRouteResult = result;
                    final DrivePath drivePath = mDriveRouteResult.getPaths()
                            .get(0);
                    aMap.clear();// 清理地图上的所有覆盖物
                    DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(
                            mContext, aMap, drivePath,
                            mDriveRouteResult.getStartPos(),
                            mDriveRouteResult.getTargetPos(), list1);


                    drivingRouteOverlay.setNodeIconVisibility(false);//设置节点marker是否显示
                    drivingRouteOverlay.setThroughPointIconVisibility(false);
                    drivingRouteOverlay.setIsColorfulline(true);//是否用颜色展示交通拥堵情况，默认true
                    drivingRouteOverlay.removeFromMap();
                    drivingRouteOverlay.addToMap();
                    drivingRouteOverlay.zoomToSpan();
                    drawMarkers();
                    mBottomLayout.setVisibility(View.VISIBLE);
                    int dis = (int) drivePath.getDistance();
                    int dur = (int) drivePath.getDuration();
                    String des = AMapUtil.getFriendlyTime(dur) + "(" + AMapUtil.getFriendlyLength(dis) + ")";
                    mRotueTimeDes.setText(des);
                    mRouteDetailDes.setVisibility(View.VISIBLE);
                    int taxiCost = (int) mDriveRouteResult.getTaxiCost();
                    mRouteDetailDes.setText("打车约" + taxiCost + "元");

                    //模拟点击
                    mBottomLayout.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(mContext,
//                                    DriveRouteDetailActivity.class);
                                    CalculateRouteActivity.class);
                            intent.putExtra("drive_path", drivePath);
                            intent.putExtra("drive_result",
                                    mDriveRouteResult);
                            startActivity(intent);

                            startNavi();
                        }
                    });

                } else if (result != null && result.getPaths() == null) {
                    ToastUtil.show(mContext, R.string.no_result);
                }

            } else {
                ToastUtil.show(mContext, R.string.no_result);
            }
        } else {
            ToastUtil.showerror(this.getApplicationContext(), errorCode);
        }

    }

    /**
     * 开始导航
     */
    private void startNavi() {
//        if (routeID != -1){
//            mAMapNavi.selectRouteId(routeID);
            Intent gpsintent = new Intent(getApplicationContext(), RouteNaviActivity.class);
            gpsintent.putExtra("gps", false); // gps 为true为真实导航，为false为模拟导航
            startActivity(gpsintent);
//        }
    }

    @Override
    public void onWalkRouteSearched(WalkRouteResult result, int errorCode) {

    }


    /**
     * 显示进度框
     */
    private void showProgressDialog() {
        if (progDialog == null)
            progDialog = new ProgressDialog(this);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setIndeterminate(false);
        progDialog.setCancelable(true);
        progDialog.setMessage("正在搜索");
        progDialog.show();
    }

    /**
     * 隐藏进度框
     */
    private void dissmissProgressDialog() {
        if (progDialog != null) {
            progDialog.dismiss();
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onRideRouteSearched(RideRouteResult arg0, int arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.map_traffic) {
            if (aMap.isTrafficEnabled()) {
                mTrafficView.setImageResource(R.drawable.map_traffic_white);
                aMap.setTrafficEnabled(false);
            } else {
                mTrafficView.setImageResource(R.drawable.map_traffic_hl_white);
                aMap.setTrafficEnabled(true);
            }
        }
    }

    @Override
    public void onMapLoaded() {
        searchRouteResult(ROUTE_TYPE_DRIVE, RouteSearch.DRIVING_SINGLE_DEFAULT);
    }

    /**
     * 自定义带数字的标注
     */
    private void drawMarkers() {
//        for (int i = 0; i < lists.size(); i++) {
//            if (lists.get(i).getLat() != 0.0 && lists.get(i).getLng() != 0.0) {
//                View view = View.inflate(this, R.layout.marker_view, null);
//                TextView mark_id = (TextView) view.findViewById(R.id.mark_id);
//                mark_id.setText((i + 1) + "");
//                Bitmap bitmap = convertViewToBitmap(view);


//                MarkerOptions options = new MarkerOptions();
//                LatLng markerPosition = new LatLng(lists.get(i).getLat(), lists.get(i).getLng());
//                options.position(markerPosition);
//                Marker marker = aMap.addMarker(options);
        //窗口信息
//                Marker marker = drawMarkerOnMap(new LatLng(lists.get(i).getLat(), lists.get(i).getLng()), bitmap, lists.get(i));
//                Animation markerAnimation = new ScaleAnimation(0, 1, 0, 1); //初始化生长效果动画
//                markerAnimation.setDuration(1000);  //设置动画时间 单位毫秒
//                marker.setAnimation(markerAnimation);
//                marker.startAnimation();
//            }
//        }


        ArrayList<MarkerOptions> markerOptionlst = new ArrayList<>();
        for (int i = 0; i < lists.size(); i++) {
//            View view = View.inflate(this, R.layout.marker_view, null);
//            TextView mark_id = (TextView) view.findViewById(R.id.mark_id);

            final Mapinfo locationMarketBean = lists.get(i);
//            MarkerOptions markerOption = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(convertViewToBitmap(view)));
            MarkerOptions markerOption = new MarkerOptions().icon(BitmapDescriptorFactory
                    .fromView(getBitmapView(locationMarketBean)));
            markerOption.position(new LatLng(locationMarketBean.getLat(),
                    locationMarketBean.getLng()));
//            markerOption.title(locationMarketBean.getDetilId() + "\n" + locationMarketBean.getDetilIdNuber())
//                    .snippet(locationMarketBean.getUid())
//                    .setInfoWindowOffset(0, 150);
            markerOption.setFlat(true);
            markerOption.draggable(false);
            Marker marker = aMap.addMarker(markerOption);
            markerOptionlst.add(markerOption);
//            marker.showInfoWindow();
        }
//            marker.setObject(locationMarketBean);
//            markerOptionlst.add(marker);

        markerArrayList = aMap.addMarkers(markerOptionlst, true);

        aMap.setOnMarkerClickListener(new OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                for (int i = 0; i < markerArrayList.size(); i++) {
                    if (marker.equals(markerArrayList.get(i))) {
                        Toast.makeText(DriveRouteActivity.this, lists.get(i).getDetilId(), Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }
        });


    }

    public View getBitmapView(Mapinfo be) {
        LayoutInflater factory = LayoutInflater.from(this);
        View view = factory.inflate(R.layout.marker_view, null);
        TextView tv_title = (TextView) view.findViewById(R.id.mark_id);
        ImageView iv_loction = (ImageView) view.findViewById(R.id.iv_locticon);
        tv_title.setText(be.getDetilId() + "\n" + be.getDetilIdNuber());
        //开始位置
        if (be.getUid().equals("4")) {
            iv_loction.setImageDrawable(getResources().getDrawable(R.drawable.amap_start));
        } else if (be.getUid().equals("17")) {
            iv_loction.setImageDrawable(getResources().getDrawable(R.drawable.amap_end));
        } else {
            iv_loction.setImageDrawable(getResources().getDrawable(R.drawable.amap_through));
        }
        return view;
    }

    /**
     * 将view转Bitmap
     */
    public static Bitmap convertViewToBitmap(View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        return bitmap;
    }

    //在地图上画mark

    private Marker drawMarkerOnMap(LatLng point, Bitmap markerIcon, Mapinfo mapInfo) {
        if (aMap != null && point != null) {

            aMap.setOnMarkerClickListener(new OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    return false;
                }
            });
            Marker marker = aMap.addMarker(new MarkerOptions().anchor(0.5f, 1)
                    .position(point)
                    .title(mapInfo.getDetilId() + "\n" + mapInfo.getDetilIdNuber())
                    .snippet(mapInfo.getUid())
                    .setInfoWindowOffset(0, 150)
                    .icon(BitmapDescriptorFactory.fromBitmap(markerIcon))
                    .draggable(false));
            marker.showInfoWindow();

            return marker;
        }
        return null;
    }

    @Override
    public void onInitNaviFailure() {

    }

    @Override
    public void onInitNaviSuccess() {
        /**
         * 方法:
         *   int strategy=mAMapNavi.strategyConvert(congestion, avoidhightspeed, cost, hightspeed, multipleroute);
         * 参数:
         * @congestion 躲避拥堵
         * @avoidhightspeed 不走高速
         * @cost 避免收费
         * @hightspeed 高速优先
         * @multipleroute 多路径
         *
         * 说明:
         *      以上参数都是boolean类型，其中multipleroute参数表示是否多条路线，如果为true则此策略会算出多条路线。
         * 注意:
         *      不走高速与高速优先不能同时为true
         *      高速优先与避免收费不能同时为true
         */
        int strategy = 0;
        try {
            strategy = mAMapNavi.strategyConvert(true, false, false, false, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        mAMapNavi.calculateDriveRoute(startList, endList, wayList, strategy);
    }

    @Override
    public void onStartNavi(int i) {

    }

    @Override
    public void onTrafficStatusUpdate() {

    }

    @Override
    public void onLocationChange(AMapNaviLocation aMapNaviLocation) {

    }

    @Override
    public void onGetNavigationText(int i, String s) {

    }

    @Override
    public void onGetNavigationText(String s) {

    }

    @Override
    public void onEndEmulatorNavi() {

    }

    @Override
    public void onArriveDestination() {

    }

    @Override
    public void onCalculateRouteFailure(int i) {

    }

    @Override
    public void onReCalculateRouteForYaw() {

    }

    @Override
    public void onReCalculateRouteForTrafficJam() {

    }

    @Override
    public void onArrivedWayPoint(int i) {

    }

    @Override
    public void onGpsOpenStatus(boolean b) {

    }

    @Override
    public void onNaviInfoUpdate(NaviInfo naviInfo) {

    }

    @Override
    public void onNaviInfoUpdated(AMapNaviInfo aMapNaviInfo) {

    }

    @Override
    public void updateCameraInfo(AMapNaviCameraInfo[] aMapNaviCameraInfos) {

    }

    @Override
    public void updateIntervalCameraInfo(AMapNaviCameraInfo aMapNaviCameraInfo, AMapNaviCameraInfo aMapNaviCameraInfo1, int i) {

    }

    @Override
    public void onServiceAreaUpdate(AMapServiceAreaInfo[] aMapServiceAreaInfos) {

    }

    @Override
    public void showCross(AMapNaviCross aMapNaviCross) {

    }

    @Override
    public void hideCross() {

    }

    @Override
    public void showModeCross(AMapModelCross aMapModelCross) {

    }

    @Override
    public void hideModeCross() {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo[] aMapLaneInfos, byte[] bytes, byte[] bytes1) {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo aMapLaneInfo) {

    }

    @Override
    public void hideLaneInfo() {

    }

    @Override
    public void onCalculateRouteSuccess(int[] ints) {
        cleanRouteOverlay();
        HashMap<Integer, AMapNaviPath> paths = mAMapNavi.getNaviPaths();
        for (int i = 0; i < ints.length; i++) {
            AMapNaviPath path = paths.get(ints[i]);
            if (path != null) {
                drawRoutes(ints[i], path);
            }
        }
//        setRouteLineTag(paths, ints);
        aMap.setMapType(AMap.MAP_TYPE_NAVI);
    }
    /**
     * 绘制路径规划结果
     *
     * @param routeId 路径规划线路ID
     * @param path    AMapNaviPath
     */
    private void drawRoutes(int routeId, AMapNaviPath path) {
        aMap.moveCamera(CameraUpdateFactory.changeTilt(0));
        RouteOverLay routeOverLay = new RouteOverLay(aMap, path, this);
        try {
            routeOverLay.setWidth(60f);
        } catch (Exception e) {
            e.printStackTrace();
        }
        routeOverLay.setTrafficLine(true);
        routeOverLay.addToMap();
        routeOverlays.put(routeId, routeOverLay);
    }

    private void cleanRouteOverlay() {
        for (int i = 0; i < routeOverlays.size(); i++) {
            int key = routeOverlays.keyAt(i);
            RouteOverLay overlay = routeOverlays.get(key);
            overlay.removeFromMap();
            overlay.destroy();
        }
        routeOverlays.clear();
    }
    @Override
    public void notifyParallelRoad(int i) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo aMapNaviTrafficFacilityInfo) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[] aMapNaviTrafficFacilityInfos) {

    }

    @Override
    public void OnUpdateTrafficFacility(TrafficFacilityInfo trafficFacilityInfo) {

    }

    @Override
    public void updateAimlessModeStatistics(AimLessModeStat aimLessModeStat) {

    }

    @Override
    public void updateAimlessModeCongestionInfo(AimLessModeCongestionInfo aimLessModeCongestionInfo) {

    }

    @Override
    public void onPlayRing(int i) {

    }

    @Override
    public void onCalculateRouteSuccess(AMapCalcRouteResult aMapCalcRouteResult) {

    }

    @Override
    public void onCalculateRouteFailure(AMapCalcRouteResult aMapCalcRouteResult) {

    }

    @Override
    public void onNaviRouteNotify(AMapNaviRouteNotifyData aMapNaviRouteNotifyData) {

    }
}

