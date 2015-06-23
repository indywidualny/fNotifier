package org.indywidualni.fnotifier;

import android.annotation.SuppressLint;
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

public class SettingsFragment extends PreferenceFragment {

    private static Context context;
    private SharedPreferences preferences;
    private SharedPreferences.OnSharedPreferenceChangeListener myPrefListner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // set context
        context = MyApplication.getContextOfApplication();

        // get shared preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // listener for changing preferences (works after the value change)
        myPrefListner = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @SuppressLint("CommitPrefEdits")
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                // service intent (start, stop)
                final Intent intent = new Intent(context, NotificationsService.class);

                switch (key) {
                    case "notifications_activated":
                        if (prefs.getBoolean("notifications_activated", false))
                            context.startService(intent);
                        else
                            context.stopService(intent);
                        break;
                    case "interval_pref":
                        // restart service after time interval change
                        if (prefs.getBoolean("notifications_activated", false)) {
                            context.stopService(intent);
                            context.startService(intent);
                        }
                        break;
                    case "feed_url":
                        // remove saved date to fresh check
                        preferences.edit().putString("saved_date", "").commit();
                        // restart service
                        if (prefs.getBoolean("notifications_activated", false)) {
                            context.stopService(intent);
                            context.startService(intent);
                        }
                        break;
                }

            }
        };

        // register the listener above
        preferences.registerOnSharedPreferenceChangeListener(myPrefListner);

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
    public void onResume() {
        super.onResume();

        // update ringtone preference summary
        String ringtoneString = preferences.getString("ringtone", "content://settings/system/notification_sound");
        Uri ringtoneUri = Uri.parse(ringtoneString);
        Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
        String name = ringtone.getTitle(context);
        if ("".equals(ringtoneString))
            name = getString(R.string.silent);
        RingtonePreference rp = (RingtonePreference) findPreference("ringtone");
        rp.setSummary(getString(R.string.notification_sound_description) + name);
    }

}