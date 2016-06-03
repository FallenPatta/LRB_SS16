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
    private int portNumber = 5000;
    private String hostName = "127.0.0.1";
    private IP_Selection mainActivity;
    private Socket mainSocket;
    private String message;

    //public Client(String host, int pNum, TextView lm, String req, TextView infoView, IP_Selection mainAct) {
    public Client(String host, int pNum, IP_Selection mainAct) {
        hostName = host;
        portNumber = pNum;
        mainActivity = mainAct;
    }

    public void connectSocket(String msg) {

        try {
            mainSocket = new Socket(hostName, portNumber);
            mainSocket.setSoTimeout(5000);
            message = msg;
        }
        catch (Exception e){
            ToastMessage(e.getMessage());
            e.printStackTrace();
        }

        start();
    }

    public void setHostName(String host){
        if(validIP(host)){
            this.hostName = host;
        }
    }

    public void setPortNumber(int port){
        if(port <= 65535) this.portNumber = port;
    }

    private void WAIT(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {

        }
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

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void run() {

        //TODO: Java Client Code einfuegen

    }

    public void start() {
        t = new Thread(this);
        t.start();
    }

}
