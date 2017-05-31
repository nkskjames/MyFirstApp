package com.example.njames.myfirstapp;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBVersionAttribute;

import java.util.Set;

/**
 * Created by njames on 5/24/17.
 */

@DynamoDBTable(tableName = "UserRegistration")
public class DBUserRegistration {

    private String userId;
    private String thingId;

    @DynamoDBHashKey (attributeName = "user_id")
    public String getUser() {
        return userId;
    }

    public void setUser(String userId) {
        this.userId = userId;
    }

    @DynamoDBRangeKey (attributeName = "thing_id")
    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingId) {
        this.thingId = thingId;
    }
    /*
    @DynamoDBVersionAttribute
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
    */
}
