package com.liukwarm.mapsample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private final LatLngBounds UCBSTART = new LatLngBounds(new LatLng(37, -110), new LatLng(38, -125));
    private final LatLng UCBCENTER = new LatLng(37.8715, -122.259);
    private final CameraPosition UCBPOSITION = new CameraPosition.Builder().target(UCBCENTER).bearing(84).tilt(1).zoom(15).build();
    private HashMap<String, CustomMarker> restRooms;
    private ClusterManager<CustomMarker> mClusterManager;
    private Location location;
    private GoogleApiClient mGoogleApiClient;
    private LocationListener locationListener;
    private boolean offlineMode;
    private JSONObject user;
    private HashMap<String, JSONObject> visited;
    private boolean userUpdate = false;
    private LocationManager lm;
    private ArrayList<NavDrawerItem> list;

    private int zero, half, one, one_half, two, two_half, three, three_half, four, four_half, five;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        TypedArray stars = getResources().obtainTypedArray(R.array.stars);
        zero = stars.getResourceId(0, -1);
        half = stars.getResourceId(1, -1);
        one = stars.getResourceId(2, -1);
        one_half = stars.getResourceId(3, -1);
        two = stars.getResourceId(4, -1);
        two_half = stars.getResourceId(5, -1);
        three = stars.getResourceId(6, -1);
        three_half = stars.getResourceId(7, -1);
        four = stars.getResourceId(8, -1);
        four_half = stars.getResourceId(9, -1);
        five = stars.getResourceId(10, -1);

        String id = getIntent().getExtras().getString("user");
        visited = new HashMap<String, JSONObject>();
        if (id == null) {
            offlineMode = true;
            user = null;
        } else {
            offlineMode = false;
            try {
                user = new JSONObject(id);
                userUpdate = true;
            } catch (JSONException e) {}

        }

        restRooms = new HashMap<String, CustomMarker>();

        setUpClusterer();

        update();


        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {

            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                CameraPosition newP = cameraPosition;

//                LatLng temp = new LatLng(cameraPosition.target.latitude, cameraPosition.target.longitude);
//                if (!UCBSTART.contains(temp)) {
//                    Log.d("CameraChange", "outside");
//                } else {
//                    Log.d("CameraChange", "inside");
//                }

                mClusterManager.onCameraChange(newP);
            }
        });

        // Acquire a reference to the system Location Manager
        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
