package com.amap.driveroute.navigation;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.AMapNaviView;
import com.amap.api.navi.AMapNaviViewListener;
import com.amap.api.navi.AMapNaviViewOptions;
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
import com.amap.driveroute.Mapinfo;
import com.amap.driveroute.R;
import com.amap.driveroute.util.ToastUtil;
import com.autonavi.tbt.TrafficFacilityInfo;

import java.util.ArrayList;

/**
 * 按照选定策略导航
 */
public class RouteNaviActivity extends Activity implements AMapNaviListener, AMapNaviViewListener {

    AMapNaviView mAMapNaviView;
    AMapNavi mAMapNavi;
    private AMap aMap;
    private ArrayList<Mapinfo> lists;
    private RouteOverLay routeOverlay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_basic_navi);


        mAMapNaviView = (AMapNaviView) findViewById(R.id.navi_view);
        mAMapNaviView.onCreate(savedInstanceState);
        mAMapNaviView.setAMapNaviViewListener(this);

        mAMapNavi = AMapNavi.getInstance(getApplicationContext());
        mAMapNavi.addAMapNaviListener(this);
        mAMapNavi.setUseInnerVoice(true);

        mAMapNavi.setEmulatorNaviSpeed(60);
        boolean gps = getIntent().getBooleanExtra("gps", false);
        lists = (ArrayList<Mapinfo>) getIntent().getSerializableExtra("list");
        if (gps) {
            mAMapNavi.startNavi(AMapNavi.GPSNaviMode);
        } else {
            mAMapNavi.startNavi(AMapNavi.EmulatorNaviMode);
        }

        AMapNaviViewOptions options = mAMapNaviView.getViewOptions();
        //关闭自动绘制路线（如果你想自行绘制路线的话，必须关闭！！！）
        options.setAutoDrawRoute(false);
        mAMapNaviView.setViewOptions(options);

        aMap = mAMapNaviView.getMap();
        routeOverlay = new RouteOverLay(mAMapNaviView.getMap(), mAMapNavi.getNaviPath(), this);
//    //设置起点的图标
//        routeOverlay.setStartPointBitmap(convertViewToBitmap(lists.get(0)));
//    //设置终点的图标
//        routeOverlay.setEndPointBitmap(convertViewToBitmap(lists.get(lists.size() - 1)));
    //设置途经点的图标
//        routeOverlay.setWayPointBitmap(convertViewToBitmap(lists));
        drawMarkers();
        try {
            routeOverlay.setWidth(30);
        } catch (Exception e) {
    //宽度须>0
            e.printStackTrace();
        }
        int color[] = new int[10];
        color[0] = Color.BLACK;
        color[1] = Color.RED;
        color[2] = Color.BLUE;
        color[3] = Color.YELLOW;
        color[4] = Color.GRAY;
