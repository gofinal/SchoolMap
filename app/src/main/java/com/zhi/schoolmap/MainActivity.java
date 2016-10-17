package com.zhi.schoolmap;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Camera;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.Projection;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Text;
import com.amap.api.maps.model.TextOptions;
import com.amap.api.maps.overlay.WalkRouteOverlay;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;


public class MainActivity extends AppCompatActivity implements LocationSource
        ,AMapLocationListener ,AMap.OnMarkerClickListener,RouteSearch.OnRouteSearchListener {
    private MapView mMapView;
    private AMap aMap;//定义一个地图对象
    private UiSettings mUiSettings;//定义一个UiSettings对象


    //定位
    private OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;

    LatLng schoolLatng;
    //List<Marker> markers;

    Button button ;
    TextView titleText,contentText;
    RelativeLayout layout;
    ImageView imageView;

    //路径规划
    private RouteSearch mRouteSearch;
    private WalkRouteResult mWalkRouteResult;
    private LatLonPoint mStartPoint ;//起点
    private LatLonPoint mEndPoint;//终点
    private final int ROUTE_TYPE_WALK = 3;

    private LinearLayout mHideLayout;
    private TextView navDetailsText;
    private Button navButton;
    private ProgressDialog progDialog = null;// 搜索时进度条

    private boolean isGoto=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，实现地图生命周期管理
        mMapView.onCreate(savedInstanceState);
        mRouteSearch = new RouteSearch(this);//初始化routeSearch 对象
        mRouteSearch.setRouteSearchListener(this);//设置数据回调监听器
        initViews();
        initDatas();
        init();
    }

    private void initViews() {
        layout= (RelativeLayout) findViewById(R.id.layout);
        button= (Button) findViewById(R.id.button);
        titleText= (TextView) findViewById(R.id.title_text);
        contentText= (TextView) findViewById(R.id.content_text);
        imageView= (ImageView) findViewById(R.id.image_view);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchRouteResult(ROUTE_TYPE_WALK, RouteSearch.WalkDefault);
            }
        });

        mHideLayout= (LinearLayout) findViewById(R.id.hide_layout);
        navDetailsText= (TextView) findViewById(R.id.line_text);
        navButton= (Button) findViewById(R.id.nav_button);
    }
    /**
     * 开始搜索路径规划方案
     */
    public void searchRouteResult(int routeType, int mode) {
        if (mStartPoint == null) {
            Toast.makeText(this, "定位中，稍后再试...", Toast.LENGTH_SHORT).show();
            if (mlocationClient == null) {
                mlocationClient = new AMapLocationClient(this);
                mLocationOption = new AMapLocationClientOption();
                //设置定位监听
                mlocationClient.setLocationListener(this);
                //设置为高精度定位模式
                mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
                //设置定位参数
                mlocationClient.setLocationOption(mLocationOption);
                // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
                // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
                // 在定位结束后，在合适的生命周期调用onDestroy()方法
                // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
                mlocationClient.startLocation();
                isGoto=true;
            }
            return;
        }
        if (mEndPoint == null) {
            Toast.makeText(this, "终点未设置", Toast.LENGTH_SHORT).show();
        }
       showProgressDialog();
        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(
                mStartPoint, mEndPoint);
        if (routeType == ROUTE_TYPE_WALK) {// 步行路径规划
            RouteSearch.WalkRouteQuery query = new RouteSearch.WalkRouteQuery(fromAndTo, mode);
            mRouteSearch.calculateWalkRouteAsyn(query);// 异步路径规划步行模式查询
        }
    }
    /**
     * 显示进度框
     */
    private void showProgressDialog() {
        if (progDialog == null)
            progDialog = new ProgressDialog(this);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setIndeterminate(false);
        progDialog.setCancelable(false);
        progDialog.setMessage("正在搜索,请稍后...");
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
    private void initMarkers() {
    /*    Marker marker = aMap.addMarker(new MarkerOptions().
                position(schoolLatng).
                title("学校"));*/
        Marker marker1 =aMap.addMarker(new MarkerOptions().
                position(new LatLng(36.009068,103.970642)).
                title(getString(R.string.xueshubaogaoting)).
                snippet(getString(R.string.null_data)));

        TextOptions textOptions1 = new TextOptions()
                .position(new LatLng(36.009068,103.970642))
                .text(getString(R.string.xueshubaogaoting))
                .fontColor(Color.BLACK)
                .fontSize(30)
                .rotate(0)
                .align(Text.ALIGN_CENTER_HORIZONTAL, Text.ALIGN_CENTER_VERTICAL)
                .zIndex(1.f).typeface(Typeface.DEFAULT_BOLD);
        aMap.addText(textOptions1);


        Marker marker2 =aMap.addMarker(new MarkerOptions().
                position(new LatLng(36.008774,103.972121)).
                title(getString(R.string.shiyanlou)).
                snippet(getString(R.string.shiyanlou_s)));

        TextOptions textOptions2 = new TextOptions()
                .position(new LatLng(36.008774,103.972121))
                .text(getString(R.string.shiyanlou))
                .fontColor(Color.BLACK)
                .fontSize(30)
                .rotate(0)
                .align(Text.ALIGN_CENTER_HORIZONTAL, Text.ALIGN_CENTER_VERTICAL)
                .zIndex(1.f).typeface(Typeface.DEFAULT_BOLD);
        aMap.addText(textOptions2);

        Marker marker3 =aMap.addMarker(new MarkerOptions().
                position(new LatLng(36.009364,103.97335)).
                title(getString(R.string.tushuguan)).
                snippet(getString(R.string.tushuguan_s)));

        TextOptions textOptions3 = new TextOptions()
                .position(new LatLng(36.009364,103.97335))
                .text(getString(R.string.tushuguan))
                .fontColor(Color.BLACK)
                .fontSize(30)
                .rotate(0)
                .align(Text.ALIGN_CENTER_HORIZONTAL, Text.ALIGN_CENTER_VERTICAL)
                .zIndex(1.f).typeface(Typeface.DEFAULT_BOLD);
        aMap.addText(textOptions3);

        Marker marker4 =aMap.addMarker(new MarkerOptions().
                position(new LatLng(36.008251,103.972263)).
                title(getString(R.string.shitang))
                .snippet(getString(R.string.shitang_s)));

        TextOptions textOptions4 = new TextOptions()
                .position(new LatLng(36.008251,103.972263))
                .text(getString(R.string.shitang))
                .fontColor(Color.BLACK)
                .fontSize(30)
                .rotate(0)
                .align(Text.ALIGN_CENTER_HORIZONTAL, Text.ALIGN_CENTER_VERTICAL)
                .zIndex(1.f).typeface(Typeface.DEFAULT_BOLD);
        aMap.addText(textOptions4);

        Marker marker5 =aMap.addMarker(new MarkerOptions().
                position(new LatLng(36.008251,103.973458)).
                title(getString(R.string.tiyuguan)).
                snippet(getString(R.string.null_data)));

        TextOptions textOptions5 = new TextOptions()
                .position(new LatLng(36.008251,103.973458))
                .text(getString(R.string.tiyuguan))
                .fontColor(Color.BLACK)
                .fontSize(30)
                .rotate(0)
                .align(Text.ALIGN_CENTER_HORIZONTAL, Text.ALIGN_CENTER_VERTICAL)
                .zIndex(1.f).typeface(Typeface.DEFAULT_BOLD);
        aMap.addText(textOptions5);

        Marker marker6 =aMap.addMarker(new MarkerOptions().
                position(new LatLng(36.009291,103.974046)).
                title(getString(R.string.caochang)).
                snippet(getString(R.string.null_data)));


        TextOptions textOptions6 = new TextOptions()
                .position(new LatLng(36.009291,103.974046))
                .text(getString(R.string.caochang))
                .fontColor(Color.BLACK)
                .fontSize(30)
                .rotate(0)
                .align(Text.ALIGN_CENTER_HORIZONTAL, Text.ALIGN_CENTER_VERTICAL)
                .zIndex(1.f).typeface(Typeface.DEFAULT_BOLD);
        aMap.addText(textOptions6);

        Marker marker7 =aMap.addMarker(new MarkerOptions().
                position(new LatLng(36.010612,103.973458)).
                title(getString(R.string.keyanlou)).
                snippet(getString(R.string.null_data)));

        TextOptions textOptions7 = new TextOptions()
                .position(new LatLng(36.010612,103.973458))
                .text(getString(R.string.keyanlou))
                .fontColor(Color.BLACK)
                .fontSize(30)
                .rotate(0)
                .align(Text.ALIGN_CENTER_HORIZONTAL, Text.ALIGN_CENTER_VERTICAL)
                .zIndex(1.f).typeface(Typeface.DEFAULT_BOLD);
        aMap.addText(textOptions7);


       /* markers= new ArrayList<>();
        markers.add(marker1);
        markers.add(marker2);
        markers.add(marker3);
        markers.add(marker4);
        markers.add(marker5);
        markers.add(marker6);
        markers.add(marker7);*/

    }

    private void initDatas() {
        schoolLatng = new LatLng(36.008774,103.972121);

    }

    private void init() {
        if (aMap == null) {
            aMap = mMapView.getMap();
            mUiSettings = aMap.getUiSettings();
            mUiSettings.setMyLocationButtonEnabled(true);
            mUiSettings.setScaleControlsEnabled(true);
            mUiSettings.setCompassEnabled(true);
            aMap.setLocationSource(this);// 设置定位监听
            aMap.setOnMarkerClickListener(this);//marker点击事件
            aMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(
                   schoolLatng, 17, 30, 30)));
            initMarkers();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，实现地图生命周期管理
        mMapView.onPause();
        deactivate();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，实现地图生命周期管理
        mMapView.onSaveInstanceState(outState);
    }

    @Override//激活定位
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        Log.e("激活定位","++++++");
        mListener = onLocationChangedListener;
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();
        }
    }

    @Override//停止定位
    public void deactivate() {
        Log.e("停止定位","++++++");
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        mStartPoint = new LatLonPoint(aMapLocation.getLatitude(), aMapLocation.getLongitude());//起点，116.335891,39.942295
        if (mListener != null) {
            mListener.onLocationChanged(aMapLocation);// 显示系统小蓝点
        }

        if (isGoto){
            searchRouteResult(ROUTE_TYPE_WALK, RouteSearch.WalkDefault);
            isGoto=false;
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (aMap != null) {
            jumpPoint(marker);
            aMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(
                    marker.getPosition(), 18, 30, 30)));
            showLayout(marker);
        }
        return true;
    }

    /**
     * marker点击时跳动一下
     */
    public void jumpPoint(final Marker marker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = aMap.getProjection();
        final LatLng markerLatlng = marker.getPosition();
        Point markerPoint = proj.toScreenLocation(markerLatlng);
        markerPoint.offset(0, -100);
        final LatLng startLatLng = proj.fromScreenLocation(markerPoint);
        final long duration = 1500;

        final Interpolator interpolator = new BounceInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * markerLatlng.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * markerLatlng.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));
                if (t < 1.0) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }



    private void showLayout(Marker marker) {

        mEndPoint = new LatLonPoint(marker.getPosition().latitude,marker.getPosition().longitude);
        button.setVisibility(View.VISIBLE);
        layout.setVisibility(View.VISIBLE);
        titleText.setText(marker.getTitle());
        contentText.setText(marker.getSnippet());
        if (marker.getTitle().equals(getString(R.string.xueshubaogaoting))){
            imageView.setImageResource(R.mipmap.xueshubaogaoting);
        }else  if (marker.getTitle().equals(getString(R.string.shitang))){
            imageView.setImageResource(R.mipmap.shitang);
        }else  if (marker.getTitle().equals(getString(R.string.shiyanlou))){
            imageView.setImageResource(R.mipmap.shiyanlou);
        }else  if (marker.getTitle().equals(getString(R.string.tiyuguan))){
            imageView.setImageResource(R.mipmap.tiyuguan);
        }else  if (marker.getTitle().equals(getString(R.string.tushuguan))){
            imageView.setImageResource(R.mipmap.tushuguan);
        }else  if (marker.getTitle().equals(getString(R.string.caochang))){
            imageView.setImageResource(R.mipmap.caochang);
        }else  if (marker.getTitle().equals(getString(R.string.keyanlou))){
            imageView.setImageResource(R.mipmap.keyanlou);
        }
    }

    private void hideLayout(){
        button.setVisibility(View.GONE);
        layout.setVisibility(View.GONE);
    }

    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

    }

    @Override
    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {

    }

    @Override
    public void onWalkRouteSearched(WalkRouteResult result, int errorCode) {
        dissmissProgressDialog();
        hideLayout();
        dissmissProgressDialog();
        aMap.clear();// 清理地图上的所有覆盖物
        if (errorCode == 1000) {
            if (result != null && result.getPaths() != null) {
                if (result.getPaths().size() > 0) {
                    mWalkRouteResult = result;
                    final WalkPath walkPath = mWalkRouteResult.getPaths()
                            .get(0);
                    WalkRouteOverlay walkRouteOverlay = new WalkRouteOverlay(
                            this, aMap, walkPath,
                            mWalkRouteResult.getStartPos(),
                            mWalkRouteResult.getTargetPos());
                    walkRouteOverlay.removeFromMap();
                    walkRouteOverlay.addToMap();
                    walkRouteOverlay.zoomToSpan();
                    mHideLayout.setVisibility(View.VISIBLE);
                    int dis = (int) walkPath.getDistance();
                    int dur = (int) walkPath.getDuration();
                    String des = AMapUtil.getFriendlyTime(dur)+"("+AMapUtil.getFriendlyLength(dis)+")";
                    navDetailsText.setText(des);
                    navButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(MainActivity.this,NavActivity.class);
                            intent.putExtra("startLatitude",mStartPoint.getLatitude());
                            intent.putExtra("startLongitude",mStartPoint.getLongitude());
                            intent.putExtra("endLatitude",mEndPoint.getLatitude());
                            intent.putExtra("endLongitude",mEndPoint.getLongitude());
                            startActivity(intent);
                        }
                    });
                } else if (result != null && result.getPaths() == null) {
                    Toast.makeText(this, "对不起，没有获取到相关数据", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "对不起，没有搜索到相关数据", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "错误：errorCode"+errorCode, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

    }
}
