package com.example.njames.myfirstapp;

/**
 * Created by njames on 6/3/17.
 */

class AuthenticationSingleton {
    private static final AuthenticationSingleton ourInstance = new AuthenticationSingleton();

    static AuthenticationSingleton getInstance() {
        return ourInstance;
    }

    private AuthenticationSingleton() {
    }
}
