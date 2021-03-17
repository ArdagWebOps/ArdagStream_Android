package in.ardagro.ardagstream;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.util.Calendar;

public class StreamBroadCastReceiver extends BroadcastReceiver {
    PendingIntent pendingIntent;
    AlarmManager alarmManager;
    @Override
    public void onReceive(Context context, Intent intent) {
        alarmManager = (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        Intent uploadcamshotSerRec = new Intent(context.getApplicationContext(),StreamBroadCastReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(context,0,uploadcamshotSerRec,0);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ((Calendar.getInstance().getTimeInMillis()) +1000*60*2),pendingIntent);
        Intent uploadCamShotIntent = new Intent(context ,UploadCamshotService.class);
        context.startService(uploadCamShotIntent);
    }
}
