package com.liukwarm.mapsample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private final LatLngBounds UCBSTART = new LatLngBounds(new LatLng(37, -110), new LatLng(38, -125));
    private final LatLng UCBCENTER = new LatLng(37.8715, -122.259);
    private final CameraPosition UCBPOSITION = new CameraPosition.Builder().target(UCBCENTER).bearing(84).tilt(1).zoom(15).build();
    private HashSet<JSONObject> restRooms;
    private ClusterManager<CustomMarker> mClusterManager;
    private Location location;
    private GoogleApiClient mGoogleApiClient;
    private LocationListener locationListener;
    private boolean offlineMode;
    private JSONObject user;
    private HashMap<String, JSONObject> visited;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        String id = getIntent().getExtras().getString("user");
        visited = new HashMap<String, JSONObject>();
        if (id == null) {
            offlineMode = true;
            user = null;
        } else {
            offlineMode = false;
            try {
                user = new JSONObject(id);
                if (user != null) {
                    JSONArray userRatings = new JSONArray(user.get("ratings"));
                    for (int i = 0; i < userRatings.length(); i++) {
                        JSONObject rating = userRatings.getJSONObject(i);
                        visited.put(rating.getString("_id"), rating);
                    }
                }
            } catch (JSONException e) {}

        }

        restRooms = new HashSet<JSONObject>();

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
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

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

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        Button button = (Button) findViewById(R.id.new_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                newRestroom(v);
            }
        });

    }

    private void update() {
        GetTask getTask = new GetTask();
        getTask.execute();

        try {
            restRooms = (HashSet<JSONObject>) getTask.get();
        } catch (InterruptedException e) {}
        catch (ExecutionException e) {}

        if (restRooms != null) {
            for (JSONObject rr : restRooms) {
                try {
                    mClusterManager.addItem(new CustomMarker((Double) rr.get("lat"), (Double) rr.get("lng"), (String) rr.get("_id")));
                } catch (JSONException e) {
                }
            }
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "Cannot connect to server.", Toast.LENGTH_SHORT);
            toast.show();
        }
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
                Toast toast = Toast.makeText(getApplicationContext(), "new rating", Toast.LENGTH_SHORT);
                toast.show();
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
        if (visited.containsKey(item.getID())) {
            intent.putExtra("existing", visited.get(item.getID()).toString());
        } else {
            intent.putExtra("existing", (String) null);
        }
        startActivity(intent);
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

        public CustomMarker(double lat, double lng, String id) {
            mPosition = new LatLng(lat, lng);
            this.id = id;
        }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }

        public boolean equals(CustomMarker other) {
            return other.getID().equals(this.id);
        }

        public String getID() {
            return this.id;
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
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        Criteria crta = new Criteria();
//        crta.setAccuracy(Criteria.ACCURACY_FINE);
//        crta.setAltitudeRequired(true);
//        crta.setBearingRequired(true);
//        crta.setCostAllowed(true);
//        crta.setPowerRequirement(Criteria.POWER_LOW);
//        String provider = locationManager.getBestProvider(crta, true);
//        Toast toast = Toast.makeText(getApplicationContext(), "Provider: " + provider, Toast.LENGTH_SHORT);
//        toast.show();

//        String provider = LocationManager.GPS_PROVIDER;
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        Intent intent = new Intent(this, NewRestroom.class);
        intent.putExtra("lat", location.getLatitude());
        intent.putExtra("lng", location.getLongitude());
        intent.putExtra("user", user.toString());

//        Toast toast = Toast.makeText(getApplicationContext(), "Current: " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT);
//        toast.show();

//        intent.putExtra("lat", 0.0);
//        intent.putExtra("lng", 0.0);

        startActivity(intent);
    }

}
