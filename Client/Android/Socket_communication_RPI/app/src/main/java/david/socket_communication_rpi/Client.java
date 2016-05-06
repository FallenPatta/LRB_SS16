package david.socket_communication_rpi;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.RadialGradient;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client implements Runnable {
    Thread t;
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
    private int [] colors = {0,0,0,0,0,0,0,0,0};
    public Object lock = new Object();
    public final Lock mutex = new ReentrantLock(true);

    //public Client(String host, int pNum, TextView lm, String req, TextView infoView, IP_Selection mainAct) {
    public Client(String host, int pNum, TextView lm, String req, IP_Selection mainAct, TextView indicator) {
        colors = new int[]{0,0,0,0,0,0,0,0,0};
        hostName = host;
        portNumber = pNum;
        latestMessage = lm;
        indication = indicator;
        //screenInfo = infoView;
        setRequest(req);
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

    private void getSynchronized() {
        while(isRunning()) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void stopThread() {
        runThread = false;
    }

    private void connectSocket() {
        boolean connecting = true;

        getSynchronized();

        runThread = true;

        synchronized (lock) {
            while (connecting) {
                try {
                    mainSocket = new Socket(hostName, portNumber);
                    outWriter = new PrintWriter(mainSocket.getOutputStream(), true);
                    inReader = new BufferedReader(new InputStreamReader(mainSocket.getInputStream()));
                    String fromServer;
                    mainSocket.setSoTimeout(3000);
                    while ((fromServer = inReader.readLine()) != null & connecting) {
                        if (fromServer != null) {
                            //UPoutput(fromServer);
                        }
                        WAIT(250);
                        outWriter.println("OK");

                        while ((fromServer = inReader.readLine()) != null) {
                            if (fromServer.equals("connected")) {
                            } else if (fromServer.equals("OK")) {

                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        indication.setBackgroundColor(Color.argb(255, 0, 255, 80));
                                        recolor2(3);
                                    }
                                });
                                connecting = false;
                                mainActivity.setClientRunning(1);

                                stopThread();
                                lock.notify();
                                return;
                            }
                        }
                    }
                    System.out.println("CONNECTED");
                    if (fromServer == null) {
                        mainActivity.setClientRunning(0);
                        mainSocket.close();
                        stopThread();
                        lock.notify();
                        return;
                    }
                } catch (UnknownHostException e) {
                    System.out.println("Unknown host: " + hostName);
                    //UPinfo("Could not reach...");
                    mainActivity.setClientRunning(0);
                    stopThread();
                    lock.notify();
                    return;

                } catch (IOException e) {
                    System.out.println("No I/O");
                    //UPinfo("Input/Output-Error");
                    mainActivity.setClientRunning(0);
                    stopThread();
                    lock.notify();
                    return;
                }
            }
        }
        System.out.println("ENDING");

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

    private void WAIT(int millis) {
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

    private void recolor2(int numb) {
        final IP_Selection here = mainActivity;
        final TextView there = indication;
        final int num = numb;
        here.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                there.setBackgroundResource(R.drawable.gradienten);
                there.getBackground().setLevel(num);
            }
        });
    }

//    private void UPinfo(String s) {
//        try {
//            final String inUI = s;
//            mainActivity.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    screenInfo.setText(inUI);
//                }
//            });
//        } catch (Exception e) {
//        }
//    }

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
        String[] contents = s.split(" ");
        if (contents[0].equals("Status")) {
            return true;
        }
        return false;
    }

    private String calculateUseroutput(String s) {
        StringBuilder sb = new StringBuilder("");
        String[] outputs = s.split(" ");
        boolean statfound = false;
        int statNum = -1;
        for (int i = 0; i < outputs.length; i++) {
            String[] msgsearch = outputs[i].split(":");
            if (msgsearch.length == 2) {
                if (msgsearch[0].equals("Wasser")) {
                    statfound = true;
                    statNum = i;
                    sb.append(msgsearch[0]);
                    sb.append(": ");
                    sb.append(msgsearch[1]);
                    sb.append(" ");

                    if (msgsearch[1].equals("An")) {
                        recolor2(0);
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Button stb = (Button)mainActivity.findViewById(R.id.start_btn);
                                stb.setText("Wasser Ausschalten");
                            }
                        });
                        //recolor(250, 40, 0);
                    } else if (msgsearch[1].equals("Aus")) {
                        recolor2(1);
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Button stb = (Button) mainActivity.findViewById(R.id.start_btn);
                                stb.setText("Wasser Anschalten");
                            }
                        });
                    }

                }
            }
        }
        for (int i = 0; i < outputs.length; i++) {
            if (i != statNum) {
                sb.append(outputs[i]);
                if (outputs[i].equals("Status")) {
                    sb.append(":");
                }
                sb.append(" ");
            }
        }

        return sb.toString();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void run() {
        synchronized (lock) {
            try {
                //PrintWriter out = new PrintWriter(mainSocket.getOutputStream(), true);
                //BufferedReader in = new BufferedReader(new InputStreamReader(mainSocket.getInputStream()));
                PrintWriter out = outWriter;
                BufferedReader in = inReader;
                String fromServer;
                String fromUser = request;
                if (fromUser != null) {
                    System.out.println("USERMESSAGE: " + fromUser);
                    out.println(fromUser);
                }
                while ((fromServer = in.readLine()) != null) {

                    if (fromServer.equals("OK")) {
                        stopThread();
                        mainActivity.setIsSending(false);
                        lock.notify();
                        return;
                    } else if (gotStatus(fromServer)) {
                        String toUI = calculateUseroutput(fromServer);
                        //UPoutput(fromServer);
                        UPoutput(toUI);
                        request = "OK";
                    } else {
                        request = "OK";
                    }

                    //recolor();

                    fromUser = request;

                    if (fromUser != null) {
                        out.println(fromUser);
                    } else {
                        System.out.println("NULL FROM USER");
                        stopThread();
                        mainActivity.setIsSending(false);
                        lock.notify();
                        return;
                    }
                }
                if (fromServer == null) {
                    recolor2(2);
                    try {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                WAIT(200);
                                for(int i = 0; i < 2; i++){
                                    Vibrator vib = (Vibrator) mainActivity.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                                    vib.vibrate(200);
                                    WAIT(300);
                                }
                            }
                        }).start();
                        ToastMessage("Verbindung abgebrochen");
                        Uri not2 = RingtoneManager.getActualDefaultRingtoneUri(mainActivity.getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone r = RingtoneManager.getRingtone(mainActivity.getApplicationContext(), not2);
                        r.play();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mainActivity.setClientRunning(0);
                    stopThread();
                    mainActivity.setIsSending(false);
                    lock.notify();
                    return;
                }
                System.out.println("SOMETHING WENT WRONG");
            } catch (IOException e) {
                System.out.println("No I/O");
                try {
                    mainSocket.close();
                } catch (IOException f) {
                }
                connectSocket();
                run();
                stopThread();
                return;
            }
            runThread = false;
            mainActivity.setIsSending(false);
            lock.notify();
        }
    }

    public void start() {
        runThread = true;
        t = new Thread(this);
        t.start();
    }

}
