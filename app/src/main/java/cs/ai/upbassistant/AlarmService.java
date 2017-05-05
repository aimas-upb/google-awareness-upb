package cs.ai.upbassistant;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

import static android.support.v4.content.WakefulBroadcastReceiver.startWakefulService;

public class AlarmService extends BroadcastReceiver {

    public static String filename = null;
    private int seconds = 10;

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Eu sunt alarma", Toast.LENGTH_SHORT).show();
        Intent awarnessIntent = new Intent(context, AwarenessBackgroundService.class);
        startWakefulService(context, awarnessIntent);
    }

    public void setAlarm(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmService.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        Integer frec = new Integer(this.seconds);
        filename = DateFormat.getDateTimeInstance().format(new Date()) + "_" + frec.toString() +
                new String(".json");

        // 10 de secunde
        int minutes = 1000 * this.seconds;
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),minutes, pendingIntent );

        /*
         * Enable to automatically restart the alarm when the device is rebooted
         */
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    public void cancelAlarm(Context context) {
        Intent intent = new Intent(context, AlarmService.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pendingIntent);

        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
