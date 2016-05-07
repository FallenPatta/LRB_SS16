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
    Thread heartbeat;
    Thread watchdog;
    private boolean heartBeating = false;
    private long avail;
    private int wasserStatus;
    private int portNumber = 50007;
    private String hostName = "127.0.0.1";
    private String request = "OK";
    private boolean runThread;
    private TextView latestMessage;
    private TextView indication;
    //private TextView screenInfo;
    private IP_Selection mainActivity;
    private Socket mainSocket;
    private PrintWriter outWriter;
    private BufferedReader inReader;
    private List<String> commandArray = Collections.synchronizedList(new ArrayList<String>());
    public Object lock = new Object();
    public final Lock mutex = new ReentrantLock(true);

    //public Client(String host, int pNum, TextView lm, String req, TextView infoView, IP_Selection mainAct) {
    public Client(String host, int pNum, TextView lm, String req, IP_Selection mainAct, TextView indicator) {
        hostName = host;
        portNumber = pNum;
        latestMessage = lm;
        indication = indicator;
        //screenInfo = infoView;
        setRequest(req);
        mainActivity = mainAct;
        wasserStatus = 0;
        avail = 0;
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

    public boolean connected(){
        return (!mainSocket.isClosed() & mainSocket.isConnected());
    }

    public void stopThread() {
        runThread = false;
    }

    private void connectSocket() {
        runThread = true;
        mainActivity.setClientRunning(1);

        try {
            mainSocket = new Socket(hostName, portNumber);
            outWriter = new PrintWriter(mainSocket.getOutputStream(), true);
            inReader = new BufferedReader(new InputStreamReader(mainSocket.getInputStream()));
            String fromServer;
            mainSocket.setSoTimeout(5000);
        } catch (UnknownHostException e) {
            System.out.println("Unknown host: " + hostName);
            UPoutput("Could not reach host");
            mainActivity.setClientRunning(0);
            stopThread();
            return;
        } catch (IOException e) {
            System.out.println("No I/O");
            UPoutput("Connection broke");
            mainActivity.setClientRunning(0);
            stopThread();
            return;
        }

        start();

        heartbeat = new Thread(new Runnable() {
            @Override
            public void run() {
                if(!heartBeating) {
                    heartBeating = true;
                    while (isRunning()) {
                        if(commandArray.size() == 0) appendMessage("SendStatus", false);
                        WAIT(250);
                    }
                }
            }
        });
        heartbeat.start();

        watchdog = new Thread(new Runnable() {
            @Override
            public void run() {
                timeoutThread();
            }
        });
        watchdog.start();

        System.out.println("ENDING");
    }

    public void appendMessage(String s, boolean front){
        if(front){
            this.commandArray.add(0, s);
            return;
        }
        this.commandArray.add(s);
    }

    public void setRequest(String req) {
        request = req;
    }

    public String getRequest() {
        return request;
    }

    public boolean isRunning() {
        return runThread;
    }

    public void closeConnection() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    request = "disconnect";
                    run();
                    mainSocket.close();
                } catch (IOException e) {
                }
            }
        });
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

    private boolean colorEqual(int[] a, int[]b){
        for(int i = 0; i<9; i++) if(a[i] != b[i]) return false;
        return true;
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
                    double norm = (double) wasserStatus / 255.0;
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

    private boolean gotStatus(String s) {
        if(s.contains("<Status>") & s.contains("</Status>")) return true;
        return false;
    }

    private String getStatus(String s){

        if(gotStatus(s)){
            int start = s.indexOf("<Status>");
            int end = s.indexOf("</Status>");
            return s.substring(start+"<Status>".length(), end);
        }

        return null;
    }

    private String calculateUseroutput(String s) {
        StringBuilder sb = new StringBuilder("");
        int statNum = -1;
        if (s.contains("<Wasser>")) {
            int begin = s.indexOf("<Wasser>");
            begin += "<Wasser>".length();
            int end = s.indexOf("</Wasser>");
            String wStatus = s.substring(begin, end);
            sb.append("Wasser: " + wStatus);
        }

        if(s.contains("<Temperatur>")){
            int begin = s.indexOf("<Temperatur>");
            begin += "<Temperatur>".length();
            int end = s.indexOf("</Temperatur>");
            String tStatus = s.substring(begin, end);
            sb.append(" - Temperatur: ");
            System.out.println("TSTAT: " + tStatus);
            try{
                int temp = Integer.parseInt(tStatus);
                wasserStatus = temp;
                if(wasserStatus < 100) sb.append("Kalt");
                if(wasserStatus >= 100 & wasserStatus < 200) sb.append("Warm");
                if(wasserStatus >= 200 & wasserStatus <= 255) sb.append("Heiß");
                recolor2(temp);
            } catch (NumberFormatException e){
            }
        }

        return sb.toString();
    }

    private void timeoutThread() {
        while (isRunning()) {
            WAIT(5000);
            if (avail == 0) {
                recolor2(2);
                try {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            WAIT(200);
                            for (int i = 0; i < 2; i++) {
                                Vibrator vib = (Vibrator) mainActivity.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                                vib.vibrate(200);
                                WAIT(300);
                            }
                        }
                    }).start();
                    ToastMessage("Verbindung abgebrochen\nNeustartversuch");
                    Uri not2 = RingtoneManager.getActualDefaultRingtoneUri(mainActivity.getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(mainActivity.getApplicationContext(), not2);
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mainActivity.setClientRunning(0);
                stopThread();
                try {
                    t.join();
                    heartbeat.join();
                }catch(InterruptedException e){
                }
                connectSocket();
                return;
            } else avail = 0;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void run() {
        System.out.println("STARTING: " + isRunning());
        PrintWriter out = outWriter;

        Scanner inScanner = null;
        try{
        inScanner = new Scanner(mainSocket.getInputStream());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return;
        }

        String fromServer = "";
        String fromUser = "";

        while (isRunning()) {

            try {
                if(mainSocket.getInputStream().available()>0) avail+=mainSocket.getInputStream().available();
                while (mainSocket.getInputStream().available() > 0) {
                    fromServer += inScanner.next();
                }
            } catch (IOException e) {
            System.out.println(e.getMessage());
            return;
            }

            if (!fromServer.isEmpty()) {
                String singleMessage = getStatus(fromServer);
                if (singleMessage != null) {
                    String toUI = calculateUseroutput(singleMessage);
                    UPoutput(toUI);
                    fromServer = fromServer.substring(fromServer.indexOf("</Status>") + "</Status>".length());
                }
                else if (fromServer.contains("\n")){
                    fromServer = "";
                }
            }

            while (commandArray.size() > 0) {
                System.out.println(commandArray.size());
                fromUser = commandArray.get(0);
                commandArray.remove(0);
                System.out.println("USERMESSAGE: " + fromUser);
                out.println(fromUser);
            }
            WAIT(20);
        }
    }

    public void start() {
        runThread = true;
        mainActivity.setClientRunning(1);
        t = new Thread(this);
        t.start();
    }

}
