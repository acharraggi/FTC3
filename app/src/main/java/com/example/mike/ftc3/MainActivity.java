package com.example.mike.ftc3;

import android.content.Intent;
//import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class MainActivity extends AppCompatActivity {

    /** Called when the user clicks the wifi scan button */
    public void displayWifiScan(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, DisplayWifiScan.class);
        startActivity(intent);
    }

    /** Called when the user clicks the new wifi scan button */
    public void displayWifiScan2(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, DisplayWifiScan2.class);
        startActivity(intent);
    }

    /** Called when the user clicks the channel scan button */
    public void displayChannelScan(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, DisplayChannelScan.class);
        startActivity(intent);
    }

    /** Called when the user clicks the channel scan2 button */
    public void displayChannelScan2(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, DisplayChannelScan2.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
}
