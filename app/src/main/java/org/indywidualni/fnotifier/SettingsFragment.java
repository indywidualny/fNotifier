package org.indywidualni.fnotifier;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.util.Log;

import net.grandcentrix.tray.TrayAppPreferences;

public class SettingsFragment extends PreferenceFragment {

    private static Context context;
    private TrayAppPreferences trayPreferences;
    private SharedPreferences preferences;
    private SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        context = MyApplication.getContextOfApplication();
        trayPreferences = new TrayAppPreferences(context);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // listener for changing preferences (works after the value change)
        prefChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                // service intent (start, stop)
                final Intent intent = new Intent(context, NotificationsService.class);

                switch (key) {
                    case "notifications_activated":
                        // turn on or turn off the service
                        if (prefs.getBoolean("notifications_activated", false))
                            context.startService(intent);
                        else
                            context.stopService(intent);
                        break;
                    case "interval_pref":
                        // update Tray Preference before restarting the service
                        trayPreferences.put("interval_pref", Integer.parseInt(preferences.getString("interval_pref", "1800000")));
                        // restart the service after time interval change
                        if (prefs.getBoolean("notifications_activated", false)) {
                            context.stopService(intent);
                            context.startService(intent);
                        }
                        break;
                    case "feed_url":
                        trayPreferences.put("feed_url", preferences.getString("feed_url", ""));
                        // remove saved date for fresh check
                        trayPreferences.put("saved_date", "");
                        // restart service
                        if (prefs.getBoolean("notifications_activated", false)) {
                            context.stopService(intent);
                            context.startService(intent);
                        }
                        break;
                    case "ringtone":
                        trayPreferences.put("ringtone", preferences.getString("ringtone", "content://settings/system/notification_sound"));
                        break;
                    case "vibrate":
                        trayPreferences.put("vibrate", preferences.getBoolean("vibrate", false));
                        break;
                    case "led_light":
                        trayPreferences.put("led_light", preferences.getBoolean("led_light", false));
                        break;
                    case "led_color":
                        LedColor color = LedColor.toEnum(preferences.getString("led_color", "white"));
                        trayPreferences.put("led_color", color.getValue());
                        break;
                }

                // what's going on, dude?
                Log.v("SharedPreferenceChange", key + " changed in SettingsFragment");
            }
        };


        // listener for get_feed preference
        findPreference("get_facebook_feed").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.v("SettingsFragment", "get_facebook_feed clicked");
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.get_facebook_feed_link))));
                return true;
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        // register the listener
        preferences.registerOnSharedPreferenceChangeListener(prefChangeListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        // unregister the listener
        preferences.unregisterOnSharedPreferenceChangeListener(prefChangeListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        // update ringtone preference summary
        String ringtoneString = preferences.getString("ringtone", "content://settings/system/notification_sound");
        Uri ringtoneUri = Uri.parse(ringtoneString);
        String name;

        try {
            Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
            name = ringtone.getTitle(context);
        } catch (Exception ex) {
            ex.printStackTrace();
            name = "Default";
        }

        if ("".equals(ringtoneString))
            name = getString(R.string.silent);

        RingtonePreference rp = (RingtonePreference) findPreference("ringtone");
        rp.setSummary(getString(R.string.notification_sound_description) + name);
    }

}