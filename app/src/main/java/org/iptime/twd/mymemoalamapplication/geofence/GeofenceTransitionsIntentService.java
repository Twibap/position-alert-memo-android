package org.iptime.twd.mymemoalamapplication.geofence;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.gson.Gson;

import org.iptime.twd.mymemoalamapplication.NotificationActionButtonReceiver;
import org.iptime.twd.mymemoalamapplication.R;
import org.iptime.twd.mymemoalamapplication.edit.Memo;
import org.iptime.twd.mymemoalamapplication.edit.memo.EditMemoActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tky476 on 2017. 9. 12..
 */

/**
 * Listener for geofence transition changes.
 *
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition. Creates a notification
 * as the output.
 */
public class GeofenceTransitionsIntentService extends IntentService{

    private static final String TAG = "GeofenceTransitionsIS";

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public GeofenceTransitionsIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.e(TAG, "onHandleIntent");

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Get the transition details as a String.
            List<Memo> geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition,
                    triggeringGeofences);

            // Send notification and log the transition details.
            for(Memo memo : geofenceTransitionDetails){
                Log.e(TAG, memo.toString());
                sendNotification(memo);
            }
        } else {
            // Log the error.
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }
    }

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * ArrayList 로 반환하도록 수정
     *
     * @param geofenceTransition    The ID of the geofence transition.
     * @param triggeringGeofences   The geofence(s) triggered.
     * @return                      The transition details formatted as String.
     */
    private List<Memo> getGeofenceTransitionDetails(
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        // Get the Ids of each geofence that was triggered.
        ArrayList<Memo> triggeringGeofencesIdsList = new ArrayList<>();
        SharedPreferences sf = getSharedPreferences("memo", MODE_PRIVATE);
        Gson gson = new Gson();
        for (Geofence geofence : triggeringGeofences) {
            String key = geofence.getRequestId();
            triggeringGeofencesIdsList.add(gson.fromJson(sf.getString(key, null), Memo.class));
        }

        return triggeringGeofencesIdsList;
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MainActivity.
     * @param memo
     */
    private void sendNotification(Memo memo) {

        if (memo != null) {
            Context context = getApplicationContext();
            // 알람 소리 설정
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            // Notification 설정
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_action_edit_memo).setContentTitle(memo.mTitle).setContentText(memo.mPlaceAddr).setSound(defaultSoundUri);
            Intent resultIntent = new Intent(context, EditMemoActivity.class);
            resultIntent.putExtra("memo", memo);

            // 세부내용이 있다면 확장 Notification
            if (memo.mContent != null) {
                NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
                style.bigText(memo.mContent);
                style.setSummaryText(memo.mPlaceAddr);

                builder.setStyle(style);
            }

            // ** Notification 버튼 설정 시작
            // [완료] 버튼 : 메모를 삭제한다.
            Intent intentActionDone = new Intent(context, NotificationActionButtonReceiver.class);
            intentActionDone.putExtra("action", NotificationActionButtonReceiver.REQUEST_ACTION_CANCEL);
            intentActionDone.putExtra("memo", memo);
            intentActionDone.putExtra("cancel", memo.mId);
            PendingIntent pendingActionDone =
                    PendingIntent.getBroadcast(
                            context,
                            NotificationActionButtonReceiver.REQUEST_ACTION_CANCEL,
                            intentActionDone,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            NotificationCompat.Action actionDone =
                    new NotificationCompat.Action(
                            R.drawable.ic_action_delete,
                            getString(R.string.button_notification_complete),
                            pendingActionDone
                    );
            builder.addAction(actionDone);

            // [상세보기] 버튼
            Intent intentActionDetail = new Intent(context, EditMemoActivity.class);
            intentActionDetail.putExtra("memo", memo);  // Activity에 메모 전달
            intentActionDetail.putExtra("cancel", memo.mId);
            PendingIntent pendingActionDetail =
                    PendingIntent.getActivity(
                            context,
                            NotificationActionButtonReceiver.REQUEST_ACTION_SHOW_DETAIL,
                            intentActionDetail,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            NotificationCompat.Action actionDetail =
                    new NotificationCompat.Action(
                            R.drawable.ic_action_detail,
                            getString(R.string.button_notification_edit),
                            pendingActionDetail
                    );
            builder.addAction(actionDetail);

            // [나중에] 버튼 : Notification 을 삭제한다
            Intent intentActionSnooze = new Intent(context, NotificationActionButtonReceiver.class);
            intentActionSnooze.putExtra("action", NotificationActionButtonReceiver.REQUEST_ACTION_SHOW_AGAIN);
            intentActionSnooze.putExtra("cancel", memo.mId);
            PendingIntent pendingActionSnooze =
                    PendingIntent.getBroadcast(
                            context,
                            NotificationActionButtonReceiver.REQUEST_ACTION_SHOW_AGAIN,
                            intentActionSnooze,
                            PendingIntent.FLAG_CANCEL_CURRENT
                    );
            NotificationCompat.Action actionSnooze =
                    new NotificationCompat.Action(
                            R.drawable.ic_action_snooze,
                            getString(R.string.button_notification_snooze),
                            pendingActionSnooze
                    );
            builder.addAction(actionSnooze);

            // ** 버튼설정 끝

            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(EditMemoActivity.class);

            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);

            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(memo.mId, PendingIntent.FLAG_UPDATE_CURRENT);

            // Head Up 알림
            builder.setFullScreenIntent(resultPendingIntent, true);
//        builder.setContentIntent(resultPendingIntent);

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            manager.notify(memo.mId, builder.build());

            Log.i(TAG, memo.toString());
        } else {
            Toast.makeText(this, "Memo is Null", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, TAG+" onCreate", Toast.LENGTH_SHORT).show();

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, TAG+" onDestroy", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }
}
