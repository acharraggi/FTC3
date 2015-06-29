package com.example.mike.ftc3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
//import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
//import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
//import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

// make list fancier: http://eureka.ykyuen.info/2010/01/03/android-simple-listview-using-simpleadapter/

public class DisplayWifiScan extends AppCompatActivity implements View.OnClickListener {
    private WifiManager wifi;
    private int size = 0;
    private List<ScanResult> results;

    //String ITEM_KEY = "key";
    // create the grid item mapping
    private final String[] from = new String[] {"SSID", "frequency", "channel", "strength"};
    private final int[] to = new int[] { R.id.SSID, R.id.frequency, R.id.channel, R.id.strength };

    private final ArrayList<HashMap<String, String>> arraylist = new ArrayList<HashMap<String, String>>();
    private SimpleAdapter adapter;

    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            results = wifi.getScanResults();
            Collections.sort(results, new Comparator<ScanResult>() {
                @Override
                public int compare(final ScanResult object1, final ScanResult object2) {
                // sort ascending channel#, descending strength
                return ((object2.frequency*1000-object2.level) - (object1.frequency*1000-object1.level));
                }
            });
            size = results.size();
            displayScan();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_wifi_scan);

        //TextView textStatus = (TextView) findViewById(R.id.textStatus);
        Button buttonScan = (Button) findViewById(R.id.buttonReScan);
        buttonScan.setOnClickListener(this);
        ListView lv = (ListView)findViewById(R.id.list);

        View header = getLayoutInflater().inflate(R.layout.header, null);
        lv.addHeaderView(header);

        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled())
        {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }

        this.adapter = new SimpleAdapter(DisplayWifiScan.this, arraylist, R.layout.row, from, to);
        lv.setAdapter(this.adapter);

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
        arraylist.clear();
        Toast.makeText(this, "Wifi network(s) found = " + size, Toast.LENGTH_SHORT).show();

        size = size - 1;
        while (size >= 0)
        {
            HashMap<String, String> item = new HashMap<String, String>();
            //item.put(ITEM_KEY, results.get(size).SSID + ",   Frequency: " + results.get(size).frequency + ",   Channel: "+ convertFrequencyToChannel(results.get(size).frequency) + ",   Strength: " + WifiManager.calculateSignalLevel(results.get(size).level,5));
            item.put("SSID", results.get(size).SSID);
            item.put("frequency", Integer.toString(results.get(size).frequency));
            item.put("channel", Integer.toString(convertFrequencyToChannel(results.get(size).frequency)));
            item.put("strength", Integer.toString(WifiManager.calculateSignalLevel(results.get(size).level, 5)));
            //item.put("strength", Integer.toString(results.get(size).level));
            arraylist.add(item);

            size--;
            adapter.notifyDataSetChanged();
        }
        adapter.notifyDataSetChanged();
    }

    public void onClick(View view)
    {
        wifiScan();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_wifi_scan, menu);
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
        } else if (id == R.id.action_search){
            wifiScan();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);
    }
}
