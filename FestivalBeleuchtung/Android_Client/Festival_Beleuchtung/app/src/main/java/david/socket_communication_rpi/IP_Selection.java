package david.socket_communication_rpi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.math.BigInteger;
import java.net.*;
import java.lang.*;
import java.io.*;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import android.os.Vibrator;

public class IP_Selection extends AppCompatActivity {

    private void saveToFile(String fname, String content) {
        FileOutputStream fos = null;

        Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);

        if (isSDPresent) {
            try {
                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Festival-IP/";
                File storageFile = new File(path);
                if (!storageFile.exists()) {
                    storageFile.mkdirs();
                }
                final File textFile = new File(storageFile, fname + ".txt");
                if (!textFile.exists()) {
                    textFile.createNewFile();
                }

                fos = new FileOutputStream(textFile);

                fos.write(content.getBytes());
                fos.close();
            } catch (IOException e) {

            }
        } else {
            try {
                String file = fname + ".txt";
                FileOutputStream fOut = openFileOutput(file, MODE_PRIVATE);
                fOut.write(content.getBytes());
                fOut.close();
            } catch (IOException e) {
                System.out.println("IOEXCEPTION");
            }
        }
    }

    private void removeFile(String fname) {
        File file = new File(fname);
        file.delete();
    }

    private String readFile(String fname) {

        Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);

        if (isSDPresent) {
            File file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/Festival-IP/" + fname + ".txt");

            StringBuilder text = new StringBuilder();

            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                if ((line = br.readLine()) != null) {
                    text.append(line);
                }
                br.close();
            } catch (IOException e) {
                //You'll need to add proper error handling here
                return "none";
            }
            return text.toString();
        } else {

            StringBuilder text = new StringBuilder();

            try {
                FileInputStream fin = openFileInput(fname + ".txt");
                int c;
                String temp = "";
                while ((c = fin.read()) != -1) {
                    temp = temp + Character.toString((char) c);
                }
                fin.close();
                return temp;
            } catch (IOException e) {

            }
        }

        return "";
    }

    double abs(double a){
        return a >= 0 ? a : -a;
    }

    double[] col = {0,0,0};
    boolean recoloring = false;
    double r = 0;
    double g = 0;
    double b = 0;

    boolean shiftColor(double r, double g, double b, int thres, int velo){

        if(thres < velo) thres = velo;
        int thres2 = velo+1;
        boolean diverging = false;
        double fr=1, fg=1, fb = 1;
        if(abs(r - col[0]) > thres2) fr *= velo;
        if(abs(g - col[1]) > thres2) fg *= velo;
        if(abs(b - col[2]) > thres2) fb *= velo;

        if(abs(r - col[0]) > thres & r - col[0] > 0){ col[0] += fr; diverging = true;}
        if(abs(r - col[0]) > thres & r - col[0] < 0){ col[0] -= fr; diverging = true;}

        if(abs(g - col[1]) > thres & g - col[1] > 0){ col[1] += fg; diverging = true;}
        if(abs(g - col[1]) > thres & g - col[1] < 0){ col[1] -= fg; diverging = true;}

        if(abs(b - col[2]) > thres & b - col[2] > 0){ col[2] += fb; diverging = true;}
        if(abs(b - col[2]) > thres & b - col[2] < 0){col[2] -= fb; diverging = true;}

        return diverging;
    }

    void recolor(TextView tView, double red, double green, double blue) {
        final IP_Selection here = this;
        final TextView there = tView;
        r = red;
        g = green;
        b = blue;

        //create a new gradient color
        final GradientDrawable rg = new GradientDrawable();

        rg.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        rg.setGradientRadius(800);


            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(!recoloring) {
                        recoloring = true;
                        while (shiftColor(r, g, b, 10, 5)) {
                            here.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    int combinedColor = (0xff << 24) + ((int) col[0] << 16) + ((int) col[1] << 8) + ((int) col[2]);
                                    int[] color = {combinedColor, Color.parseColor("#00000000")};//Color.parseColor(cString)
                                    float pos = (float)col[0] + (float)col[1] + (float)col[2];
                                    pos = pos / 785.0f;
                                    rg.setGradientCenter(0.2f+(0.6f*pos), 0.2f+(0.6f*pos));
                                    rg.setColors(color);
                                    there.setBackground(rg);
                                }
                            });
                            WAIT(10);
                        }
                        recoloring = false;
                    }
                }
                }).start();
    }

    private IP_Selection mainReference = this;

    private void WAIT(int millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {

        }
    }

    public static boolean validIP(String ip) {
        try {
            if (ip == null || ip.isEmpty()) {
                return false;
            }

            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }

            for (String s : parts) {
                int i = Integer.parseInt(s);
                if ((i < 0) || (i > 255)) {
                    return false;
                }
            }
            if (ip.endsWith(".")) {
                return false;
            }

            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    protected String wifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("WIFIIP", "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }

    private GoogleApiClient client;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation change
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip__selection);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final EditText ipField = (EditText) findViewById(R.id.ip_text);
        ipField.bringToFront();
        ipField.invalidate();
        final TextView colorInd = (TextView) findViewById(R.id.color_indicator);
        final SeekBar redBar = (SeekBar) findViewById(R.id.red_slider);
        final SeekBar greenBar = (SeekBar) findViewById(R.id.green_slider);
        final SeekBar blueBar = (SeekBar) findViewById(R.id.blue_slider);
        redBar.setMax(255); greenBar.setMax(255); blueBar.setMax(255);
        redBar.setProgress(255 / 2); greenBar.setProgress(255 / 2); blueBar.setProgress(255/2);
        recolor(colorInd, 255/2,255/2,255/2);

        int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        if (actionBarTitleId > 0) {
            TextView title = (TextView) findViewById(actionBarTitleId);
            if (title != null) {
                title.setTextColor(Color.BLACK);
            }
        }

        String savedIP = readFile("FestivalIP");
        if (validIP(savedIP)) {
            ipField.setText(savedIP);
        } else{
            ipField.setText("192.168.0.2");
        }

        colorInd.setOnClickListener(new View.OnClickListener() {
            Context context = getApplicationContext();

            @Override
            public void onClick(View v) {
                Vibrator vib = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
                vib.vibrate(30);
            }
        });

        findViewById(R.id.start_btn).setOnClickListener(new View.OnClickListener() {
            Context context = getApplicationContext();

            @Override
            public void onClick(View v) {
                Vibrator vib = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
                vib.vibrate(30);
            }
        });

        redBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                recolor(colorInd, redBar.getProgress(), g, b);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        greenBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                recolor(colorInd, r, greenBar.getProgress(), b);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        blueBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                recolor(colorInd, r, g, blueBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });



        findViewById(R.id.save_btn).setOnClickListener(new View.OnClickListener() {
            Context context = getApplicationContext();

            @Override
            public void onClick(View v) {
                if (validIP(ipField.getText().toString())) {
                    removeFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Festival-IP/FestivalIP.txt");
                    saveToFile("FestivalIP", ipField.getText().toString());
                    Toast.makeText(context, "Gespeichert", Toast.LENGTH_SHORT).show();
                    Vibrator vib = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
                    vib.vibrate(30);
                } else {
                    Toast.makeText(context, "Please enter a valid IPv4-address", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.remove_btn).setOnClickListener(new View.OnClickListener() {
            Context context = getApplicationContext();

            @Override
            public void onClick(View v) {
                ipField.setText("");
                Vibrator vib = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
                vib.vibrate(30);
            }
        });

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();

        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Farbauswahl", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://david.festival_color_selection/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        finish();
        System.exit(0);

        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Farbauswahl", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://david.festival_color_selection/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
