package org.indywidualni.fnotifier;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.net.URL;
import java.util.ArrayList;

import nl.matshofman.saxrssreader.RssFeed;
import nl.matshofman.saxrssreader.RssItem;
import nl.matshofman.saxrssreader.RssReader;

public class MainActivity extends ActionBarActivity {

    private SharedPreferences preferences;
    private SwipeRefreshLayout pullToRefresh;
    private ArrayList<RssItem> items;
    private ListView list;
    private String prevUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set a toolbar to replace the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        prevUrl = preferences.getString("feed_url", "");
        list = (ListView) findViewById(R.id.list);

        // start service if it's somehow stopped but activated
        if (preferences.getBoolean("notifications_activated", false)) {
            final Intent intent = new Intent(MyApplication.getContextOfApplication(), NotificationsService.class);
            MyApplication.getContextOfApplication().startService(intent);
        }

        // cancel notifications when 'All notifications' button was clicked
        try {
            if ("ALL_NOTIFICATIONS_ACTION".equals(getIntent().getAction()))
                NotificationsService.cancelAllNotifications();
        } catch (Exception ignored) {}

        // set empty view (nothing is loaded now)
        list.setEmptyView(findViewById(R.id.empty));
        list.setAdapter(null);

        // load items
        final String feedUrl = preferences.getString("feed_url", "");
        if (!"".equals(feedUrl))
            new RssReaderTask(MainActivity.this).execute(feedUrl);
        else {
            Toast.makeText(getApplicationContext(), getString(R.string.please_configure), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SettingsActivity.class));
        }

        // swipe refresh layout
        pullToRefresh = (SwipeRefreshLayout) findViewById(R.id.pullToRefresh);
        pullToRefresh.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pullToRefresh.setRefreshing(false);
                final String feedUrl = preferences.getString("feed_url", "");
                new RssReaderTask(MainActivity.this).execute(feedUrl);
            }
        });

        // on list item click listener
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    String url = items.get(position).getLink();
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) {}
            }
        });
    }

    // refresh button
    public void loadItems(MenuItem item) {
        final String feedUrl = preferences.getString("feed_url", "");
        new RssReaderTask(MainActivity.this).execute(feedUrl);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // cancel notifications when 'All notifications' button was clicked
        try {
            if ("ALL_NOTIFICATIONS_ACTION".equals(getIntent().getAction()))
                NotificationsService.cancelAllNotifications();
        } catch (Exception ignored) {}

        // load items when url was null before but it's ok now
        final String currentUrl = preferences.getString("feed_url", "");
        if ("".equals(prevUrl) && !"".equals(currentUrl))
            new RssReaderTask(MainActivity.this).execute(currentUrl);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.email_me:
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "koras@indywidualni.org", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                startActivity(Intent.createChooser(emailIntent, getString(R.string.choose_email_client)));
                return true;
            case R.id.google_play:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=Indywidualni")));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private class RssReaderTask extends AsyncTask<String, Void, ArrayList<RssItem>> {

        private static final int MAX_RETRY = 3;
        private ProgressDialog dialog;

        public RssReaderTask(Activity activity) {
            dialog = new ProgressDialog(activity);
        }

        @Override
        protected void onPreExecute() {
            this.dialog.setMessage(getString(R.string.loading));
            this.dialog.show();
        }

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
        protected void onPostExecute(final ArrayList<RssItem> result) {
            try {
                if (result.size() >= 1) {
                    populateItemsList(result);
                    items = result;
                }
            } catch (Exception ex) {
                if ("".equals(preferences.getString("feed_url", "")))
                    Toast.makeText(getApplicationContext(), getString(R.string.please_configure), Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getApplicationContext(), getString(R.string.loading_error), Toast.LENGTH_SHORT).show();
            }

            if (dialog.isShowing())
                dialog.dismiss();
        }

    }

    private void populateItemsList(ArrayList<RssItem> items) {
        ItemsArrayAdapter adapter = new ItemsArrayAdapter(this, items);
        list.setAdapter(adapter);
    }

}