package com.example.njames.myfirstapp;

/**
 * Created by njames on 5/29/2017.
 */

public class RequestClass {
    public String td0;
    public String td1;
    public String td2;
    public String thingName;

    public RequestClass(String thingName,String label0,String label1,String label2) {
        this.thingName = thingName;
        this.td0 = label0;
        this.td1 = label1;
        this.td2 = label2;
    }

    public RequestClass() {
    }
};
