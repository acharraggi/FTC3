package com.example.mike.FTCScanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TableRow.LayoutParams;
import android.widget.Toast;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class DisplayWifiScan2 extends AppCompatActivity {
    private WifiManager wifi;
    private int size = 0;
    private List<ScanResult> results;
    private boolean startDisplay = false;

    // preference variables
    private volatile Boolean excludeWeakWifi = true;
    private volatile Boolean excludeRobots = true;
    private SharedPreferences sharedPref;
    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs,
                                                      String key) {
                    Log.d("DisplayWifiScan2", "myReceiver called, key="+key);
                    if (key.equals("exclude_weak_wifi")) {
                        boolean newValue=prefs.getBoolean("exclude_weak_wifi", false);
                        if(newValue != excludeWeakWifi) {
                            excludeWeakWifi = newValue;
                            wifiScan();  //rescan
                        }
                    }
                    if (key.equals("exclude_robots")) {
                        boolean newValue=prefs.getBoolean("exclude_robots", false);
                        if(newValue != excludeRobots) {
                            excludeRobots = newValue;
                            wifiScan();  //rescan
                        }
                    }
                }
            };

    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            Log.d("DisplayWifiScan2", "myReceiver called");
            // note: this is called initially, then occasionally as networks change
            if(!startDisplay) { // but just in case, don't call displayScan if I'm still displaying the last scan
                startDisplay = true;
                results = wifi.getScanResults();
                Collections.sort(results, new Comparator<ScanResult>() {
                    @Override
                    public int compare(final ScanResult object1, final ScanResult object2) {
                        // sort ascending channel#, descending strength
                        return ((object2.frequency * 1000 - object2.level) - (object1.frequency * 1000 - object1.level));
                    }
                });
                size = results.size();
                tl.removeAllViews();    // clear table
                addHeaders();           // add table headers
                displayScan();          // add table rows
            }
        }
    };

    private TableLayout tl;
    private TableRow tr;
    private TableRow trDivider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("DisplayWifiScan2", "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_wifi_scan2);
        tl = (TableLayout) findViewById(R.id.maintable);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        excludeWeakWifi = sharedPref.getBoolean("exclude_weak_wifi", false);
        excludeRobots = sharedPref.getBoolean("exclude_robots", false);
        sharedPref.registerOnSharedPreferenceChangeListener(prefListener);
        Log.d("DisplayWifiScan2","excludeWeakWifi = "+excludeWeakWifi);
        Log.d("DisplayWifiScan2","excludeRobots = "+excludeRobots);

        addHeaders();

        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled())
        {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }
        registerReceiver(myReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        // start with initial scan
        wifiScan();
    }

    private static int convertFrequencyToChannel(int freq) {
        if (freq >= 2412 && freq <= 2484) {
            return (freq - 2412) / 5 + 1;
        } else if (freq >= 5170 && freq <= 5825) {
            return (freq - 5170) / 5 + 34;
        } else {
            return -1;
        }
    }

    private void wifiScan()
    {
        wifi.startScan();
    }

    private void displayScan()
    {
        Log.d("DisplayWifiScan2", "displayScan called");
        Toast.makeText(this, "Wifi network(s) found = " + size, Toast.LENGTH_SHORT).show();
        TextView SSID_TV, channelTV, strengthTV;

        size = size - 1;
        while (size >= 0)
        {
            int wifiStrength = WifiManager.calculateSignalLevel(results.get(size).level, 5);
            String ssid_name = results.get(size).SSID.isEmpty() ? results.get(size).BSSID : results.get(size).SSID;
            Log.d("displayScan","ssid_name = "+ssid_name);

            if( (!excludeWeakWifi || wifiStrength>0)   &&             // if exclude weak wifi is on and signle is weak, this expresss is false
                  (!excludeRobots || !ssid_name.toUpperCase().contains("SHAW") )  //
                    ) {
                /** Create a TableRow dynamically **/
                tr = new TableRow(this);

                 /** Creating a TextView to add to the row **/
                SSID_TV = new TextView(this);
                SSID_TV.setText(ssid_name);
                SSID_TV.setTextColor(Color.BLACK);
                SSID_TV.setTextSize(getResources().getDimension(R.dimen.myFontSize));
                SSID_TV.setEllipsize(TextUtils.TruncateAt.END); //doesn't seem to work
                SSID_TV.setTypeface(Typeface.DEFAULT);
                SSID_TV.setPadding(5, 2, 5, 2);
                tr.addView(SSID_TV);  // Adding textView to tablerow.


                /** Creating another textview **/
                channelTV = new TextView(this);
                channelTV.setText(Integer.toString(convertFrequencyToChannel(results.get(size).frequency)));
                channelTV.setTextColor(Color.BLACK);
                channelTV.setTextSize(getResources().getDimension(R.dimen.myFontSize));
                channelTV.setPadding(5, 2, 5, 2);
                channelTV.setTypeface(Typeface.DEFAULT);
                tr.addView(channelTV); // Adding textView to tablerow.

                /** Creating another textview **/
                strengthTV = new TextView(this);
                strengthTV.setText(Integer.toString(wifiStrength));
                strengthTV.setTextColor(Color.BLACK);
                strengthTV.setTextSize(getResources().getDimension(R.dimen.myFontSize));
                strengthTV.setPadding(5, 2, 5, 2);
                strengthTV.setTypeface(Typeface.DEFAULT);
                tr.addView(strengthTV); // Adding textView to tablerow.

                // Add the TableRow to the TableLayout
                tl.addView(tr, new TableLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT));

                //add row divider
                trDivider = new TableRow(this);
                trDivider.setPadding(0, 0, 0, 1); //Border between rows
                trDivider.setBackgroundColor(Color.LTGRAY);
                tl.addView(trDivider, new TableLayout.LayoutParams( // add line between rows
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT));
            }
            size--;
        }
        startDisplay = false;       // allow new wifi scan result
    }

    /** This function add the headers to the table **/
    private void addHeaders() {
        Log.d("DisplayWifiScan2", "addHeaders called");
        /** Create a TableRow dynamically **/
        tr = new TableRow(this);
        //tr.setBackground(gd);

        //Log.d("DisplayWifiScan2", "r.dimen.myFontSize = " + getResources().getDimension(R.dimen.myFontSize));

        /** Creating a TextView to add to the row **/
        TextView SSID_TV = new TextView(this);
        SSID_TV.setText("Network");
        SSID_TV.setTextColor(Color.BLACK);
        SSID_TV.setTextSize(getResources().getDimension(R.dimen.myFontSize));
        SSID_TV.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        SSID_TV.setPadding(5, 5, 5, 5);
        tr.addView(SSID_TV);  // Adding textView to tablerow.

        /** Creating another textview **/
        TextView channelTV = new TextView(this);
        channelTV.setText("Channel");
        channelTV.setTextColor(Color.BLACK);
        channelTV.setTextSize(getResources().getDimension(R.dimen.myFontSize));
        channelTV.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        channelTV.setPadding(5, 5, 5, 5);
        tr.addView(channelTV); // Adding textView to tablerow.

        /** Creating another textview **/
        TextView strengthTV = new TextView(this);
        strengthTV.setText("Strength");
        strengthTV.setTextColor(Color.BLACK);
        strengthTV.setTextSize(getResources().getDimension(R.dimen.myFontSize));
        strengthTV.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        strengthTV.setPadding(5, 5, 5, 5);
        tr.addView(strengthTV); // Adding textView to tablerow.

        // Add the TableRow to the TableLayout
        tl.addView(tr, new TableLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));

        //add row divider
        trDivider = new TableRow(this);
        trDivider.setPadding(0, 0, 0, 1); //Border between rows
        trDivider.setBackgroundColor(Color.LTGRAY);
        tl.addView(trDivider, new TableLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_wifi_scan2, menu);
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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        Log.d("DisplayWifiScan2", "onDestroy called");
        super.onDestroy();
        unregisterReceiver(myReceiver);
        sharedPref.unregisterOnSharedPreferenceChangeListener(prefListener);
    }

    // don't need this yet, gets called initially prior to first scan AND it is NOT called after prefernce screen goes away.
//    @Override
//    public void onResume(){
//        super.onResume();
//        // put your code here...
//        tl.removeAllViews();    // clear table
//        addHeaders();           // add table headers
//        displayScan();          // add table rows
//    }
}
