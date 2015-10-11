package com.liukwarm.mapsample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by liukwarm on 10/11/15.
 */
public class RealSplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash_screen);

        Thread welcomeThread = new Thread() {

            @Override
            public void run() {
                try {
                    super.run();
                    sleep(2000);
                } catch (Exception e) {

                } finally {

                    Intent categoryPageIntent = new Intent(getApplicationContext(), SplashScreenActivity.class);
                    finish();
                    startActivity(categoryPageIntent);
                }
            }
        };
        welcomeThread.start();
    }
}
