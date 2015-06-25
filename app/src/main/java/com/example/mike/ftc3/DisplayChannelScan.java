package com.example.mike.ftc3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
//import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DisplayChannelScan extends AppCompatActivity {
    private WifiManager wifi;
    private List<ScanResult> results;

    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            //Log.i("DisplayChannelScan", "onReceive called");
            // this is called initially, and as networks are dropped or added
            results = wifi.getScanResults();
            Collections.sort(results, new Comparator<ScanResult>() {
                @Override
                public int compare(final ScanResult object1, final ScanResult object2) {
                    return (object2.level - object1.level); // sort ascending order
                }
            });
            llDraw();  // redraw the network graphic
        }
    };

    private boolean mMeasured = false;
    private int llWidth = 0;
    private int llHeight = 0;
    private LinearLayout ll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("DisplayChannelScan", "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_channel_scan);

        ll = (LinearLayout) findViewById(R.id.channel_scan);

        ll.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Android must layout the screen before you can get it's size so we set up this callback.
                if (!mMeasured) {
                    // Here your view is already laid out and measured for the first time
                    mMeasured = true; // Some optional flag to mark, that we already got the sizes
                    llWidth = ll.getWidth();
                    Log.i("DisplayChannelScan", "llWidth = " + llWidth);
                    llHeight = ll.getHeight();
                    Log.i("DisplayChannelScan", "llHeight = " + llHeight);
                }
            }
        });

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

    private  void wifiScan()
    {
        wifi.startScan();
    }

    private void llDraw() {
        // draws on a canvas and then sets the background to be that canvas.
        Paint paint = new Paint();

        Bitmap bg = Bitmap.createBitmap(llWidth, llHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bg);

        paint.setColor(Color.parseColor("black"));
        canvas.drawRect(0, 0, llWidth, llHeight, paint);

        paint.setColor(Color.parseColor("white"));

        int tickSize = llHeight/18;
        for(int i = 1; i<18; i++) {
            canvas.drawLine(18, i * tickSize, 25, i * tickSize, paint); // draw tick marks
            if(i>2 && i<16) {
                canvas.drawText(String.valueOf(i - 2), 0, i * tickSize + 4, paint);  // add channel numbers beside tick marks
            }
        }

                for(ScanResult r: results) {
            int channel = convertFrequencyToChannel(r.frequency);
            int signalLevel = WifiManager.calculateSignalLevel(r.level, 5); // returns 0 to 4
            if (channel > 0 && channel < 14 && signalLevel > 0) {  // ignore unexpected Wifi frequencies, and weak signals.
                int tick = channel+2;
                int strength = convertLevelToStrength(r.level)*llWidth/100;


                paint.setStyle(Paint.Style.STROKE); // try outlined ovals
                switch (signalLevel){
                    case 4: paint.setColor(Color.parseColor("#90FF0000"));  //red
                        break;
                    case 3: paint.setColor(Color.parseColor("#90FF8000"));  //orange
                        break;
                    case 2: paint.setColor(Color.parseColor("#90FFFF00"));  //yellow
                        break;
                    case 1: paint.setColor(Color.parseColor("#9000FF00"));  //green
                        break;
                    default: paint.setColor(Color.parseColor("#90303030"));  //light grey
                        break;
                }
                canvas.translate((-strength / 2) + 30, 0);  // shift where we draw half oval so it lines up beside the tick marks.
                canvas.drawArc(new RectF(0, tickSize * (tick - 2), strength, tickSize * (tick + 2)), 270, 180, true, paint); // draw half an oval

                paint.setStyle(Paint.Style.FILL); // fill interior with more transparent colour
                switch (signalLevel){
                    case 4: paint.setColor(Color.parseColor("#30FF0000"));  //red
                        break;
                    case 3: paint.setColor(Color.parseColor("#30FF8000"));  //orange
                        break;
                    case 2: paint.setColor(Color.parseColor("#30FFFF00"));  //yellow
                        break;
                    case 1: paint.setColor(Color.parseColor("#3000FF00"));  //green
                        break;
                    default: paint.setColor(Color.parseColor("#30303030"));  //light grey
                        break;
                }
                canvas.drawArc(new RectF(2, tickSize * (tick - 2)+2, strength-2, tickSize * (tick + 2)-2), 270, 180, true, paint); // draw half an oval
                canvas.translate(strength / 2 - 30, 0);     // undo shift

                //paint.setColor(Color.parseColor("white"));
                switch (signalLevel){
                    case 4: paint.setColor(Color.parseColor("#FFFF0000"));  //red
                        break;
                    case 3: paint.setColor(Color.parseColor("#AAFF8000"));  //orange
                        break;
                    case 2: paint.setColor(Color.parseColor("#90FFFF00"));  //yellow
                        break;
                    case 1: paint.setColor(Color.parseColor("#9000FF00"));  //green
                        break;
                    default: paint.setColor(Color.parseColor("#90303030"));  //light grey
                        break;
                }
                canvas.drawText(r.SSID, strength, tick * tickSize, paint);  // write network name beside oval
            }
        }

        ll.setBackground(new BitmapDrawable(getResources(), bg)); // set graphic as background
        ll.invalidate();   // force layout redraw
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_channel_scan, menu);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);  // unregister the Wifi receiver
    }

    private static int convertFrequencyToChannel(int freq) {
        // convert standard Wifi frequencies to Wifi channel numbers.
        if (freq >= 2412 && freq <= 2484) {
            return (freq - 2412) / 5 + 1;
        } else if (freq >= 5170 && freq <= 5825) {
            return (freq - 5170) / 5 + 34;
        } else {
            return -1;
        }
    }

    private static int convertLevelToStrength(int level) {
        // convert wifi level to a number between 0-100, likely values 5-50
        int r;
        if (level < -100) {
            r = 0;
        }
        else if (level > 0) {
            r = 100;
        }
        else r = 100+level;

        //Log.d("DisplayChannelScan","Level = "+ level + ", r =" +r);
        return(r);
    }
}
