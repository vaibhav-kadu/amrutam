package com.amrutamtechnology.amrutam;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.FirebaseApp;


import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
    }
}
