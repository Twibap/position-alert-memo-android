package org.iptime.twd.mymemoalamapplication;

import android.app.Application;

import org.iptime.twd.mymemoalamapplication.edit.Memo;
import org.iptime.twd.mymemoalamapplication.geofence.GeofenceManager;

import java.util.ArrayList;

/**
 * Created by tky476 on 2017. 9. 21..
 */

public class BaseApplication extends Application {
    private GeofenceManager mManager;
    private ArrayList<Memo> memoData;

    public GeofenceManager getGeofenceManager() {
        return mManager;
    }

    public void setGeofenceManager(GeofenceManager mManager) {
        this.mManager = mManager;
    }

    public ArrayList<Memo> getMemoData(){
        return memoData;
    }

    public void setMemoData(ArrayList<Memo> memoData) {
        this.memoData = memoData;
    }
}
