package com.liukwarm.mapsample;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by liukwarm on 10/10/15.
 */
public class LoginActivity extends Activity {


    private String id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_screen);

        this.id = getIntent().getExtras().getString("id");
//
//        Toast toast2 = Toast.makeText(getApplicationContext(), "Welcome, new user", Toast.LENGTH_SHORT);
//        toast2.show();
//
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                submit(v);
            }
        });
    }


    private void submit(View v) {
        EditText et = (EditText) findViewById(R.id.new_login);
        String login = et.getText().toString();
        LoginTask lt = new LoginTask();
        lt.execute(login, id);
        JSONObject user = null;
        try {
            user = lt.get();
        } catch (InterruptedException e) {offlineMode ();}
        catch (ExecutionException e) {offlineMode();}
        if (user == null) {
            offlineMode();
        }
        try {
            Toast toast2 = Toast.makeText(getApplicationContext(), "Welcome, " + user.getString("username"), Toast.LENGTH_SHORT);
            toast2.show();
            Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
            intent.putExtra("user", user.toString());
            startActivity(intent);
        } catch (NullPointerException e) {
            offlineMode();
        } catch(JSONException e) {
            offlineMode();
        }

    }
    public void offlineMode() {
        Toast toast2 = Toast.makeText(getApplicationContext(), "No Internet Access. Offline mode.", Toast.LENGTH_SHORT);
        toast2.show();
        Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
        intent.putExtra("user", (String) null);
        startActivity(intent);
    }


    private class LoginTask extends AsyncTask<String, String, JSONObject> {
        protected JSONObject doInBackground(String... urls) {
            try {
                String url="http://104.197.87.241/api/signup";
                URL object=new URL(url);

                HttpURLConnection con = (HttpURLConnection) object.openConnection();
                con.setDoOutput(true);
                con.setDoInput(true);
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestMethod("POST");

                JSONObject send = new JSONObject();
                try {
                    send.put("device_id", urls[1]);
                    send.put("username", urls[0]);
                } catch(JSONException e){}

                Log.d("MyApp", send.toString());

                OutputStream os = con.getOutputStream();
                os.write(send.toString().getBytes("UTF-8"));
                os.close();

                OutputStreamWriter wr= new OutputStreamWriter(con.getOutputStream());
                wr.write(send.toString());
                con.getResponseCode();

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


