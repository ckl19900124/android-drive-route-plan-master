package com.amap.driveroute;

import java.io.Serializable;

/**
 * Created by Administrator on 2019-6-10.
 */

public class Mapinfo implements Serializable{

    /**
     * "4", 39.8530620000, 116.3747240000, "鸿运水果烟酒超市", "详细地址详细地址5555"
     */
    private String uid;
    private double juli;
    private String detilId;
    private String detilIdNuber;
    private double lat;
    private double lng;

    public Mapinfo(String s, double v, double v1, String str1, String str2) {
        this.uid = s;
        this.lat = v;
        this.lng = v1;
        this.detilId = str1;
        this.detilIdNuber = str2;

    }

    public Mapinfo() {

    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public double getJuli() {
        return juli;
    }

    public void setJuli(double juli) {
        this.juli = juli;
    }

    public String getDetilId() {
        return detilId;
    }

    public void setDetilId(String detilId) {
        this.detilId = detilId;
    }

    public String getDetilIdNuber() {
        return detilIdNuber;
    }

    public void setDetilIdNuber(String detilIdNuber) {
        this.detilIdNuber = detilIdNuber;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}
