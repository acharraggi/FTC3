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
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DisplayChannelScan extends ActionBarActivity {
    WifiManager wifi;
    List<ScanResult> results;

    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            results = wifi.getScanResults();
            Collections.sort(results, new Comparator<ScanResult>() {
                @Override
                public int compare(final ScanResult object1, final ScanResult object2) {
                    return (object2.level - object1.level); // sort ascending order
                }
            });
            //displayScan();
            llDraw();
        }
    };

    boolean mMeasured = false;
    int llWidth = 0;
    int llHeight = 0;
    LinearLayout ll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("DisplayChannelScan", "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_channel_scan);

        ll = (LinearLayout) findViewById(R.id.channel_scan);

        ll.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!mMeasured) {
                    // Here your view is already layed out and measured for the first time
                    mMeasured = true; // Some optional flag to mark, that we already got the sizes
                    llWidth = ll.getWidth();
                    Log.i("DisplayChannelScan", "llWidth = " + llWidth);
                    llHeight = ll.getHeight();
                    Log.i("DisplayChannelScan", "llHeight = " + llHeight);
                    //llDraw();
                    //ll.invalidate();
                }
            }
        });

        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() == false)
        {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }
        registerReceiver(myReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        // start with initial scan
        wifiScan();

    }

    public void wifiScan()
    {
        wifi.startScan();
    }

    private void llDraw() {
        Paint paint = new Paint();

        Bitmap bg = Bitmap.createBitmap(llWidth, llHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bg);

        paint.setColor(Color.parseColor("#000000"));   //black
        canvas.drawRect(0, 0, llWidth, llHeight, paint);

        paint.setColor(Color.parseColor("#FFFFFF"));   //white

        int tickSize = llHeight/18;
        for(int i = 1; i<18; i++) {
            paint.setColor(Color.parseColor("#FFFFFF"));   //white
            canvas.drawLine(18, i * tickSize, 25, i * tickSize, paint);
            if(i>2 && i<16) {
                canvas.drawText(String.valueOf(i - 2), 0, i * tickSize + 4, paint);
//                paint.setColor(Color.parseColor("#A0FF0000"));   //red
//                canvas.drawText("network name",205,i*tickSize+4, paint);
            }
        }

        for(ScanResult r: results) {
            int channel = convertFrequencyToChannel(r.frequency);
            int signalLevel = WifiManager.calculateSignalLevel(r.level, 5);
            if (channel > 0 && signalLevel > 0) {
                int tick = channel+2;
                int strength = convertLevelToStrength(r.level)*llWidth/100;

                switch (signalLevel){
                    case 5: paint.setColor(Color.parseColor("#2080FFFF"));
                            break;
                    case 4: paint.setColor(Color.parseColor("#4080FFFF"));
                        break;
                    case 3: paint.setColor(Color.parseColor("#6080FFFF"));
                        break;
                    case 2: paint.setColor(Color.parseColor("#8080FFFF"));
                        break;
                    case 1: paint.setColor(Color.parseColor("#A080FFFF"));
                        break;
                    default: paint.setColor(Color.parseColor("#A080FFFF"));
                        break;
                }
                canvas.translate((-strength / 2) + 30, 0);
                canvas.drawArc(new RectF(0, tickSize * (tick - 2), strength, tickSize * (tick + 2)), 270, 180, true, paint);
                canvas.translate(strength / 2 - 30, 0);

                paint.setColor(Color.parseColor("#A080FFFF"));
                canvas.drawText(r.SSID, strength, tick * tickSize + 4, paint);
            }
        }

//        paint.setColor(Color.parseColor("#A0FF0000"));  //red
//        canvas.drawRect(30, tickSize, 200, tickSize*5, paint);
//
//        canvas.translate(-50,0);
//        canvas.drawArc(new RectF(30,tickSize*10,100,tickSize*14),270,180,true,paint);
//        canvas.translate(50,0);

//        paint.setColor(Color.parseColor("#20FF0000"));  //red
//        canvas.drawRect(0, 0, 100, 100, paint);
//        canvas.drawRect(20, 20, 120, 120, paint);
//        canvas.drawRect(40, 40, 140, 140, paint);
//        canvas.drawRect(60, 60, 160, 160, paint);
//        canvas.drawRect(80, 80, 180, 180, paint);

//        paint.setColor(Color.parseColor("#FFFF0000"));  //red
//        canvas.drawText("hello",200,200,paint);

//        paint.setColor(Color.parseColor("#AAFFFF80"));  // LIGHT YELLOW
//        for(int i = 0; i<5; i++) {
//            canvas.drawRect(200+i*20, i*20, 300+i*20, 100+i*20, paint);
//        }
//
//        paint.setColor(Color.parseColor("#A080FFFF"));  // LIGHT BLUE
//        for(int i = 0; i<5; i++) {
//            canvas.drawRect(200+i*20, 100+i*20, 300+i*20, 200+i*20, paint);
//        }
//
//        paint.setColor(Color.parseColor("#A0FF80FF"));  // LIGHT PURPLE
//        for(int i = 0; i<5; i++) {
//            canvas.drawRect(200+i*20, 200+i*20, 300+i*20, 300+i*20, paint);
//        }
//        paint.setColor(Color.parseColor("#A0FF8080"));  // light red
//        for(int i = 0; i<5; i++) {
//            canvas.drawRect(200+i*20, 300+i*20, 300+i*20, 400+i*20, paint);
//        }
//        paint.setColor(Color.parseColor("#A08080FF"));  // medium blue
//        for(int i = 0; i<5; i++) {
//            canvas.drawRect(200+i*20, 400+i*20, 300+i*20, 500+i*20, paint);
//        }
//
//        paint.setColor(Color.parseColor("#A080FF80"));  // light green
//        for(int i = 0; i<5; i++) {
//            canvas.drawRect(200+i*20, 500+i*20, 300+i*20, 600+i*20, paint);
//        }
//
//        paint.setColor(Color.parseColor("#200FF000"));  //blue
//        for(int i = 0; i<5; i++) {
//            canvas.drawRect(i*20, 100+i*20, 100+i*20, 200+i*20, paint);
//        }
//        paint.setColor(Color.parseColor("#200000FF")); //green
//        for(int i = 0; i<5; i++) {
//            canvas.drawRect(i*20, 200+i*20, 100+i*20, 300+i*20, paint);
//        }
//        paint.setColor(Color.parseColor("#2000FFFF"));  // cyan
//        for(int i = 0; i<5; i++) {
//            canvas.drawRect(i*20, 300+i*20, 100+i*20, 400+i*20, paint);
//        }
//        paint.setColor(Color.parseColor("#20FF00FF"));  // magenta
//        for(int i = 0; i<5; i++) {
//            canvas.drawRect(i*20, 400+i*20, 100+i*20, 500+i*20, paint);
//        }
//        paint.setColor(Color.parseColor("#20FFFF00"));  // yellow
//        for(int i = 0; i<5; i++) {
//            canvas.drawRect(i*20, 500+i*20, 100+i*20, 600+i*20, paint);
//        }

        ll.setBackground(new BitmapDrawable(getResources(), bg));
        ll.invalidate();
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
        unregisterReceiver(myReceiver);
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

    private static int convertLevelToStrength(int level) {
        int r;
        // convert wifi level to a number between 0-100, likely values 5-50
        if (level < -100) {
            r = 0;
        }
        else if (level > 0) {
            r = 100;
        }
        else r = 100+level;

        Log.d("DisplayChannelScan","Level = "+ level + ", r =" +r);
        return(r);
    }
}
