package com.liukwarm.mapsample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by liukwarm on 10/10/15.
 */
public class RatingActivity extends Activity {

    private JSONObject user;
    private double rating;
    private String comment;
    private String restID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_rating);
        Intent intent = getIntent();
        String id = getIntent().getExtras().getString("user");
        this.restID = getIntent().getExtras().getString("restID");

        if (this.restID == null) {
            finish();
        }
        if (id == null) {
            user = null;
            finish();
        } else {
            try {
                user = new JSONObject(id);
            } catch (JSONException e) {}
        }

        Button button = (Button) findViewById(R.id.send_new);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                newRatingRequest();
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

    public boolean newRatingRequest() {

        JSONObject send = new JSONObject();

        EditText et = (EditText) findViewById(R.id.comment);
        comment = et.getText().toString();

        RatingBar rb = (RatingBar) findViewById(R.id.rating);
        rating = rb.getRating();

        Toast toast = Toast.makeText(getApplicationContext(), "Rating " + rating, Toast.LENGTH_SHORT);
        toast.show();


        try {
            send.put("score", rating);
            send.put("user", user.get("_id"));
            send.put("restroom", restID);
            send.put("description", comment);

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
        return true;
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
