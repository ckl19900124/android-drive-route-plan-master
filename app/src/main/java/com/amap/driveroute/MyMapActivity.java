package com.amap.driveroute;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkRouteResult;
import com.amap.driveroute.overlay.DrivingRouteOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2019-6-10.
 */

public class MyMapActivity extends Activity implements AMap.OnInfoWindowClickListener ,RouteSearch.OnRouteSearchListener ,AMap.OnMarkerClickListener{
    private MapView map;
    private AMap aMap;
    private UiSettings uiSettings;
    private CameraUpdate mUpdate;
    private ArrayList<Mapinfo> list,lists,jlist;  //途经点集合
    private ArrayList<LatLonPoint> list1;
    private DriveRouteResult driveRouteResult;// 驾车模式查询结果
    private int drivingMode = RouteSearch.DrivingDefault;// 驾车默认模式
    private RouteSearch routeSearch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_map);
        map = (MapView)findViewById(R.id.route_map);

        map.onCreate(savedInstanceState);

        //（模拟数据）得到途经点集合
        list=new ArrayList<>();
        list.add(new Mapinfo("4", 39.8530620000, 116.3747240000, "鸿运水果烟酒超市", "详细地址详细地址5555"));
        list.add(new Mapinfo("6", 39.8516080000, 116.3734790000, "世纪华联超市", "详细地址详细地址7777"));
        list.add(new Mapinfo("10",39.844693, 116.369901, "鲜花婚庆水果", "详细地址详细地址1010"));
        list.add(new Mapinfo("12",39.84237, 116.381086, "街角蛋糕", "详细地址详细地址66666"));
        list.add(new Mapinfo("16",39.854152, 116.415689, "北京同仁堂第二中医院", "详细地址详细地址222"));

        list.add(new Mapinfo("17",39.860975, 116.412025, "北京市益华食品厂", "详细地址详细地址3333"));

        initdata();//根据途经点到起点的距离进行排序
        init();  //初始化地图


        //得到途经点坐标
        list1=new ArrayList<>();
        for (int i=0;i<list.size();i++){
            list1.add(new LatLonPoint(list.get(i).getLat(),list.get(i).getLng()));

        }

        //InfoWindow  和 Marker 监听 以及线路规划监听
        aMap.setOnInfoWindowClickListener(this);
        aMap.setOnMarkerClickListener(this);

        routeSearch = new RouteSearch(this);

        routeSearch.setRouteSearchListener(this);

        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(new LatLonPoint(39.854648,116.367852), new LatLonPoint(39.833673,116.387424)); //设置起点和终点


        RouteSearch.DriveRouteQuery query = new RouteSearch.DriveRouteQuery(fromAndTo, drivingMode, list1, null, "");// 第一个参数表示路径规划的起点和终点，第二个参数表示驾车模式，第三个参数表示途经点，第四个参数表示避让区域，第五个参数表示避让道路
        routeSearch.calculateDriveRouteAsyn(query);// 异步路径规划驾车模式查询


    }

    /**

     * 根据途经点到起点的距离进行排序

     */

    public  void  initdata(){

        jlist=new ArrayList<>();
        for (int i=0;i<list.size();i++){

            Mapinfo mapInfo=new Mapinfo();
            float[] results=new float[1];
            Location.distanceBetween(39.854648, 116.367852, list.get(i).getLat(), list.get(i).getLng(), results);

            mapInfo.setJuli(results[0]);
            mapInfo.setUid(list.get(i).getUid());
            jlist.add(mapInfo);


        }
        double temp;
        String str;
        for(int i = jlist.size() - 1; i >= 0; i --) {
            for (int j = 0; j < i; j++) {
                if (jlist.get(j).getJuli() > jlist.get(j+1).getJuli()) {
                    temp = jlist.get(j).getJuli();
                    str=jlist.get(j).getUid();
                    jlist.get(j).setJuli(jlist.get(j + 1).getJuli());
                    jlist.get(j).setUid(jlist.get(j + 1).getUid());
                    jlist.get(j+1).setJuli(temp);
                    jlist.get(j+1).setUid(str);

                }
            }
        }
        lists=new ArrayList<>();
        for (int i=0;i<jlist.size();i++){
            String id=jlist.get(i).getUid();
            for (int j=0;j<list.size();j++){
                if(id.equals(list.get(j).getUid())){
                    lists.add(list.get(j));
                }
            }
        }

    }

    /**

     *将view转Bitmap

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
            Marker marker = aMap.addMarker(new MarkerOptions().anchor(0.5f, 1)
                    .position(point)
                    .title(mapInfo.getUid() + "\n" + mapInfo.getDetilId())
                    .snippet(mapInfo.getDetilIdNuber())
                    .icon(BitmapDescriptorFactory.fromBitmap(markerIcon)));
            marker.showInfoWindow();

            return marker;
        }
        return null;

    }
        /**
         * 初始化map对象
         */


    private void init() {


        if (aMap == null) {

            aMap = map.getMap();
            uiSettings = aMap.getUiSettings();
            uiSettings.setZoomControlsEnabled(false);
            // 这是地理位置（经纬度）
            for (int i = 0; i < lists.size(); i++) {
                mUpdate = CameraUpdateFactory.newCameraPosition(new CameraPosition(
                        new LatLng(lists.get(i).getLat(), lists.get(i).getLng()), 16, 0, 30));
                // 定位的方法
                aMap.moveCamera(mUpdate);
            }
            drawMarkers();

        }

    }
        /**
         * 自定义带数字的标注
         */


    private void drawMarkers() {




        for (int i = 0; i < lists.size(); i++) {
            if (lists.get(i).getLat()!=0.0&&lists.get(i).getLng()!=0.0) {
                View view = View.inflate(this,R.layout.marker_view, null);
                TextView mark_id = (TextView) view.findViewById(R.id.mark_id);
                mark_id.setText((i + 1) + "");
                Bitmap bitmap = convertViewToBitmap(view);
                //窗口信息
                drawMarkerOnMap(new LatLng(lists.get(i).getLat(), lists.get(i).getLng()), bitmap,  lists.get(i));
            }
        }
    }




    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        map.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        map.onDestroy();

    }

