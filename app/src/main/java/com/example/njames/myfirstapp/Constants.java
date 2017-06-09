package com.example.njames.myfirstapp;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * Created by njames on 5/30/2017.
 */

public class Constants {
    public static final String ACTION_ADD_ENDPOINT = "com.example.njames.myfirstapp.action.ADD_ENDPOINT";
    public static final String ACTION_GET_THINGS = "com.example.njames.myfirstapp.action.GET_THINGS";
    public static final String ACTION_POST_FORM = "com.example.njames.myfirstapp.action.POST_FORM";
    public static final String ACTION_RECEIVE_DATA = "com.example.njames.myfirstapp.action.RECEIVE_DATA";
    public static final String ACTION_AWS_LOGIN = "com.example.njames.myfirstapp.action.AWS_LOGIN";
    public static final String ACTION_SIGNUP_DONE = "com.example.njames.myfirstapp.action.SIGNUP_DONE";
    public static final String EXTRA_THING_ID = "com.example.njames.myfirstapp.extra.THING_ID";
    public static final String EXTRA_THING_LIST = "com.example.njames.myfirstapp.extra.THING_LIST";
    public static final String EXTRA_POST_RESPONSE = "com.example.njames.myfirstapp.extra.POST_RESPONSE";
    public static final String EXTRA_POST_THINGNAME = "com.example.njames.myfirstapp.extra.POST_THINGNAME";
    public static final String EXTRA_AWS_ID = "com.example.njames.myfirstapp.extra.AWS_ID";
    public static final String PREFKEY_BBQ_AUTH = "com.example.njames.myfirstapp.preferences.AUTH";
    public static final String LOGIN_ACCOUNT = "com.example.njames.myfirstapp.preferences.AUTH.LOGIN_ACCOUNT";
    public static final String LOGIN_TOKEN = "com.example.njames.myfirstapp.preferences.AUTH.LOGIN_TOKEN";
    public static final String FIREBASE_TOKEN = "com.example.njames.myfirstapp.preferences.AUTH.FIREBASE_TOKEN";
    public static final String THINGS_LIST = "com.example.njames.myfirstapp.preferences.AUTH.THINGS_LIST";
    public static final String COGNITO_POOL_ID = "us-west-2:96107a1e-261a-4a63-8b28-776d91dd44d7";

    public static void hideKeyboard(AppCompatActivity app) {
        View view = app.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)app.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}
