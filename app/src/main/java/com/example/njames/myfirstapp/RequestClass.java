package com.example.njames.myfirstapp;

/**
 * Created by njames on 5/29/2017.
 */

public class RequestClass  {

    public String thingName;
    public String token;
    int cnt = 0;
    public String[] td = new String[3];
    Integer tu[] = new Integer[3];
    Integer tl[] = new Integer[3];

    public RequestClass(String thingName, String token) {
        this.thingName = thingName;
        this.token = token;
        cnt = 0;
        for (int i=0;i<td.length;i++) {
            tu[i] = 0;
            tl[i] = 0;
            td[i] = "Temp "+String.valueOf(i+1);
        }
    }

    public void add(String tdx, Integer tux, Integer tlx) {
        if (cnt >=3) {
            return;
        }
        td[cnt] = tdx;
        tu[cnt] = tux;
        tl[cnt] = tlx;
        cnt++;
    }
};
