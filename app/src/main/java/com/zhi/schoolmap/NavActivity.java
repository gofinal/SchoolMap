package com.zhi.schoolmap;


import android.content.Intent;
import android.os.Bundle;


import com.amap.api.navi.AMapNaviView;
import com.amap.api.navi.model.NaviLatLng;


public class NavActivity extends BaseNavActivity {


    double startLatitude,startLongitude,endLatitude,endLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav);
        mAMapNaviView = (AMapNaviView) findViewById(R.id.navi_view);
        mAMapNaviView.onCreate(savedInstanceState);
        mAMapNaviView.setAMapNaviViewListener(this);
        Intent intent =getIntent();
        startLatitude=intent.getDoubleExtra("startLatitude",0);
        startLongitude=intent.getDoubleExtra("startLongitude",0);
        endLatitude=intent.getDoubleExtra("endLatitude",0);
        endLongitude=intent.getDoubleExtra("endLongitude",0);
    }


    @Override
    public void onInitNaviSuccess() {
        mAMapNavi.calculateWalkRoute(new NaviLatLng(startLatitude,startLongitude), new NaviLatLng(endLatitude,endLongitude));
    }


}
