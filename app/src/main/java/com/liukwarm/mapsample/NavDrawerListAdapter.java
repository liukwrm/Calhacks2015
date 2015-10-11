package com.liukwarm.mapsample;

/**
 * Created by liukwarm on 10/10/15.
 */
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;


import java.util.ArrayList;

public class NavDrawerListAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<NavDrawerItem> navDrawerItems;

    public NavDrawerListAdapter(Context context, ArrayList<NavDrawerItem> navDrawerItems){
        this.context = context;
        this.navDrawerItems = navDrawerItems;
    }

    @Override
    public int getCount() {
        return navDrawerItems.size();
    }

    @Override
    public Object getItem(int position) {
        return navDrawerItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.rr_list_item, parent, false);
        }

        ImageView number = (ImageView) convertView.findViewById(R.id.number);
        TextView name = (TextView) convertView.findViewById(R.id.name);
        ImageView rb = (ImageView) convertView.findViewById(R.id.ratingBar);
        TextView distance = (TextView) convertView.findViewById(R.id.distance);
        ImageButton go = (ImageButton) convertView.findViewById(R.id.go);

        number.setImageResource(navDrawerItems.get(position).number);
        name.setText(navDrawerItems.get(position).name);
        rb.setImageResource(navDrawerItems.get(position).number);
        distance.setText(navDrawerItems.get(position).distance);
        go.setImageResource(navDrawerItems.get(position).go);

        return convertView;
    }

}