//窗口监听回调


    @Override
    public void onInfoWindowClick(Marker marker) {
        Toast.makeText(this,marker.getTitle()+"\n"+marker.getSnippet(),Toast.LENGTH_SHORT).show();



    }

    //公交回调
    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

    }
//驾车回调
    @Override
    public void onDriveRouteSearched(DriveRouteResult result, int rCode) {
        if (rCode == 0) {
            if (result != null && result.getPaths() != null
                    && result.getPaths().size() > 0) {
                driveRouteResult = result;
                DrivePath drivePath = driveRouteResult.getPaths().get(0);

                RouteTool routeTool=  new RouteTool(this,aMap,drivePath,driveRouteResult.getStartPos(), driveRouteResult.getTargetPos(),list1);
                routeTool.setView(Color.BLUE,6);
                routeTool.removeFromMap();
                routeTool.addToMap();
                routeTool.zoomToSpan();


            } else {

                Toast.makeText(this,"对不起，没有搜索到相关数据！",Toast.LENGTH_SHORT).show();
            }
        } else if (rCode == 27) {

            Toast.makeText(this,"搜索失败,请检查网络连接！",Toast.LENGTH_SHORT).show();
        } else if (rCode == 32) {

            Toast.makeText(this,"key验证无效！",Toast.LENGTH_SHORT).show();
        } else {

            Toast.makeText(this,"未知错误，请稍后重试!错误码为"+rCode,Toast.LENGTH_SHORT).show();
        }
    }


//步行回调
    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

    }

    @Override
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

    }


//标注监听回调


    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        return false;
    }



    /**
     * 自定义路线的颜色、宽度、小汽车标志
     */

    class RouteTool extends DrivingRouteOverlay {
        int color;//路线颜色
        float lineWidth;//路线宽度

        public RouteTool(Context context, AMap aMap, DrivePath drivePath, LatLonPoint latLonPoint, LatLonPoint latLonPoint1,List<LatLonPoint> throughPointList) {
            super(context, aMap, drivePath, latLonPoint, latLonPoint1,throughPointList);
        }
        // 修改路线宽度
        public float getLineWidth() {
            return lineWidth;
        }

        // 修改路线颜色
        @Override
        protected int getDriveColor() {
            return color;
        }


        /*修改起点marker样式*/
        @Override
        protected BitmapDescriptor getStartBitmapDescriptor() {
            BitmapDescriptor reBitmapDescriptor=new BitmapDescriptorFactory().fromResource(R.drawable.amap_start);

            return reBitmapDescriptor;
        }


        /*修改中间点marker样式*/
        @Override
        protected BitmapDescriptor getDriveBitmapDescriptor() {
            BitmapDescriptor reBitmapDescriptor=new BitmapDescriptorFactory().fromResource(R.drawable.amap_through);

            return reBitmapDescriptor;
        }
        /*修改终点marker样式*/
        @Override
        protected BitmapDescriptor getEndBitmapDescriptor() {
            BitmapDescriptor reBitmapDescriptor=new BitmapDescriptorFactory().fromResource(R.drawable.amap_end);
            return reBitmapDescriptor;
        }
        // 一个工具方法，修改颜色和宽度
        public void setView(int color ,float width) {
            this.color=color;
            this.lineWidth=width;
        }
    }

}
