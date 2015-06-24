package org.indywidualni.fnotifier;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;

import nl.matshofman.saxrssreader.RssItem;

public class ItemsArrayAdapter extends ArrayAdapter<RssItem> {

    public ItemsArrayAdapter(Context context, ArrayList<RssItem> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RssItem item = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);

        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView date = (TextView) convertView.findViewById(R.id.date);
        TextView id = (TextView) convertView.findViewById(R.id.id);

        title.setText(item.getTitle());
        try {
            date.setText(DateFormat.getDateTimeInstance().format(item.getPubDate()));
        } catch (Exception ex) {
            date.setText(getContext().getString(R.string.date_null));
        }
        id.setText("#" + String.format("%02d", position + 1));

        return convertView;
    }

}