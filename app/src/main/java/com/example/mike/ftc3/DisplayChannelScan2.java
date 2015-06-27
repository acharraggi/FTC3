package com.example.mike.ftc3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
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

public class DisplayChannelScan2 extends AppCompatActivity {
    private WifiManager wifi;
    private List<ScanResult> results;

    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            //Log.i("DisplayChannelScan2", "onReceive called");
            // this is called initially, and as networks are dropped or added
            results = wifi.getScanResults();
            Collections.sort(results, new Comparator<ScanResult>() {
                @Override
                public int compare(final ScanResult object1, final ScanResult object2) {
                    // level is a number between about -90 to -50 with -90 being weaker.
                    return (object1.level - object2.level); // stronger networks will print last
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
        Log.i("DisplayChannelScan2", "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_channel_scan2);

        ll = (LinearLayout) findViewById(R.id.channel_scan2);

        ll.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Android must layout the screen before you can get it's size so we set up this callback.
                if (!mMeasured) {
                    // Here your view is already laid out and measured for the first time
                    mMeasured = true; // Some optional flag to mark, that we already got the sizes
                    llWidth = ll.getWidth();
                    Log.i("DisplayChannelScan2", "llWidth = " + llWidth);
                    llHeight = ll.getHeight();
                    Log.i("DisplayChannelScan2", "llHeight = " + llHeight);
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

    private void wifiScan()
    {
        wifi.startScan();
    }

    private void llDraw() {
        // draws on a canvas and then sets the background to be that canvas.
        Paint paint = new Paint();
        Rect bounds = new Rect();
        //Path myPath = new Path();

        Bitmap bg = Bitmap.createBitmap(llWidth, llHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bg);

        paint.setTextSize(getResources().getDimensionPixelSize(R.dimen.myFontSize));
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

        int channelSize[] = new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0}; // declare size 14
        String channelNames[] = new String[] {"","","","","","","","","","","","","",""};

        for(ScanResult r: results) {
            int channel = convertFrequencyToChannel(r.frequency);
            int signalLevel = WifiManager.calculateSignalLevel(r.level, 5); // returns 0 to 4

            if (channel > 0 && channel < 14 && signalLevel > 0) {  // ignore unexpected Wifi frequencies, and weak signals.
                int tick = channel+2;
               // int strength = convertLevelToStrength(r.level)*llWidth/100;
                int strength = convertLevelToStrength(r.level)*2;
                //Log.i("DisplayChannelScan2","level = "+r.level+", signalLevel = "+signalLevel+", strength = "+strength);

                switch (signalLevel){
                    case 4: paint.setColor(Color.parseColor("#90FF0000")); //red 90FF0000
                        break;
                    case 3: paint.setColor(Color.parseColor("#90FF00FF"));  //purple 90FFFF00 (orange 90FF8000)
                        break;
                    case 2: paint.setColor(Color.parseColor("#90FFFF00"));  //yellow
                        break;
                    case 1: paint.setColor(Color.parseColor("#9000FF00"));  //green
                        break;
                    default: paint.setColor(Color.parseColor("#90606060"));  //light grey
                        break;
                }

                RectF myRectF = new RectF(30 + channelSize[channel], tickSize * (tick - 2), 30 + strength + channelSize[channel], tickSize * (tick + 2));
                canvas.drawRoundRect(myRectF, 15, 15, paint);
                double myHypot = Math.hypot((double)myRectF.width(),(double)myRectF.height());

                paint.getTextBounds(r.SSID, 0, r.SSID.length(), bounds);
                //Log.i("DisplayChannelScan2","myHypot = "+(int)myHypot+", text width = "+ bounds.width()+", strength = "+strength);

                Path myPath = new Path();
                float hOffset = 0;
                float vOffset = bounds.height()/2;

                if(bounds.width()+2 <= strength) { //+2 is to draw text at least 1 pixel inside box
                    myPath.moveTo(31 + channelSize[channel], tickSize * tick); //31 is +1 px
                    myPath.lineTo(30 + strength + channelSize[channel], tickSize * tick);
                    hOffset = (strength - (bounds.width()+2)) / 2;      // centre text on path
                }
                else {
                    if (((float) myHypot) - (bounds.width()+2) < 0) {  //+2 is to draw text at least 1 pixel inside box
                        hOffset = 0;  // text is bigger than hypotenuse, draw corner to corner
                        vOffset = 0;
                        int textOffset = strength - bounds.height() - 1;
                        if (textOffset < 0) {
                            textOffset = 0;
                        }
                        myPath.moveTo(31 + channelSize[channel], tickSize * (tick - 2) + 1); //make path just inside rectangle by 1px
                        myPath.lineTo(31 + textOffset + channelSize[channel], tickSize * (tick + 2) - 1); // move a bit less as text drawn on top of line
                    } else {
                        hOffset = 0;
                        vOffset = 0;
                        int textWidth = bounds.width();
                        if (textWidth > (int)myHypot) {
                            textWidth = (int)myHypot;
                        }
                        // set up trig, find missing side
                        double c = ((double)textWidth)/2;
                        double b = ((double)(strength))/2;
                        int textOffset = (int)Math.round(Math.sqrt(c*c - b*b));
                        //Log.i("DisplayChannelScan2", "textOffset = " + textOffset + ", text width = " + bounds.width() + ", strength = " + strength);
                        myPath.moveTo(31 + channelSize[channel], (tickSize * tick) - textOffset); //make path just inside rectangle by 1px
                        //TODO: although trig calc ok, we actually need to draw text not a line, so the '35' and '+5' below are kluges to get text to mostly fit an not get truncated
                        myPath.lineTo(35 + channelSize[channel]+ strength - bounds.height(), (tickSize * tick) + textOffset + bounds.height()+5); // move a bit more as text drawn on top of line
                     }
                }
                paint.setColor(Color.parseColor("white"));
                canvas.drawTextOnPath(r.SSID,myPath,hOffset,vOffset,paint);  // naturally truncates if text too big to fit on path

                channelSize[channel] = channelSize[channel] + strength;
                channelNames[channel] = channelNames[channel] + r.SSID + " ";
            }
        }

        ll.setBackground(new BitmapDrawable(getResources(), bg)); // set graphic as background
        ll.invalidate();   // force layout redraw
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_channel_scan2, menu);
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
        // convert standard Wifi frequencies to Wifi channel numbers. 1-13
        if (freq >= 2412 && freq <= 2484) {
            return (freq - 2412) / 5 + 1;
  //      } else if (freq >= 5170 && freq <= 5825) {
  //          return (freq - 5170) / 5 + 34;
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

        //Log.d("DisplayChannelScan2","Level = "+ level + ", r =" +r);
        return(r);
    }
}
