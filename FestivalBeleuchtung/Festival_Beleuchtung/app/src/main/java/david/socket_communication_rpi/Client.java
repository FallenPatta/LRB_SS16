package david.socket_communication_rpi;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SyncStatusObserver;
import android.graphics.Color;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.net.*;
import java.lang.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client implements Runnable {
    Thread t;
    private int portNumber = 50007;
    private String hostName = "127.0.0.1";
    private TextView latestMessage;
    private TextView indication;
    private IP_Selection mainActivity;
    private Socket mainSocket;
    private List<String> commandArray = Collections.synchronizedList(new ArrayList<String>());
    public Object lock = new Object();
    public final Lock mutex = new ReentrantLock(true);

    //public Client(String host, int pNum, TextView lm, String req, TextView infoView, IP_Selection mainAct) {
    public Client(String host, int pNum, TextView lm, String req, IP_Selection mainAct, TextView indicator) {
        hostName = host;
        portNumber = pNum;
        latestMessage = lm;
        indication = indicator;
        mainActivity = mainAct;
        Thread Tconnect = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!hostName.equals("127.0.0.1")) {
                    connectSocket();
                }
            }
        });
        Tconnect.start();
    }

    private void connectSocket() {

        try {
            mainSocket = new Socket(hostName, portNumber);
            mainSocket.setSoTimeout(5000);
        } catch (UnknownHostException e) {
            System.out.println("Unknown host: " + hostName);
            UPoutput("Could not reach host");
            mainActivity.setClientRunning(0);
            return;
        } catch (IOException e) {
            System.out.println("No I/O");
            UPoutput("Connection broke");
            mainActivity.setClientRunning(0);
            return;
        }

        start();
    }

    private void WAIT(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {

        }
    }

    private void UPoutput(String s) {
        try {
            final String inUI = s;
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    latestMessage.setText(inUI);
                }
            });
        } catch (Exception e) {
        }
    }

    private int[] overlayColors(int colors1[], int colors2[], double norm){

        int color[] = {
                Color.argb(0xff, (int)(Color.red(colors2[0]) * norm + Color.red(colors1[0]) * (1-norm)), (int)(Color.green(colors2[0]) * norm + Color.green(colors1[0]) * (1-norm)), (int)(Color.blue(colors2[0]) * norm + Color.blue(colors1[0]) * (1-norm)))
                ,   Color.argb(0xff, (int)(Color.red(colors2[1]) * norm + Color.red(colors1[1]) * (1-norm)), (int)(Color.green(colors2[1]) * norm + Color.green(colors1[1]) * (1-norm)), (int)(Color.blue(colors2[1]) * norm + Color.blue(colors1[1]) * (1-norm)))
                ,   Color.argb(0xff, (int)(Color.red(colors2[2]) * norm + Color.red(colors1[2]) * (1-norm)), (int)(Color.green(colors2[2]) * norm + Color.green(colors1[2]) * (1-norm)), (int)(Color.blue(colors2[2]) * norm + Color.blue(colors1[2]) * (1-norm)))
        };

        return color;
    }

    private void recolor2(int temp) {
        final IP_Selection here = mainActivity;
        final TextView there = indication;
        final int temperatur = temp;
        //int[] colors2 = {Color.parseColor("#FF800000"),Color.parseColor("#FFFF0000"), Color.parseColor("#FFFFAF00")};
        //int[] colors1 = {Color.parseColor("#FF000050"),Color.parseColor("#FF168CAF"), Color.parseColor("#FFFFFFFF")};

        //create a new gradient color
        final GradientDrawable rg = new GradientDrawable();

        rg.setGradientType(GradientDrawable.RADIAL_GRADIENT);

        here.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                    double norm = (double) 180 / 255.0;
                    int[] colors2 = {Color.parseColor("#FF800000"),Color.parseColor("#FFFF0000"), Color.parseColor("#FFFFAF00")};
                    int[] colors1 = {Color.parseColor("#FF000050"),Color.parseColor("#FF168CAF"), Color.parseColor("#FFFFFFFF")};

                    int color[] = overlayColors(colors1, colors2, norm);

                    rg.setColors(color);
                    rg.setGradientRadius(550);
                    rg.setGradientCenter(0.5f, 1.0f);
                    there.setBackground(rg);
                    WAIT(10);
                }
        });
    }

    private void ToastMessage(String s) {
        try {
            final String inUI = s;
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mainActivity.getApplicationContext(), inUI, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
        }
    }

    private String calculateUseroutput(String s) {
        //TODO: Useroutput anpassen
        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void run() {

        //TODO: Java Client Code einfuegen

    }

    public void start() {
        mainActivity.setClientRunning(1);
        t = new Thread(this);
        t.start();
    }

}
