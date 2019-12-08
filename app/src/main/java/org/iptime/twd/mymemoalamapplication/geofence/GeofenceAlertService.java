package org.iptime.twd.mymemoalamapplication.geofence;

import android.app.Notification;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.iptime.twd.mymemoalamapplication.BaseApplication;
import org.iptime.twd.mymemoalamapplication.BaseService;
import org.iptime.twd.mymemoalamapplication.NotificationActionButtonReceiver;
import org.iptime.twd.mymemoalamapplication.edit.Memo;

public class GeofenceAlertService extends BaseService {

    private GeofenceManager mManager;
    private NotificationActionButtonReceiver mReceiver;

    public GeofenceAlertService() {
    }

    @Override
    public void onCreate() {
        mManager = ((BaseApplication) getApplicationContext()).getGeofenceManager();
        mManager.startGeofencesAlert();

        startForeground(2, new Notification());
        // Receiver 등록
        mReceiver = new NotificationActionButtonReceiver();
        IntentFilter intentFilter = new IntentFilter("My.Notification.Action");
        this.registerReceiver(mReceiver, intentFilter);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.hasExtra("method")) {
            String method = intent.getStringExtra("method");

            Log.e(TAG, method);

            switch (method) {
                case "startAlert":
                    mManager.startGeofencesAlert();
                    break;
                case "stopAlert":
                    mManager.stopGeofencesAlert();
                    break;
                case "addAlert":
                    if (intent.hasExtra("memo")) {
                        Memo memo = intent.getParcelableExtra("memo");
                        mManager.addGeofence(memo);
                    }
                    break;
                case "rmAlert":
                    if (intent.hasExtra("memo")) {
                        Memo memo = intent.getParcelableExtra("memo");
                        mManager.rmGeofence(memo);
                    }
                    break;
                case "clearAlert":
                    mManager.clearGeofence();
                    break;
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");

        mManager.stopGeofencesAlert();

        unregisterReceiver(mReceiver);

        super.onDestroy();
    }
}
