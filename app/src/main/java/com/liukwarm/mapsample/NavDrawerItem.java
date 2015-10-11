package com.liukwarm.mapsample;

import org.json.JSONObject;

/**
 * Created by liukwarm on 10/10/15.
 */
public class NavDrawerItem {

    int number;
    String name;
    double rating;
    String distance;
    int go;
    JSONObject obj;


    public NavDrawerItem(){}

    public NavDrawerItem(int number, String name, double rating, String distance, int go, JSONObject obj){
        this.number = number;
        this.name = name;
        this.rating = rating;
        this.distance = distance;
        this.go = go;
        this.obj = obj;
    }

}