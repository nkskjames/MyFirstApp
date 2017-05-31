package com.example.njames.myfirstapp;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;
/**
 * Created by njames on 5/29/2017.
 */


public interface UpdateThingDescInterface {

    @LambdaFunction
    void UpdateThingDesc(RequestClass request);

}
