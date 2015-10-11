package com.liukwarm.mapsample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by liukwarm on 10/10/15.
 */
public class SplashScreenActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
//                    new ConnectTask().execute(location);
        } else {
            offlineMode();
        }

        JSONObject user = connect();
//
//
        if (user == null) {
            Intent intent2 = new Intent(getApplicationContext(), LoginActivity.class);
            intent2.putExtra("id", Settings.Secure.getString(getApplicationContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID));
            startActivity(intent2);
        } else {
            try {
                Toast toast2 = Toast.makeText(getApplicationContext(), "Welcome, " + user.getString("username"), Toast.LENGTH_SHORT);
                toast2.show();
                Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                intent.putExtra("user", user.toString());
                startActivity(intent);
            } catch(JSONException e) {
                offlineMode();
            }

        }
//
//        offlineMode();

    }

    public void newUser() {
        Toast toast2 = Toast.makeText(getApplicationContext(), "New User", Toast.LENGTH_SHORT);
        toast2.show();
        Intent intent2 = new Intent(getApplicationContext(), LoginActivity.class);
        intent2.putExtra("id", Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID));
         startActivity(intent2);
    }


    public void offlineMode() {
        Toast toast2 = Toast.makeText(getApplicationContext(), "No Internet Access. Offline mode.", Toast.LENGTH_SHORT);
        toast2.show();
        Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
        intent.putExtra("user", (String) null);
        startActivity(intent);
    }

    public JSONObject connect() {
        LoginTask lt = new LoginTask();
        lt.execute(getApplicationContext());
        try {
            return lt.get();
        } catch (InterruptedException e) {}
        catch (ExecutionException e) {}

        return null;
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
}