//以途径点的index分隔，用不同的颜色绘制路段
        routeOverlay.addToMap(color, mAMapNavi.getNaviPath().getWayPointIndex());
    }

    private ArrayList<Marker> markerArrayList;

    private void drawMarkers() {
        ArrayList<MarkerOptions> markerOptionlst = new ArrayList<>();
        for (int i = 0; i < lists.size(); i++) {

            final Mapinfo locationMarketBean = lists.get(i);
            MarkerOptions markerOption = new MarkerOptions().icon(BitmapDescriptorFactory
                    .fromView(getBitmapView(locationMarketBean)));
            markerOption.position(new LatLng(locationMarketBean.getLat(),
                    locationMarketBean.getLng()));
            markerOption.setFlat(true);
            markerOption.draggable(false);
            Marker marker = aMap.addMarker(markerOption);
            markerOptionlst.add(markerOption);
        }
        markerArrayList = aMap.addMarkers(markerOptionlst, true);
        aMap.setOnMarkerClickListener(new AMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                for (int i = 0; i < markerArrayList.size(); i++) {
                    if (marker.equals(markerArrayList.get(i))) {
                        Toast.makeText(RouteNaviActivity.this, lists.get(i).getDetilId(), Toast.LENGTH_SHORT).show();
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
    public Bitmap convertViewToBitmap(Mapinfo bean) {

            View view = View.inflate(this, R.layout.marker_view, null);
            TextView tv_title = (TextView) view.findViewById(R.id.mark_id);
            ImageView iv_loction = (ImageView) view.findViewById(R.id.iv_locticon);
            tv_title.setText(bean.getDetilId() + "\n" + bean.getDetilIdNuber());
            if (bean.getUid().equals("4")){
                iv_loction.setImageDrawable(getResources().getDrawable(R.drawable.amap_start));
            }else if (bean.getUid().equals("17")){
                iv_loction.setImageDrawable(getResources().getDrawable(R.drawable.amap_end));
            }else {
                iv_loction.setImageDrawable(getResources().getDrawable(R.drawable.amap_through));
            }

            view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            view.buildDrawingCache();
            Bitmap bitmap = view.getDrawingCache();
            return bitmap;
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
        //去掉红绿灯图标显示
        routeOverLay.setLightsVisible(false);
        routeOverLay.setTrafficLightsVisible(false);
//        routeOverLay.setStartPointBitmap(convertViewToBitmap(list.get(0)));
//        routeOverLay.setEndPointBitmap(convertViewToBitmap(list.get(list.size() - 1)));
        routeOverLay.setTrafficLine(true);
        routeOverLay.setRouteOverlayVisible(false);
        routeOverLay.addToMap();
        //修改自定义marker
        drawMarkers();
//        routeOverlays.put(routeId, routeOverLay);


    }

    @Override
    protected void onResume() {
        super.onResume();
        mAMapNaviView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAMapNaviView.onPause();

        //
        //        停止导航之后，会触及底层stop，然后就不会再有回调了，但是讯飞当前还是没有说完的半句话还是会说完
        //        mAMapNavi.stopNavi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAMapNaviView.onDestroy();
        mAMapNavi.stopNavi();
//		mAMapNavi.destroy();

    }

    @Override
    public void onInitNaviFailure() {
        Toast.makeText(this, "init navi Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onInitNaviSuccess() {
    }

    @Override
    public void onStartNavi(int type) {

    }

    @Override
    public void onTrafficStatusUpdate() {

    }

    @Override
    public void onLocationChange(AMapNaviLocation location) {

    }

    @Override
    public void onGetNavigationText(int type, String text) {

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
    public void onCalculateRouteFailure(int errorInfo) {
    }

    @Override
    public void onReCalculateRouteForYaw() {

    }

    @Override
    public void onReCalculateRouteForTrafficJam() {

    }

    /**
     * 途经点返回
     *
     * @param wayID 默认0 开始
     */
    @Override
    public void onArrivedWayPoint(int wayID) {
        //到站停止
        mAMapNaviView.onPause();
        mAMapNavi.stopNavi();
        ToastUtil.show(this, "wayID==" + wayID);
        //语音播报 做相关操作
    }

    @Override
    public void onGpsOpenStatus(boolean enabled) {
    }

    @Override
    public void onNaviSetting() {
    }

    @Override
    public void onNaviMapMode(int isLock) {

    }

    @Override
    public void onNaviCancel() {
        finish();
    }

    @Override
    public void onNaviTurnClick() {

    }

    @Override
    public void onNextRoadClick() {

    }

    @Override
    public void onScanViewButtonClick() {
    }

    @Deprecated
    @Override
    public void onNaviInfoUpdated(AMapNaviInfo naviInfo) {
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
    public void onNaviInfoUpdate(NaviInfo naviinfo) {
    }

    @Override
    public void OnUpdateTrafficFacility(TrafficFacilityInfo trafficFacilityInfo) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo aMapNaviTrafficFacilityInfo) {

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
    public void showLaneInfo(AMapLaneInfo[] laneInfos, byte[] laneBackgroundInfo, byte[] laneRecommendedInfo) {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo aMapLaneInfo) {

    }

    @Override
    public void hideLaneInfo() {

    }

    @Override
    public void onCalculateRouteSuccess(int[] ints) {

    }

    @Override
    public void notifyParallelRoad(int i) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[] aMapNaviTrafficFacilityInfos) {

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

    @Override
    public void onLockMap(boolean isLock) {
    }

    @Override
    public void onNaviViewLoaded() {
    }

    @Override
    public void onMapTypeChanged(int i) {

    }

    @Override
    public void onNaviViewShowMode(int i) {

    }

    @Override
    public boolean onNaviBackClick() {
        return false;
    }

}
