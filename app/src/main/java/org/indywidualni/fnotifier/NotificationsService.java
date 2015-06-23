package org.indywidualni.fnotifier;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.net.URL;
import java.util.ArrayList;

import nl.matshofman.saxrssreader.RssFeed;
import nl.matshofman.saxrssreader.RssItem;
import nl.matshofman.saxrssreader.RssReader;

public class NotificationsService extends Service {

    private Handler handler = null;
    private static Runnable runnable = null;
    private String feedUrl;
    private int timeInterval;
    private SharedPreferences preferences;
    private int itemCounter;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i("NotificationsService", "********** Service created! **********");

        // get shared preferences
        preferences = getSharedPreferences(getApplicationContext().getPackageName() + "_preferences", Context.MODE_MULTI_PROCESS);

        handler = new Handler();
        runnable = new Runnable() {
            public void run() {
                // get the url and time interval from shared prefs
                feedUrl = preferences.getString("feed_url", "");
                timeInterval = Integer.parseInt(preferences.getString("interval_pref", "1800000"));
                Log.i("NotificationsService", "Feed URL: " + feedUrl);
                Log.i("NotificationsService", "Time interval: " + timeInterval + " ms");

                new RssReaderTask().execute(feedUrl);

                // set repeat time interval
                handler.postDelayed(runnable, timeInterval);
            }
        };

        // first run delay (3 seconds)
        handler.postDelayed(runnable, 3000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i("NotificationsService", "********** Service stopped **********");
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // AsyncTask to get feed, process it and do all the actions needed later
    private class RssReaderTask extends AsyncTask<String, Void, ArrayList<RssItem>> {

        // max number of tries when something is wrong
        private static final int MAX_RETRY = 3;

        @Override
        protected ArrayList<RssItem> doInBackground(String... params) {

            ArrayList<RssItem> result = null;
            int tries = 0;

            while(tries++ < MAX_RETRY && result == null) {
                try {
                    Log.i("RssReaderTask", "********** doInBackground: Processing... Trial: " + tries);
                    URL url = new URL(params[0]);
                    RssFeed feed = RssReader.read(url);
                    result = feed.getRssItems();
                } catch (Exception ex) {
                    Log.i("RssReaderTask", "********** doInBackground: Feed error!");
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(ArrayList<RssItem> result) {

            /** The first service start ever will display a fake notification.
             *  Not fake actually - the latest one. I've been thinking instead
             *  of avoiding it, it's a nice example how it will work in the future.
             */

            // get the last PubDate (String) from shared prefs
            final String savedDate = preferences.getString("saved_date", "nothing");

            // if the saved PubDate is different than the new one it means there is new notification
            try {
                if (!result.get(0).getPubDate().toString().equals(savedDate))
                        notifier(result.get(0).getTitle(), result.get(0).getDescription(), result.get(0).getLink());

                // save the latest PubDate to shared prefs
                preferences.edit().putString("saved_date", result.get(0).getPubDate().toString()).apply();

                // log success
                Log.i("RssReaderTask", "********** onPostExecute: Aight biatch ;)");
            } catch (NullPointerException ex) {
                Log.i("RssReaderTask", "********** onPostExecute: NullPointerException!");
            }
        }

    }

    private void notifier(String title, String summary, String url) {
        Log.i("NotificationsService", "notifier: Start notification");

        // increment counter and create notification title
        String contentTitle;
        if (++itemCounter == 1)
            contentTitle = getString(R.string.new_item);
        else
            contentTitle = itemCounter + " " + getString(R.string.new_items);

        // start building a notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(title))
                        .setSmallIcon(R.drawable.ic_stat_f)
                        .setContentTitle(contentTitle)
                        .setContentText(title)
                        .setTicker(title)
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true);

        // if more than 1 notification set counter
        if (itemCounter > 1)
            mBuilder.setNumber(itemCounter);

        // see all the notifications button
        Intent allNotificationsIntent = new Intent(this, MainActivity.class);
        allNotificationsIntent.setAction("ALL_NOTIFICATIONS_ACTION");
        PendingIntent piAllNotifications = PendingIntent.getActivity(getApplicationContext(), 0, allNotificationsIntent, 0);
        mBuilder.addAction(0, getString(R.string.all_notifications), piAllNotifications);

        // notification sound
        Uri ringtoneUri = Uri.parse(preferences.getString("ringtone", "content://settings/system/notification_sound"));
        mBuilder.setSound(ringtoneUri);

        // vibration
        if (preferences.getBoolean("vibrate", false))
            mBuilder.setVibrate(new long[] {500, 500});

        // LED light
        if (preferences.getBoolean("led_light", false)) {
            //noinspection ConstantConditions
            switch (preferences.getString("led_color", "white")) {
                case "white":
                    mBuilder.setLights(Color.WHITE, 1, 1);
                    break;
                case "red":
                    mBuilder.setLights(Color.RED, 1, 1);
                    break;
                case "green":
                    mBuilder.setLights(Color.GREEN, 1, 1);
                    break;
                case "blue":
                    mBuilder.setLights(Color.BLUE, 1, 1);
                    break;
                case "cyan":
                    mBuilder.setLights(Color.CYAN, 1, 1);
                    break;
                case "magenta":
                    mBuilder.setLights(Color.MAGENTA, 1, 1);
                    break;
            }
        }

        // priority for Heads-up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            mBuilder.setPriority(Notification.PRIORITY_HIGH);

        // intent with notification url in extra
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

        // final notification building
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setOngoing(false);
        Notification note = mBuilder.build();

        // display a notification
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, note);
    }

    public static void cancelAllNotifications() {
        NotificationManager notificationManager = (NotificationManager)
                MyApplication.getContextOfApplication().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

}