package com.liukwarm.mapsample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

public class NewRestroom extends Activity {

    private double lat, lng;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_restroom);
        Intent intent = getIntent();
        lat = intent.getExtras().getDouble("lat");
        lng = intent.getExtras().getDouble("lng");
        name = "Restroom";

        Button button = (Button) findViewById(R.id.send_new);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                newRestroomRequest();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_restroom, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void newRestroomRequest() {

        JSONObject send = new JSONObject();

        EditText et = (EditText) findViewById(R.id.name_text);
        name = et.getText().toString();

        try {
            send.put("lat", lat);
            send.put("lng", lng);
            send.put("name", name);
        } catch(JSONException e){}


        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new SendTask().execute(send);
        } else {
            Toast toast2 = Toast.makeText(getApplicationContext(), "failed. :(", Toast.LENGTH_SHORT);
            toast2.show();
        }
        finish();
    }

    private class SendTask extends AsyncTask<JSONObject, Void, Boolean> {
        protected Boolean doInBackground(JSONObject... urls) {
            try{
                String url="http://104.197.87.241/api/ratings";
                URL object=new URL(url);

                HttpURLConnection con = (HttpURLConnection) object.openConnection();
                con.setDoOutput(true);
                con.setDoInput(true);
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestMethod("POST");

                JSONObject send = urls[0];

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
}
