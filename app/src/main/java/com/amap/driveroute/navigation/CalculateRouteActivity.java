package com.amap.driveroute.navigation;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
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
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.navi.view.RouteOverLay;
import com.amap.driveroute.Mapinfo;
import com.amap.driveroute.R;
import com.amap.driveroute.navigation.Bean.StrategyBean;
import com.amap.driveroute.navigation.util.Utils;
import com.autonavi.tbt.TrafficFacilityInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 驾车路径规划并展示对应的路线标签
 */
public class CalculateRouteActivity extends Activity implements AMapNaviListener, View.OnClickListener, AMap.OnMapLoadedListener {
    private StrategyBean mStrategyBean;
    private static final float ROUTE_UNSELECTED_TRANSPARENCY = 0.3F;
    private static final float ROUTE_SELECTED_TRANSPARENCY = 1F;

    /**
     * 导航对象(单例)
     */
    private AMapNavi mAMapNavi;

    private MapView mMapView;
    private AMap mAMap;
    private NaviLatLng endLatlng = new NaviLatLng(22.5156061011, 114.0699110860);
    private NaviLatLng startLatlng = new NaviLatLng(22.5017396959, 113.8878061060);
    private List<NaviLatLng> startList = new ArrayList<NaviLatLng>();

    private List<Mapinfo> list,jlist,lists;
    /**
     * 途径点坐标集合
     */
    private List<NaviLatLng> wayList = new ArrayList<NaviLatLng>();
    /**
     * 终点坐标集合［建议就一个终点］
     */
    private List<NaviLatLng> endList = new ArrayList<NaviLatLng>();
    /**
     * 保存当前算好的路线
     */
    private SparseArray<RouteOverLay> routeOverlays = new SparseArray<RouteOverLay>();
    /*
            * strategyFlag转换出来的值都对应PathPlanningStrategy常量，用户也可以直接传入PathPlanningStrategy常量进行算路。
            * 如:mAMapNavi.calculateDriveRoute(mStartList, mEndList, mWayPointList,PathPlanningStrategy.DRIVING_DEFAULT);
            */
    int strategyFlag = 0;

    private Button mStartNaviButton;
    private LinearLayout mRouteLineLayoutOne, mRouteLinelayoutTwo, mRouteLineLayoutThree;
    private View mRouteViewOne, mRouteViewTwo, mRouteViewThree;
    private TextView mRouteTextStrategyOne, mRouteTextStrategyTwo, mRouteTextStrategyThree;
    private TextView mRouteTextTimeOne, mRouteTextTimeTwo, mRouteTextTimeThree;
    private TextView mRouteTextDistanceOne, mRouteTextDistanceTwo, mRouteTextDistanceThree;
    private TextView mCalculateRouteOverView;
    private ImageView mImageTraffic, mImageStrategy;

