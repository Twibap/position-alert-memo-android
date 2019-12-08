package org.iptime.twd.mymemoalamapplication;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import org.iptime.twd.mymemoalamapplication.edit.Memo;

public class NotificationActionButtonReceiver extends BaseReceiver{

    public final static int REQUEST_ACTION_CANCEL = 1001;
    public final static int REQUEST_ACTION_SHOW_DETAIL = 1002;
    public final static int REQUEST_ACTION_SHOW_AGAIN = 1003;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        if (intent.hasExtra("cancel")){
            int cancel_id = intent.getIntExtra("cancel", -1);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(cancel_id);
        }

        if (intent.hasExtra("action")){
            int requestCode = intent.getIntExtra("action", -1);

            switch (requestCode){
                case REQUEST_ACTION_CANCEL:
                    deleteMemo(context, intent);
                    break;
                case REQUEST_ACTION_SHOW_AGAIN:
                    // do nothing
                    break;
                case REQUEST_ACTION_SHOW_DETAIL:
                    // never reached
                    break;
            }
        }
    }

    private void deleteMemo(Context context, Intent intent) {
        if (intent.hasExtra("memo")){
            Memo memo = intent.getParcelableExtra("memo");

            // 알람 등록 해제
            ((BaseApplication) context.getApplicationContext()).getGeofenceManager().rmGeofence(memo);

            // 재분류
            memo.mCategory = Memo.COMPLETE;

            SharedPreferences sf = context.getSharedPreferences("memo", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sf.edit();

            Gson gson = new Gson();
            editor.putString(memo.getStringId(), gson.toJson(memo));

            editor.apply();

        }
    }
}