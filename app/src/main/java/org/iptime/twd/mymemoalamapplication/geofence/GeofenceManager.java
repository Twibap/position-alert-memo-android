/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.iptime.twd.mymemoalamapplication.geofence;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.iptime.twd.mymemoalamapplication.MainActivity;
import org.iptime.twd.mymemoalamapplication.R;
import org.iptime.twd.mymemoalamapplication.edit.Memo;

import java.util.ArrayList;

/**
 * Demonstrates how to create and remove geofences using the GeofencingApi. Uses an IntentService
 * to monitor geofence transitions and creates notifications whenever a device enters or exits
 * a geofence.
 * <p>
 * This sample requires a device's Location settings to be turned on. It also requires
 * the ACCESS_FINE_LOCATION permission, as specified in AndroidManifest.xml.
 * <p>
 */
public class GeofenceManager implements OnCompleteListener<Void>{

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private Context mContext;

    /**
     * Tracks whether the user requested to add or remove geofences, or to do neither.
     * 위치정보 접근 권한이 없을 때 {@link #startGeofencesAlert()} 에서 ADD
     * 위치정보 접근 권한이 없을 때 {@link #stopGeofencesAlert()}  에서 REMOVE
     * {@link #onComplete(Task)}      에서 NONE
     */
    private enum PendingGeofenceTask {
        ADD, REMOVE, NONE
    }

    /**
     * Provides access to the Geofencing API.
     */
    private GeofencingClient mGeofencingClient;

    /**
     * The list of geofences used in this sample.
     */
    private ArrayList<Geofence> mGeofenceList;

    /**
     * Used when requesting to add or remove geofences.
     */
    private PendingIntent mGeofencePendingIntent;

    private PendingGeofenceTask mPendingGeofenceTask = PendingGeofenceTask.NONE;

    // onCreate
    public GeofenceManager(Context context){
        mContext = context;

        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<>();

        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;

        mGeofencingClient = LocationServices.getGeofencingClient(mContext);

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            performPendingGeofenceTask();
        }

    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    /**
     * 알람 시작
     * This method should be called after the user has granted the location
     * permission.
     *
     * Adds geofences, which sets alerts to be notified when the device enters or exits one of the
     * specified geofences. Handles the success or failure results returned by addGeofences().
     */
    @SuppressWarnings("MissingPermission")
    public void startGeofencesAlert() {
        if (!checkPermissions()) {
            mPendingGeofenceTask = PendingGeofenceTask.ADD;
            requestPermissions();

            showSnackbar(mContext.getString(R.string.insufficient_permissions));
            return;
        }

        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnCompleteListener(this);
        updateGeofencesAdded(true);

        Log.e(TAG, "Alert Start");
        checkAlertList();
    }

    /**
     * 알람 끝
     * This method should be called after the user has granted the location
     * permission.
     *
     * Removes geofences, which stops further notifications when the device enters or exits
     * previously registered geofences.
     */
    @SuppressWarnings("MissingPermission")
    public void stopGeofencesAlert() {
        if (!checkPermissions()) {
            mPendingGeofenceTask = PendingGeofenceTask.REMOVE;
            requestPermissions();

            showSnackbar(mContext.getString(R.string.insufficient_permissions));
            return;
        }

        mGeofencingClient.removeGeofences(getGeofencePendingIntent()).addOnCompleteListener(this);
        updateGeofencesAdded(false);

        Log.e(TAG, "Alert Stop");
    }

    /**
     * Runs when the result of calling {@link #startGeofencesAlert()} and/or {@link #stopGeofencesAlert()}
     * is available.
     * @param task the resulting Task, containing either a result or error.
     */
    @Override
    public void onComplete(@NonNull Task<Void> task) {

        mPendingGeofenceTask = PendingGeofenceTask.NONE;
        if (task.isSuccessful()) {

            String message = getGeofencesAdded() ? mContext.getString(R.string.geofences_added) :
                    mContext.getString(R.string.geofences_removed);
//            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();

        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(mContext, task.getException());
            Log.w(TAG, errorMessage);
        }

    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(mContext, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * 알람 구역 추가
     * This sample hard codes geofence data. A real app might dynamically create geofences based on
     * the user's location.
     */
    public void addGeofence(Memo memo) {

        mGeofenceList.add(new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(memo.getStringId())

                // Set the circular region of this geofence.
                .setCircularRegion(
                        memo.mPlace.latitude,
                        memo.mPlace.longitude,
                        memo.mAlertDistance
                )

                // Set the expiration duration of the geofence. This geofence gets automatically
                // removed after this period of time.
                .setExpirationDuration(Geofence.NEVER_EXPIRE)

                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes(memo.mGeofenceTransition)

                // Create the geofence.
                .build()
        );

        Log.e(TAG, "Alert Added - "+memo.toString());

    }

    // 알람 구역 삭제
    public void rmGeofence(Memo memo){

        for (int i = 0; i < mGeofenceList.size(); i++){
            if (memo.getStringId().equals(mGeofenceList.get(i).getRequestId()))
                mGeofenceList.remove(i);
        }

    }

    public void checkAlertList(){
        if (mGeofenceList.size() != 0) {
            Log.e(TAG, "GeofenceList Check");
            for (int i = 0; i < mGeofenceList.size(); i++)
                Log.e(TAG, "\t"+mGeofenceList.get(i).toString());
        }
    }

    // 알람 비우기
    public void clearGeofence(){
        mGeofenceList.clear();
    }

    /**
     * Shows a {@link Snackbar} using {@code text}.
     *
     * @param text The Snackbar text.
     */
    private void showSnackbar(final String text) {
        if (mContext instanceof Activity) {
            View container = ((Activity)mContext).findViewById(android.R.id.content);
            if (container != null) {
                Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        if (mContext instanceof Activity) {
            Activity activity = (Activity) mContext;
            Snackbar
                    .make(
                    activity.findViewById(android.R.id.content),
                    mContext.getString(mainTextStringId),
                    Snackbar.LENGTH_INDEFINITE)

                    .setAction(mContext.getString(actionStringId), listener)
                    .show();
        } else {
            Toast.makeText(mContext,
                    "Check ["+this.getClass().getSimpleName()+"] method [showSnackbar(int, int, OnClickListener)]",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Returns true if geofences were added, otherwise false.
     */
    public boolean getGeofencesAdded() {
        return PreferenceManager
                .getDefaultSharedPreferences(mContext)
                .getBoolean(Constants.GEOFENCES_ADDED_KEY, false);
    }

    /**
     * Stores whether geofences were added ore removed in {@link SharedPreferences};
     *
     * @param added Whether geofences were added or removed.
     */
    private void updateGeofencesAdded(boolean added) {
        PreferenceManager.getDefaultSharedPreferences(mContext)
                .edit()
                .putBoolean(Constants.GEOFENCES_ADDED_KEY, added)
                .apply();
    }

    /**
     * Performs the geofencing task that was pending until location permission was granted.
     *
     * 위치정보 확인 후 다시 실행하게 함
     */
    private void performPendingGeofenceTask() {
        if (mPendingGeofenceTask == PendingGeofenceTask.ADD) {
            startGeofencesAlert();
        } else if (mPendingGeofenceTask == PendingGeofenceTask.REMOVE) {
            stopGeofencesAlert();
        }
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale((Activity) mContext,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions((Activity) mContext,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions((Activity) mContext,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

}