//                Toast toast = Toast.makeText(getApplicationContext(), "Updated to " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT);
//                toast.show();
                ConnectivityManager connMgr = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    update();
                } else {
                    Toast toast2 = Toast.makeText(getApplicationContext(), "Offline Mode", Toast.LENGTH_SHORT);
                    toast2.show();
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        android.support.design.widget.FloatingActionButton button = (android.support.design.widget.FloatingActionButton) findViewById(R.id.new_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                newRestroom(v);
            }
        });

        list = new ArrayList<NavDrawerItem>();
        setList();


    }

    private void setList() {
        TreeMap<Double, JSONObject> tm = new TreeMap<Double, JSONObject>();

        Location location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        for (String s : restRooms.keySet()) {
            JSONObject rr = restRooms.get(s).getRR();
            double distance = -1;
            try {
                Location l2 = new Location("");
                l2.setLatitude(rr.getDouble("lat"));
                l2.setLongitude(rr.getDouble("lng"));
                distance = location.distanceTo(l2);
            } catch (JSONException e) {}
            if (distance != -1 && distance < 1600)
                tm.put(distance, rr);
        }

        TypedArray numbers = getResources()
                .obtainTypedArray(R.array.numbers);
        ArrayList<NavDrawerItem> list = new ArrayList<NavDrawerItem>();

        NumberFormat formatter = new DecimalFormat("#0.0");
        for (int i = 0; i < 6 && !tm.isEmpty(); i++) {
            double distance = tm.firstKey();
            JSONObject rr = tm.remove(tm.firstKey());
            try {
                int chosen = zero;
                double rating = rr.getDouble("score");
                if (rating >= 4.9) chosen = five;
                else if (rating >= 4.5) chosen = four_half;
                else if (rating >= 4.0) chosen = four;
                else if (rating >= 3.5) chosen = three_half;
                else if (rating >= 3.0) chosen = three;
                else if (rating >= 2.5) chosen = two_half;
                else if (rating >= 2.0) chosen = two;
                else if (rating >= 1.5) chosen = one_half;
                else if (rating >= 1) chosen = one;
                else if (rating >= .5) chosen = half;
                else if (rating >= 0) chosen = zero;

                list.add(new NavDrawerItem(numbers.getResourceId(i, -1), rr.getString("name"), chosen, formatter.format(distance), numbers.getResourceId(i, -1), rr));
            } catch (JSONException e) {
            }
        }

        this.list = list;

        ListView lv = (ListView) findViewById(R.id.listview);
//        Log.i("myTag", lv.toString());
//        for(int i = 0; i < lv.)
        lv.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        NavDrawerListAdapter adapter = new NavDrawerListAdapter(getApplicationContext(),
                list);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new ItemClickListener());

        mMap.setMyLocationEnabled(true);
    }

    private GoogleMap.OnMyLocationChangeListener myLocationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
        @Override
        public void onMyLocationChange(Location location) {
            LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.addMarker(new MarkerOptions().position(loc));
            if(mMap != null){
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 16.0f));
            }
        }
    };

    private class ItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {

            selectView(position);
        }
    }

    private void selectView(int position) {
        JSONObject selected = list.get(position).obj;
        try {
            newRating(restRooms.get(selected.getString("_id")));
        } catch (JSONException e) {}
    }

    private class LoginTask extends AsyncTask<Context, Void, JSONObject> {
        protected JSONObject doInBackground(Context... urls) {
            try {
                String url="http://104.197.87.241/api/login/" + Settings.Secure.getString(urls[0].getContentResolver(),
                        Settings.Secure.ANDROID_ID);
                URL object=new URL(url);

                HttpURLConnection con = (HttpURLConnection) object.openConnection();


                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

                StringBuilder total = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    total.append(line);
                }
                JSONObject returned = new JSONObject();
                try {
                    returned = new JSONObject(total.toString());
                } catch (JSONException e) {}
                return returned;
            } catch (IOException e){}
            return null;
        }

        protected void onPostExecute(Long result) {

        }
    }

    private void update() {


        GetTask getTask = new GetTask();
        getTask.execute();
        HashSet<JSONObject> newRestRooms = new HashSet<JSONObject>();
        if (userUpdate == true) {
            LoginTask lg = new LoginTask();
            lg.execute(getApplicationContext());
            try {
                if (lg.get() != null) {
                    user = lg.get();
                    if (user != null) {
                        try {
                            JSONArray userRatings = new JSONArray(user.get("ratings").toString());

                            for (int i = 0; i < userRatings.length(); i++) {
                                JSONObject rating = userRatings.getJSONObject(i);

                                visited.put(rating.getString("restroom"), rating);

                            }
                        } catch (JSONException e) {

                        }
                    }
                }
            } catch (InterruptedException e) {}
            catch (ExecutionException e) {}
        }
        userUpdate = false;
        try {
            newRestRooms = (HashSet<JSONObject>) getTask.get();
        } catch (InterruptedException e) {}
        catch (ExecutionException e) {}

        try{
            if (newRestRooms != null) {
                for (JSONObject rr : newRestRooms) {
                    if (!restRooms.containsKey(rr.getString("_id"))) {
                        try {


                            CustomMarker newMark = new CustomMarker((Double) rr.get("lat"), (Double) rr.get("lng"), (String) rr.get("_id"), rr.getString("name"), rr);
                            mClusterManager.addItem(newMark);
                            restRooms.put(rr.getString("_id"), newMark);
                        } catch (JSONException e) {
                        }
                    }
                }

            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "Cannot connect to server.", Toast.LENGTH_SHORT);
                toast.show();
            }
        } catch (JSONException e) {}
    }



    private void setUpClusterer() {
        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<CustomMarker>(this, mMap);

        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mMap.setOnCameraChangeListener(mClusterManager);
        mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<CustomMarker>() {
            @Override
            public boolean onClusterItemClick(CustomMarker item) {

                newRating(item);
                return true;
            }
        });

        mMap.setOnMarkerClickListener(mClusterManager);
    }

    private void newRating(CustomMarker item) {
        Intent intent = new Intent(this, RatingActivity.class);
        intent.putExtra("user", user.toString());
        intent.putExtra("restID", item.getID());
        intent.putExtra("name", item.getName());
        if (visited.containsKey(item.getID())) {
            intent.putExtra("existing", visited.get(item.getID()).toString());
        } else {

            intent.putExtra("existing", (String) null);
        }
        startActivity(intent);
        userUpdate = true;
        update();
    }


    private class GetTask extends AsyncTask<Location, Void, Set<JSONObject>> {
        protected Set<JSONObject> doInBackground(Location... urls) {
            try {
                String url="http://104.197.87.241/api/restrooms";
                URL object=new URL(url);

                HttpURLConnection con = (HttpURLConnection) object.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

                StringBuilder total = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    total.append(line);
                }
                JSONArray returned = new JSONArray();
                try {
                    returned = new JSONArray(total.toString());
                } catch (JSONException e) {}
                HashSet<JSONObject> toReturn = new HashSet<JSONObject>();
                for (int i = 0; i < returned.length(); i++) {
                    try {
                        toReturn.add(returned.getJSONObject(i));
                    } catch (JSONException e) {}
                }
                return toReturn;
            } catch (IOException e){}
            return null;
        }

        protected void onPostExecute(Long result) {

        }
    }

    public class CustomMarker implements ClusterItem {
        private final LatLng mPosition;
        private final String id;
        private final String name;
        private JSONObject rr;

        public CustomMarker(double lat, double lng, String id, String name, JSONObject rr) {
            mPosition = new LatLng(lat, lng);
            this.id = id;
            this.name = name;
            this.rr = rr;
        }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }
        public JSONObject getRR() {
            return rr;
        }


        public boolean equals(CustomMarker other) {
            return other.getID().equals(this.id);
        }

        public String getID() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }
    }

    private class ConnectTask extends AsyncTask<Location, Void, Boolean> {
        protected Boolean doInBackground(Location... urls) {
            try {
                String url="http://104.197.87.241/api/restrooms";
                URL object=new URL(url);

                HttpURLConnection con = (HttpURLConnection) object.openConnection();
                con.setDoOutput(true);
                con.setDoInput(true);
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestMethod("POST");

                JSONObject send = new JSONObject();
                try {
                    send.put("lat", urls[0].getLatitude() + Math.random() * .1 - .05);
                    send.put("lng", urls[0].getLongitude() + Math.random() * .1 - .05);
                    send.put("name", "potato");
                } catch(JSONException e){}

                Log.d("MyApp", send.toString());

                OutputStream os = con.getOutputStream();
                os.write(send.toString().getBytes("UTF-8"));
                os.close();

                OutputStreamWriter wr= new OutputStreamWriter(con.getOutputStream());
                wr.write(send.toString());
                con.getResponseCode();
            } catch (IOException e){}
            return true;
        }


        protected void onPostExecute(Long result) {

        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        userUpdate = true;
        update();
        setList();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();


            // Check if we were successful in obtaining the map.
            if (mMap != null) {

                setUpMap();
            }
        }
    }



    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(UCBPOSITION));
    }

    private void newRestroom(View view) {
//        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
////        Criteria crta = new Criteria();
////        crta.setAccuracy(Criteria.ACCURACY_FINE);
////        crta.setAltitudeRequired(true);
////        crta.setBearingRequired(true);
////        crta.setCostAllowed(true);
////        crta.setPowerRequirement(Criteria.POWER_LOW);
////        String provider = locationManager.getBestProvider(crta, true);
////        Toast toast = Toast.makeText(getApplicationContext(), "Provider: " + provider, Toast.LENGTH_SHORT);
////        toast.show();
//
////        String provider = LocationManager.GPS_PROVIDER;
//        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
        Location location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        Intent intent = new Intent(this, NewRestroom.class);
        intent.putExtra("lat", location.getLatitude());
        intent.putExtra("lng", location.getLongitude());
        intent.putExtra("user", user.toString());

//        Toast toast = Toast.makeText(getApplicationContext(), "Current: " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT);
//        toast.show();

//        intent.putExtra("lat", 0.0);
//        intent.putExtra("lng", 0.0);

        startActivity(intent);
        userUpdate = true;
        update();
    }

}