    private int routeID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculate_route);
        mMapView = (MapView) findViewById(R.id.navi_view);
        mMapView.onCreate(savedInstanceState);// 此方法必须重写
        initView();
        init();
        initNavi();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.calculate_route_start_navi:
                startNavi();
                break;
            case R.id.route_line_one:
                focuseRouteLine(true, false, false);
                break;
            case R.id.route_line_two:
                focuseRouteLine(false, true, false);
                break;
            case R.id.route_line_three:
                focuseRouteLine(false, false, true);
                break;
            case R.id.map_traffic:
                setTraffic();
                break;
            case R.id.strategy_choose:
                strategyChoose();
                break;
            default:
                break;
        }

    }

    /**
     * 驾车路径规划计算
     */
    private void calculateDriveRoute() {
        try {
            strategyFlag = mAMapNavi.strategyConvert(mStrategyBean.isCongestion(), mStrategyBean.isCost(), mStrategyBean.isAvoidhightspeed(), mStrategyBean.isHightspeed(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mAMapNavi.calculateDriveRoute(startList, endList, wayList, strategyFlag);
    }

    /**
     * 接收驾车偏好设置项
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Utils.ACTIVITY_RESULT_CODE == resultCode) {
            boolean congestion = data.getBooleanExtra(Utils.INTENT_NAME_AVOID_CONGESTION, false);
            mStrategyBean.setCongestion(congestion);
            boolean cost = data.getBooleanExtra(Utils.INTENT_NAME_AVOID_COST, false);
            mStrategyBean.setCost(cost);
            boolean avoidhightspeed = data.getBooleanExtra(Utils.INTENT_NAME_AVOID_HIGHSPEED, false);
            mStrategyBean.setAvoidhightspeed(avoidhightspeed);
            boolean hightspeed = data.getBooleanExtra(Utils.INTENT_NAME_PRIORITY_HIGHSPEED, false);
            mStrategyBean.setHightspeed(hightspeed);
            calculateDriveRoute();
        }
    }

    /**
     * 导航初始化
     */
    private void initNavi() {
        mStrategyBean = new StrategyBean(false, false, false, false);
        startList.add(startLatlng);
        endList.add(endLatlng);
        mAMapNavi = AMapNavi.getInstance(getApplicationContext());
        mAMapNavi.addAMapNaviListener(this);

    }

    private void initView() {
        mStartNaviButton = (Button) findViewById(R.id.calculate_route_start_navi);
        mStartNaviButton.setOnClickListener(this);

        mImageTraffic = (ImageView) findViewById(R.id.map_traffic);
        mImageTraffic.setOnClickListener(this);
        mImageStrategy = (ImageView) findViewById(R.id.strategy_choose);
        mImageStrategy.setOnClickListener(this);

        mCalculateRouteOverView = (TextView) findViewById(R.id.calculate_route_navi_overview);

        mRouteLineLayoutOne = (LinearLayout) findViewById(R.id.route_line_one);
        mRouteLineLayoutOne.setOnClickListener(this);
        mRouteLinelayoutTwo = (LinearLayout) findViewById(R.id.route_line_two);
        mRouteLinelayoutTwo.setOnClickListener(this);
        mRouteLineLayoutThree = (LinearLayout) findViewById(R.id.route_line_three);
        mRouteLineLayoutThree.setOnClickListener(this);

        mRouteViewOne = (View) findViewById(R.id.route_line_one_view);
        mRouteViewTwo = (View) findViewById(R.id.route_line_two_view);
        mRouteViewThree = (View) findViewById(R.id.route_line_three_view);

        mRouteTextStrategyOne = (TextView) findViewById(R.id.route_line_one_strategy);
        mRouteTextStrategyTwo = (TextView) findViewById(R.id.route_line_two_strategy);
        mRouteTextStrategyThree = (TextView) findViewById(R.id.route_line_three_strategy);

        mRouteTextTimeOne = (TextView) findViewById(R.id.route_line_one_time);
        mRouteTextTimeTwo = (TextView) findViewById(R.id.route_line_two_time);
        mRouteTextTimeThree = (TextView) findViewById(R.id.route_line_three_time);

        mRouteTextDistanceOne = (TextView) findViewById(R.id.route_line_one_distance);
        mRouteTextDistanceTwo = (TextView) findViewById(R.id.route_line_two_distance);
        mRouteTextDistanceThree = (TextView) findViewById(R.id.route_line_three_distance);
    }


    /**
     * 初始化AMap对象
     */
    private void init() {
        if (mAMap == null) {
            mAMap = mMapView.getMap();
            mAMap.setTrafficEnabled(false);
            mAMap.setOnMapLoadedListener(this);
            mImageTraffic.setImageResource(R.drawable.map_traffic_white);
            UiSettings uiSettings = mAMap.getUiSettings();
            //
            uiSettings.setZoomControlsEnabled(true);
        }
//        wayList.add(new NaviLatLng(22.5017396959, 113.8878061060));
        wayList.add(new NaviLatLng(22.5371144818, 113.8979167572));
        wayList.add(new NaviLatLng(22.6097694261, 114.0294007641));
        wayList.add(new NaviLatLng(22.5156061011, 114.0699110860));
//        wayList.add(new NaviLatLng(22.5156061011, 114.0699110860));

        //（模拟数据）得到途经点集合
        list = new ArrayList<>();
//        list.add(new Mapinfo("4", 22.5017396959, 113.8878061060, "前海自贸大厦", "广东省深圳市南山区南山街道前海自贸大厦"));
        list.add(new Mapinfo("6", 22.5371144818, 113.8979167572, "前海湾地铁站", "广东省深圳市南山区南头街道前海湾(地铁站)"));
        list.add(new Mapinfo("10", 22.6097694261, 114.0294007641, "深圳北站", "广东省深圳市龙华区民治街道深圳北站"));
        list.add(new Mapinfo("12", 22.5156061011, 114.0699110860, "福田站", "广东省深圳市福田区福田街道福田站"));
//        list.add(new Mapinfo("17", 22.5156061011, 114.0699110860, "福田口岸", "广东省深圳市福田区福田街道福田口岸"));

        initdata();
    }

    /**
     * 绘制路径规划结果
     *
     * @param routeId 路径规划线路ID
     * @param path    AMapNaviPath
     */
    private void drawRoutes(int routeId, AMapNaviPath path) {
        mAMap.moveCamera(CameraUpdateFactory.changeTilt(0));
        RouteOverLay routeOverLay = new RouteOverLay(mAMap, path, this);
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
        routeOverlays.put(routeId, routeOverLay);


    }


    /**
     * 开始导航
     */
    private void startNavi() {
        if (routeID != -1){
            mAMapNavi.selectRouteId(routeID);
            Intent gpsintent = new Intent(getApplicationContext(), RouteNaviActivity.class);
            gpsintent.putExtra("gps", false); // gps 为true为真实导航，为false为模拟导航
            gpsintent.putExtra("list", (Serializable) lists); // 标记点集合
            startActivity(gpsintent);
        }
    }

    /**
     * 路线tag选中设置
     *
     * @param lineOne
     * @param lineTwo
     * @param lineThree
     */
    private void focuseRouteLine(boolean lineOne, boolean lineTwo, boolean lineThree) {
        Log.d("LG", "lineOne:" + lineOne + " lineTwo:" + lineTwo + " lineThree:" + lineThree);
        setLinelayoutOne(lineOne);
        setLinelayoutTwo(lineTwo);
        setLinelayoutThree(lineThree);
    }

    /**
     * 地图实时交通开关
     */
    private void setTraffic() {
        if (mAMap.isTrafficEnabled()) {
            mImageTraffic.setImageResource(R.drawable.map_traffic_white);
            mAMap.setTrafficEnabled(false);
        } else {
            mImageTraffic.setImageResource(R.drawable.map_traffic_hl_white);
            mAMap.setTrafficEnabled(true);
        }
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

    /**
     * 跳转到驾车偏好设置页面
     */
    private void strategyChoose() {
        Intent intent = new Intent(this, StrategyChooseActivity.class);
        intent.putExtra(Utils.INTENT_NAME_AVOID_CONGESTION, mStrategyBean.isCongestion());
        intent.putExtra(Utils.INTENT_NAME_AVOID_COST, mStrategyBean.isCost());
        intent.putExtra(Utils.INTENT_NAME_AVOID_HIGHSPEED, mStrategyBean.isAvoidhightspeed());
        intent.putExtra(Utils.INTENT_NAME_PRIORITY_HIGHSPEED, mStrategyBean.isHightspeed());
        startActivityForResult(intent, Utils.START_ACTIVITY_REQUEST_CODE);
    }

    /**
     * @param paths 多路线回调路线
     * @param ints  多路线回调路线ID
     */
    private void setRouteLineTag(HashMap<Integer, AMapNaviPath> paths, int[] ints) {
        if (ints.length < 1) {
            visiableRouteLine(false, false, false);
            return;
        }
        int indexOne = 0;
        String stragegyTagOne = paths.get(ints[indexOne]).getLabels();
        setLinelayoutOneContent(ints[indexOne], stragegyTagOne);
        if (ints.length == 1) {
            visiableRouteLine(true, false, false);
            focuseRouteLine(true, false, false);
            return;
        }

        int indexTwo = 1;
        String stragegyTagTwo = paths.get(ints[indexTwo]).getLabels();
        setLinelayoutTwoContent(ints[indexTwo], stragegyTagTwo);
        if (ints.length == 2) {
            visiableRouteLine(true, true, false);
            focuseRouteLine(true, false, false);
            return;
        }

        int indexThree = 2;
        String stragegyTagThree = paths.get(ints[indexThree]).getLabels();
        setLinelayoutThreeContent(ints[indexThree], stragegyTagThree);
        if (ints.length >= 3) {
            visiableRouteLine(true, true, true);
            focuseRouteLine(true, false, false);
        }

    }

    private void visiableRouteLine(boolean lineOne, boolean lineTwo, boolean lineThree) {
        setLinelayoutOneVisiable(lineOne);
        setLinelayoutTwoVisiable(lineTwo);
        setLinelayoutThreeVisiable(lineThree);
    }

    private void setLinelayoutOneVisiable(boolean visiable) {
        if (visiable) {
            mRouteLineLayoutOne.setVisibility(View.VISIBLE);
        } else {
            mRouteLineLayoutOne.setVisibility(View.GONE);
        }
    }

    private void setLinelayoutTwoVisiable(boolean visiable) {
        if (visiable) {
            mRouteLinelayoutTwo.setVisibility(View.VISIBLE);
        } else {
            mRouteLinelayoutTwo.setVisibility(View.GONE);
        }
    }

    private void setLinelayoutThreeVisiable(boolean visiable) {
        if (visiable) {
            mRouteLineLayoutThree.setVisibility(View.VISIBLE);
        } else {
            mRouteLineLayoutThree.setVisibility(View.GONE);
        }
    }

    /**
     * 设置第一条线路Tab 内容
     *
     * @param routeID  路线ID
     * @param strategy 策略标签
     */
    private void setLinelayoutOneContent(int routeID, String strategy) {
        mRouteLineLayoutOne.setTag(routeID);
        RouteOverLay overlay = routeOverlays.get(routeID);
        overlay.zoomToSpan();
        AMapNaviPath path = overlay.getAMapNaviPath();
        mRouteTextStrategyOne.setText(strategy);
        String timeDes = Utils.getFriendlyTime(path.getAllTime());
        mRouteTextTimeOne.setText(timeDes);
        String disDes = Utils.getFriendlyDistance(path.getAllLength());
        mRouteTextDistanceOne.setText(disDes);
    }

    /**
     * 设置第二条路线Tab 内容
     *
     * @param routeID  路线ID
     * @param strategy 策略标签
     */
    private void setLinelayoutTwoContent(int routeID, String strategy) {
        mRouteLinelayoutTwo.setTag(routeID);
        RouteOverLay overlay = routeOverlays.get(routeID);
        AMapNaviPath path = overlay.getAMapNaviPath();
        mRouteTextStrategyTwo.setText(strategy);
        String timeDes = Utils.getFriendlyTime(path.getAllTime());
        mRouteTextTimeTwo.setText(timeDes);
        String disDes = Utils.getFriendlyDistance(path.getAllLength());
        mRouteTextDistanceTwo.setText(disDes);
    }

    /**
     * 设置第三条路线Tab 内容
     *
     * @param routeID  路线ID
     * @param strategy 策略标签
     */
    private void setLinelayoutThreeContent(int routeID, String strategy) {
        mRouteLineLayoutThree.setTag(routeID);
        RouteOverLay overlay = routeOverlays.get(routeID);
        AMapNaviPath path = overlay.getAMapNaviPath();
        mRouteTextStrategyThree.setText(strategy);
        String timeDes = Utils.getFriendlyTime(path.getAllTime());
        mRouteTextTimeThree.setText(timeDes);
        String disDes = Utils.getFriendlyDistance(path.getAllLength());
        mRouteTextDistanceThree.setText(disDes);
    }

    /**
     * 第一条路线是否focus
     *
     * @param focus focus为true 突出颜色显示，标示为选中状态，为false则标示非选中状态
     */
    private void setLinelayoutOne(boolean focus) {
        if (mRouteLineLayoutOne.getVisibility() != View.VISIBLE) {
            return;
        }
        try {
            RouteOverLay overlay = routeOverlays.get((int)mRouteLineLayoutOne.getTag());
            if (focus) {
                routeID = (int) mRouteLineLayoutOne.getTag();
                mCalculateRouteOverView.setText(Utils.getRouteOverView(overlay.getAMapNaviPath()));
                mAMapNavi.selectRouteId(routeID);
                overlay.setTransparency(ROUTE_SELECTED_TRANSPARENCY);
                mRouteViewOne.setVisibility(View.VISIBLE);
                mRouteTextStrategyOne.setTextColor(getResources().getColor(R.color.colorBlue));
                mRouteTextTimeOne.setTextColor(getResources().getColor(R.color.colorBlue));
                mRouteTextDistanceOne.setTextColor(getResources().getColor(R.color.colorBlue));
            } else {
                overlay.setTransparency(ROUTE_UNSELECTED_TRANSPARENCY);
                mRouteViewOne.setVisibility(View.INVISIBLE);
                mRouteTextStrategyOne.setTextColor(getResources().getColor(R.color.colorDark));
                mRouteTextTimeOne.setTextColor(getResources().getColor(R.color.colorBlack));
                mRouteTextDistanceOne.setTextColor(getResources().getColor(R.color.colorDark));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 第二条路线是否focus
     *
     * @param focus focus为true 突出颜色显示，标示为选中状态，为false则标示非选中状态
     */
    private void setLinelayoutTwo(boolean focus) {
        if (mRouteLinelayoutTwo.getVisibility() != View.VISIBLE) {
            return;
        }
        try {
            RouteOverLay overlay = routeOverlays.get((int) mRouteLinelayoutTwo.getTag());
            if (focus) {
                routeID = (int) mRouteLinelayoutTwo.getTag();
                mCalculateRouteOverView.setText(Utils.getRouteOverView(overlay.getAMapNaviPath()));
                mAMapNavi.selectRouteId(routeID);
                overlay.setTransparency(ROUTE_SELECTED_TRANSPARENCY);
                mRouteViewTwo.setVisibility(View.VISIBLE);
                mRouteTextStrategyTwo.setTextColor(getResources().getColor(R.color.colorBlue));
                mRouteTextTimeTwo.setTextColor(getResources().getColor(R.color.colorBlue));
                mRouteTextDistanceTwo.setTextColor(getResources().getColor(R.color.colorBlue));
            } else {
                overlay.setTransparency(ROUTE_UNSELECTED_TRANSPARENCY);
                mRouteViewTwo.setVisibility(View.INVISIBLE);
                mRouteTextStrategyTwo.setTextColor(getResources().getColor(R.color.colorDark));
                mRouteTextTimeTwo.setTextColor(getResources().getColor(R.color.colorBlack));
                mRouteTextDistanceTwo.setTextColor(getResources().getColor(R.color.colorDark));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 第三条路线是否focus
     *
     * @param focus focus为true 突出颜色显示，标示为选中状态，为false则标示非选中状态
     */
    private void setLinelayoutThree(boolean focus) {
        if (mRouteLineLayoutThree.getVisibility() != View.VISIBLE) {
            return;
        }
        try {
            RouteOverLay overlay = routeOverlays.get((int) mRouteLineLayoutThree.getTag());
            if (overlay == null) {
                return;
            }
            if (focus) {
                routeID = (int) mRouteLineLayoutThree.getTag();
                mCalculateRouteOverView.setText(Utils.getRouteOverView(overlay.getAMapNaviPath()));
                mAMapNavi.selectRouteId(routeID);
                overlay.setTransparency(ROUTE_SELECTED_TRANSPARENCY);
                mRouteViewThree.setVisibility(View.VISIBLE);
                mRouteTextStrategyThree.setTextColor(getResources().getColor(R.color.colorBlue));
                mRouteTextTimeThree.setTextColor(getResources().getColor(R.color.colorBlue));
                mRouteTextDistanceThree.setTextColor(getResources().getColor(R.color.colorBlue));
            } else {
                overlay.setTransparency(ROUTE_UNSELECTED_TRANSPARENCY);
                mRouteViewThree.setVisibility(View.INVISIBLE);
                mRouteTextStrategyThree.setTextColor(getResources().getColor(R.color.colorDark));
                mRouteTextTimeThree.setTextColor(getResources().getColor(R.color.colorBlack));
                mRouteTextDistanceThree.setTextColor(getResources().getColor(R.color.colorDark));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        if (mAMapNavi != null) {
            mAMapNavi.destroy();
        }
    }

    @Override
    public void onInitNaviFailure() {

    }

    @Override
    public void onInitNaviSuccess() {

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
        Toast.makeText(this.getApplicationContext(),"错误码"+i, Toast.LENGTH_LONG).show();
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
    public void onNaviInfoUpdate(NaviInfo naviInfo) {

    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo aMapNaviTrafficFacilityInfo) {

    }

    @Override
    public void OnUpdateTrafficFacility(TrafficFacilityInfo trafficFacilityInfo) {

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
        setRouteLineTag(paths, ints);
        mAMap.setMapType(AMap.MAP_TYPE_NAVI);
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
    public void onMapLoaded() {
        calculateDriveRoute();
    }


    /**
     * 将view转Bitmap
     */
    public Bitmap convertViewToBitmap(Mapinfo bean) {

        View view = View.inflate(this,R.layout.marker_view, null);
        TextView tv_title = (TextView) view.findViewById(R.id.mark_id);
        ImageView iv_loction = (ImageView) view.findViewById(R.id.iv_locticon);
        tv_title.setText(bean.getDetilId() + "\n" + bean.getDetilIdNuber());
        if (bean.getUid().equals("4")){
            iv_loction.setImageDrawable(getResources().getDrawable(R.drawable.amap_end));
        }else {
            iv_loction.setImageDrawable(getResources().getDrawable(R.drawable.amap_end));
        }

        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        return bitmap;
    }
    /**
     * 自定义带数字的标注
     */


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
            Marker marker = mAMap.addMarker(markerOption);
            markerOptionlst.add(markerOption);
        }
        markerArrayList = mAMap.addMarkers(markerOptionlst, true);
        mAMap.setOnMarkerClickListener(new AMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                for (int i = 0; i < markerArrayList.size(); i++) {
                    if (marker.equals(markerArrayList.get(i))) {
                        Toast.makeText(CalculateRouteActivity.this, lists.get(i).getDetilId(), Toast.LENGTH_SHORT).show();
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
}
