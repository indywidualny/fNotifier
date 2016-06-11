package org.indywidualni.fnotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import net.grandcentrix.tray.AppPreferences;

public class PackageReplacedIntentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i("BroadcastReceiver", "********** Package replaced! **********");
        context = MyApplication.getContextOfApplication();

        // get Shared Preferences and Tray Preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        AppPreferences trayPreferences = new AppPreferences(context);

        /** App upgrade, the important part:
         *  Rewrite all the Shared Preferences used in NotificationsService into Tray Preferences
         *  Multi-process Shared Preferences are deprecated since API 23 */
        trayPreferences.put("feed_url", preferences.getString("feed_url", ""));
        trayPreferences.put("interval_pref", Integer.parseInt(preferences.getString("interval_pref", "1800000")));
        trayPreferences.put("ringtone", preferences.getString("ringtone", "content://settings/system/notification_sound"));
        trayPreferences.put("vibrate", preferences.getBoolean("vibrate", false));
        trayPreferences.put("led_light", preferences.getBoolean("led_light", false));

        LedColor color = LedColor.toEnum(preferences.getString("led_color", "white"));
        trayPreferences.put("led_color", color.getValue());

        // create service start intent
        Intent startIntent = new Intent(context, NotificationsService.class);

        // start notifications service when it's activated at Settings
        if (preferences.getBoolean("notifications_activated", false))
            context.startService(startIntent);
    }

}