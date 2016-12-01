package com.example.mike.FTCScanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class DisplayChannelScan2 extends AppCompatActivity {
    private WifiManager wifi;
    private List<ScanResult> results;
    private int[] channelTotal = new int[14];

    // preference variables
    private Boolean excludeWeakWifi = true;
    private volatile Boolean excludeRobots = true;
    private Boolean isVertical = true;
    private SharedPreferences sharedPref;
    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs,
                                                      String key) {
                    Log.d("DisplayChannelScan2", "myReceiver called, key="+key);
                    if (key.equals("exclude_weak_wifi")) {
                        boolean newValue=prefs.getBoolean("exclude_weak_wifi", false);
                        if(newValue != excludeWeakWifi) {
                            excludeWeakWifi = newValue;
                            wifiScan();  //rescan
                        }
                    }
                    if (key.equals("is_vertical")) {
                        boolean newValue=prefs.getBoolean("is_vertical", false);
                        if(newValue != isVertical) {
                            isVertical = newValue;
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
            Log.d("DisplayChannelScan2", "onReceive called");
            // this is called initially, and as networks are dropped or added
            results = wifi.getScanResults();
            for(int i = 0; i<14; i++){
                channelTotal[i] = 0;
            }

            //filter results - remove weak signals or invalid channels
            for (Iterator<ScanResult> iterator = results.iterator(); iterator.hasNext();) {
                ScanResult r = iterator.next();
                int channel = convertFrequencyToChannel(r.frequency);
                int signalLevel = WifiManager.calculateSignalLevel(r.level, 5); // returns 0 to 4

                int signalTest = -1;
                if(excludeWeakWifi) {
                    signalTest = 0;
                }
                if ((channel > 0 && channel < 14 && signalLevel > signalTest) &&
                    (!excludeRobots || !isFTCrobot(r.SSID))) {
                    channelTotal[channel] += convertLevelToStrength(r.level) * 2;
                }
                else { // ignore unexpected frequencies. optionally ignore weak signals.
                    // Remove the current element from the iterator and the list.
                    iterator.remove();
                }
            }
            int maxChannel = 0;
            for(int i=1; i<14; i++) {
                Log.d("DisplayChannelScan2", "channelTotal["+i+"] = " + channelTotal[i]);
                if(channelTotal[i] > maxChannel) {
                    maxChannel = channelTotal[i];
                }
            }

            // sort results
            Collections.sort(results, new Comparator<ScanResult>() {
                @Override
                public int compare(final ScanResult object1, final ScanResult object2) {
                    // level is a number between about -90 to -50 with -90 being weaker.
                    return (object1.level - object2.level); // stronger networks will print last
                }
            });

            //filter results if they won't fit
            int maxSize = isVertical ? llWidth - 30 : llHeight - 30 ;
            if( maxChannel > maxSize ) {
                Log.d("DisplayChannelScan2","onReceive: results filtered to fit");
                for (Iterator<ScanResult> iterator = results.iterator(); iterator.hasNext(); ) {
                    ScanResult r = iterator.next();
                    int channel = convertFrequencyToChannel(r.frequency);

                    if (channelTotal[channel] > maxSize) {
                        channelTotal[channel] = channelTotal[channel] - convertLevelToStrength(r.level) * 2;
                        iterator.remove();
                    }
                 }
            }

            llDraw();  // redraw the network graphic
        }
    };

    private static boolean isFTCrobot(String ssid_name) {
        // examine SSID and see if it matches the FTC robot naming convention
        // Robot Name: 12345-RC or 12345-B-RC, where B, can be any letter.
        // WiFi Direct adds a prefix of DIRECT-xx- where xx is any two uppercase letters
        // eg. DIRECT-XJ-3491-C-RC
        if(ssid_name.matches("^DIRECT-[a-zA-Z]{1,2}-[0-9]{1,5}(-[A-Z])??-((DS)|(RC))$")) {
                return true;
        }
        return false;
    }

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

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        excludeWeakWifi = sharedPref.getBoolean("exclude_weak_wifi", true);
        excludeRobots = sharedPref.getBoolean("exclude_robots", false);
        isVertical = sharedPref.getBoolean("is_vertical", true);
        sharedPref.registerOnSharedPreferenceChangeListener(prefListener);
        Log.d("DisplayWifiScan2", "excludeWeakWifi = " + excludeWeakWifi + ", isVertical = " + isVertical + ", excludeRobots = " + excludeRobots);

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

    private void drawTicks(Canvas canvas, int tickSize, Paint paint) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        int fontHeight = (int)(fm.descent - fm.ascent);
        Rect bounds = new Rect();
        for(int i = 1; i<18; i++) {
            if(isVertical) {
                canvas.drawLine(18, i * tickSize, 25, i * tickSize, paint); // draw tick marks
            }
            else {
                canvas.drawLine(i * tickSize, llHeight-(fontHeight+9), i * tickSize, llHeight-(fontHeight+2), paint); // draw tick marks
            }
            if(i>2 && i<16) {
                if(isVertical) {
                    canvas.drawText(String.valueOf(i - 2), 0, i * tickSize + 4, paint);  // add channel numbers beside tick marks
                }
                else {
                    String s = String.valueOf(i - 2);
                    paint.getTextBounds(s, 0, s.length(), bounds);
                    canvas.drawText(String.valueOf(i - 2), i * tickSize - bounds.width()/2 -1, llHeight-fm.descent , paint);  // add channel numbers under tick marks
                }
            }
        }
    }
    private void llDraw() {
        Log.d("DisplayChannelScan2", "llDraw called");
        // draws on a canvas and then sets the background to be that canvas.
        Paint paint = new Paint();
        Rect bounds = new Rect();
        int tickSize;
        if(isVertical) {
            tickSize = llHeight / 18;
        }
        else {
            tickSize = llWidth / 18;
        }

        Bitmap bg = Bitmap.createBitmap(llWidth, llHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bg);

        paint.setTextSize(getResources().getDimensionPixelSize(R.dimen.myFontSize));
        Paint.FontMetrics fm = paint.getFontMetrics();
        int fontHeight = (int)(fm.descent - fm.ascent);

        paint.setColor(Color.parseColor("black"));
        canvas.drawRect(0, 0, llWidth, llHeight, paint);

        paint.setColor(Color.parseColor("white"));

        drawTicks(canvas, tickSize, paint);

        int channelSize[] = new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0}; // declare size 14

        for(ScanResult r: results) {
            int channel = convertFrequencyToChannel(r.frequency);
            int signalLevel = WifiManager.calculateSignalLevel(r.level, 5); // returns 0 to 4

            int tick = channel + 2;
            // int strength = convertLevelToStrength(r.level)*llWidth/100;
            int strength = convertLevelToStrength(r.level) * 2;
            //Log.i("DisplayChannelScan2","level = "+r.level+", signalLevel = "+signalLevel+", strength = "+strength);
            //Log.d("DisplayChannelScan2", "Network: "+r.toString());
            Log.d("DisplayChannelScan2","SSID="+r.SSID+", length="+r.SSID.length()+", isEmpty="+r.SSID.isEmpty());
            String netName = r.SSID.isEmpty() ? r.BSSID : r.SSID;

            switch (signalLevel) {
                case 4:
                    paint.setColor(Color.parseColor("#90FF0000")); //red 90FF0000
                    break;
                case 3:
                    paint.setColor(Color.parseColor("#90FF00FF"));  //purple 90FFFF00 (orange 90FF8000)
                    break;
                case 2:
                    paint.setColor(Color.parseColor("#90FFFF00"));  //yellow
                    break;
                case 1:
                    paint.setColor(Color.parseColor("#9000FF00"));  //green
                    break;
                default:
                    paint.setColor(Color.parseColor("#90606060"));  //light grey
                    break;
            }

            RectF myRectF;
            if (isVertical) {
                myRectF = new RectF(30 + channelSize[channel], tickSize * (tick - 2), 30 + strength + channelSize[channel], tickSize * (tick + 2));
            } else {
                int vOffset = llHeight - fontHeight - 9;
                myRectF = new RectF(tickSize * (tick - 2), vOffset - strength - channelSize[channel], tickSize * (tick + 2), vOffset - channelSize[channel]);
            }
            canvas.drawRoundRect(myRectF, 15, 15, paint);
            //double myHypot = Math.hypot((double)myRectF.width(),(double)myRectF.height());

            paint.getTextBounds(netName, 0, netName.length(), bounds);
            //Log.i("DisplayChannelScan2","myHypot = "+(int)myHypot+", text width = "+ bounds.width()+", strength = "+strength);

            Path myPath = new Path();
            float hOffset = 0;
            float vOffset = bounds.height() / 2;

            paint.setColor(Color.parseColor("white"));
            if (!isVertical) {
                vOffset = llHeight - fontHeight - 9 - strength/2 - channelSize[channel];
                //Log.d("DisplayChannelScan2", "vOffset=" + vOffset + ", channel=" + channel + ", llHeight=" + llHeight);
                int start = 1 + tickSize*(tick-2);
                int end = tickSize*(tick+2) - 1;
                if ((end-start)>bounds.width()) {
                    paint.setTextAlign(Paint.Align.CENTER);
                }
                else {
                    paint.setTextAlign(Paint.Align.LEFT);
                }
                myPath.moveTo(start, vOffset);
                myPath.lineTo(end, vOffset);

                canvas.drawTextOnPath(netName, myPath, 0, fm.descent, paint);  // naturally truncates if text too big to fit on path
            } else {
                if (bounds.width() + 2 <= strength) { //+2 is to draw text at least 1 pixel inside box
                    paint.setTextAlign(Paint.Align.LEFT);
                    myPath.moveTo(31 + channelSize[channel], tickSize * tick); //31 is +1 px
                    myPath.lineTo(30 + strength + channelSize[channel], tickSize * tick);
                    hOffset = (strength - (bounds.width() + 2)) / 2;      // centre text on path
                    canvas.drawTextOnPath(netName, myPath, hOffset, vOffset, paint);  // naturally truncates if text too big to fit on path
                } else {
                    paint.setTextAlign(Paint.Align.CENTER);  // letters drawn centred on x,y coords
                    char c[] = netName.toCharArray();
                    int cHeight[] = new int[c.length];
                    int cTotal = 0;

                    for (int i = 0; i < c.length; i++) {  // precalculate character heights
                        paint.getTextBounds(c, i, 1, bounds);
                        if (c[i] == ' ') {
                            cHeight[i] = fontHeight / 2; // blanks have a bounds.height of zero
                        } else {
                            cHeight[i] = bounds.height();
                        }
                        cTotal += bounds.height() + 3; // include char spacer
                    }
//                    if (cTotal > 3) {
//                        cTotal = cTotal - 3;
//                    }

                    int x = 31 + (strength / 2) + channelSize[channel];
                    int y;
                    if (cTotal >= (tickSize * (tick + 2)) - (tickSize * (tick - 2))) {
                        y = tickSize * (tick - 2) + fontHeight / 2;  // start at top of box
                    } else {
                        y = (((tickSize * (tick + 2)) - (tickSize * (tick - 2))) - cTotal) / 2 + tickSize * (tick - 2);  // centre text vertically
                    }
                    int maxY = tickSize * (tick + 2) - fontHeight / 2;

                    for (int i = 0; i < c.length && y < maxY; i++) {  // draw string vertically but letters horizontal
                        paint.getTextBounds(c, i, 1, bounds);
                        canvas.drawText(c, i, 1, x, y + (cHeight[i] / 2) + 2, paint);  // slight offset +2 seems to help with tall characters
                        y += cHeight[i] + 3; // char spacer
                    }
                }

            }
            channelSize[channel] = channelSize[channel] + strength;
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

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);  // unregister the Wifi receiver
        sharedPref.unregisterOnSharedPreferenceChangeListener(prefListener);
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
