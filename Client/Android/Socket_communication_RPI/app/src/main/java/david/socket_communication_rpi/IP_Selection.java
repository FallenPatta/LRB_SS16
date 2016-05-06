package david.socket_communication_rpi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
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

import android.os.Vibrator;

public class IP_Selection extends AppCompatActivity {

    private void saveToFile(String fname, String content) {
        FileOutputStream fos = null;

        Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);

        if (isSDPresent) {
            try {
                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Heizung-IP/";
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
            File file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/Heizung-IP/" + fname + ".txt");

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

    Client client1;
    private int clientRunning = 0;
    private String serverRequest = "OK";
    private IP_Selection mainReference = this;
    private boolean isSending = false;
    private Thread keepAlive;

    public void setIsSending(boolean b) {
        isSending = b;
    }

    public void setClientRunning(int val) {
        clientRunning = val;
    }

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

    /*private void getSynchronized() {
        while (client1.isRunning() | isSending) {
            synchronized (client1.lock) {
                try {
                    client1.lock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }*/

    public void recolor2(int numb) {
        TextView cTV = (TextView) findViewById(R.id.color_indicator);
        cTV.setBackgroundResource(R.drawable.gradienten);
        cTV.getBackground().setLevel(numb);
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

    public void send(String s) {
        final String str = s;
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (client1.lock) {
                    isSending = true;
                    serverRequest = str;
                    client1.setRequest(serverRequest);
                    System.out.println("Request: " + client1.getRequest());
                    client1.start();
                }
            }
        }).start();
        //sending.start();
    }

    private boolean multiSending = false;
    private List<String> commandArray = Collections.synchronizedList(new ArrayList<String>());

    //Commands must be separated by Space
    public void multiSend(String s) {
        System.out.println("ISIT?: " + multiSending);
        if (!multiSending) {
            multiSending = true;
            final String str = s;
            final String[] commands = str.split(" ");
            for (int i = 0; i < commands.length; i++) {
                commandArray.add(0, commands[i]);
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!(commandArray.size() == 0)) {
                        System.out.println("Getting synched");
                        client1.getSynchronized();
                        synchronized (client1.lock) {
                            String sendStr = commandArray.get(commandArray.size() - 1);
                            commandArray.remove(commandArray.size() - 1);
                            if (clientRunning == 1) {
                                send(sendStr);
                            }
                            else {
                                client1.lock.notify();
                            }
                        }
                    }
                    if (clientRunning == 0) isSending = false;
                    multiSending = false;
                }
            }).start();
            //sending.start();
        } else {
            final String str = s;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String[] commands = str.split(" ");
                    for (int i = 0; i < commands.length; i++) {
                        //commandArray.add(0, commands[i]);
                    }
                }
            }).start();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip__selection);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final EditText ipField = (EditText) findViewById(R.id.ip_text);
        final TextView updateText = (TextView) findViewById(R.id.update_text);
        final TextView colorInd = (TextView) findViewById(R.id.color_indicator);
        final Button statBtn = (Button) findViewById(R.id.status_btn);

        colorInd.getBackground().setLevel(2);

        int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        if (actionBarTitleId > 0) {
            TextView title = (TextView) findViewById(actionBarTitleId);
            if (title != null) {
                title.setTextColor(Color.BLACK);
            }
        }

        updateText.setSelected(true);

        //updateText.setMovementMethod(new ScrollingMovementMethod());
        serverRequest = "OK";

        //client1 = new Client("127.0.0.1", 50007, updateText, serverRequest, statusText, mainReference);
        client1 = new Client("127.0.0.1", 50007, updateText, serverRequest, mainReference, colorInd);
        clientRunning = 0;
        //WAIT(500);
        String savedIP = readFile("HeizungIP");
        if (validIP(savedIP)) {
            ipField.setText(savedIP);
            client1 = new Client(savedIP, 50007, updateText, serverRequest, mainReference, colorInd);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    WAIT(100);
                    while(client1.isRunning()){
                        WAIT(100);
                    }
                    multiSend("SendStatus");
                }
            }).start();
        }

        colorInd.setOnClickListener(new View.OnClickListener() {
            Context context = getApplicationContext();

            @Override
            public void onClick(View v) {
                Vibrator vib = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
                vib.vibrate(30);
            }
        });

        findViewById(R.id.this_ip_btn).setOnClickListener(new View.OnClickListener() {
            Context context = getApplicationContext();

            @Override
            public void onClick(View v) {
                String ipAddress = wifiIpAddress(context);
                if (validIP(ipAddress)) {
                    updateText.setText(ipAddress);
                } else {
                    updateText.setText("Keine geeignete IP Adresse.");
                }
            }
        });

        findViewById(R.id.start_client_btn).setOnClickListener(new View.OnClickListener() {
            Context context = getApplicationContext();

            @Override
            public void onClick(View v) {
                String deviceIP = ipField.getText().toString();

                final String devIP = deviceIP;

                Thread reconnectThread = new Thread(new Runnable() {

                    private void ToastMessage(String s) {
                        try {
                            final String inUI = s;
                            mainReference.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mainReference.getApplicationContext(), inUI, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Exception e) {
                        }
                    }

                    @Override
                    public void run() {
                        client1.getSynchronized();
                        if (clientRunning == 0) {
                            if (!validIP(devIP)) {
                                ToastMessage("Bitte eine valide IPv4 Adresse eingeben.");
                            } else {
                                Vibrator vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                                vib.vibrate(50);
                                ToastMessage("Verbindung wird neu gestartet.");
                                client1 = new Client(devIP, 50007, updateText, serverRequest, mainReference, colorInd);
                            }
                        } else {
                            ToastMessage("Verbindung wird geprüft.");
                            multiSend("OK");
                            mainReference.WAIT(50);
                            if (clientRunning == 1) {
                                ToastMessage("Es besteht bereits eine Verbindung. Wenn diese nicht nutzbar scheint hilft wahrscheinlich ein Neustart.");
                            } else {
                                ToastMessage("Keine Verbindung gefunden. Verbindung wird neu gestartet.");
                                mainReference.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        recolor2(2);
                                    }
                                });
                                if (!validIP(devIP)) {
                                    ToastMessage("Bitte eine valide IPv4 Adresse eingeben.");
                                } else {
                                    client1 = new Client(devIP, 50007, updateText, serverRequest, mainReference, colorInd);
                                }
                            }
                        }
                    }
                });
                reconnectThread.start();
            }
        });

        findViewById(R.id.start_btn).setOnClickListener(new View.OnClickListener() {
            Context context = getApplicationContext();

            @Override
            public void onClick(View v) {
                if (clientRunning == 0) {
                    Toast.makeText(context, "Es besteht keine Verbindung.", Toast.LENGTH_SHORT).show();
                } else {
                    multiSend("TurnOn");
                    Vibrator vib = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
                    vib.vibrate(30);
                }
            }
        });

        findViewById(R.id.stop_btn).setOnClickListener(new View.OnClickListener() {
            Context context = getApplicationContext();

            @Override
            public void onClick(View v) {
                client1.stopThread();
                client1.closeConnection();
                clientRunning = 0;
                Toast.makeText(context, "Client closed", Toast.LENGTH_SHORT).show();

                Vibrator vib = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
                vib.vibrate(30);

                //Restarts Application
                Intent mStartActivity = new Intent(context, IP_Selection.class);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);

                finish();
                System.exit(0);
            }
        });

        findViewById(R.id.save_btn).setOnClickListener(new View.OnClickListener() {
            Context context = getApplicationContext();

            @Override
            public void onClick(View v) {
                if (validIP(ipField.getText().toString())) {
                    removeFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Heizung-IP/HeizungIP.txt");
                    saveToFile("HeizungIP", ipField.getText().toString());
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

        findViewById(R.id.status_btn).setOnClickListener(new View.OnClickListener() {
            Context context = getApplicationContext();

            @Override
            public void onClick(View v) {
                if (clientRunning == 0) {
                    Toast.makeText(context, "Nicht verbunden.", Toast.LENGTH_SHORT).show();
                } else if (client1.isRunning()) {
                } else {
                    multiSend("SendStatus");
                    Vibrator vib = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
                    vib.vibrate(30);
                }
            }
        });

        findViewById(R.id.sendalot).setOnClickListener(new View.OnClickListener() {
            Context context = getApplicationContext();

            @Override
            public void onClick(View v) {
                if (clientRunning == 0) {
                    Toast.makeText(context, "Nicht verbunden.", Toast.LENGTH_SHORT).show();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i<1000; i++)multiSend("SendStatus");
                        }
                    }).start();
                    Vibrator vib = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
                    vib.vibrate(30);
                }
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

        keepAlive = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    if (clientRunning == 1)multiSend("SendStatus");
                    WAIT(1000);
                }
            }
        });
        keepAlive.start();

        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "IP_Selection Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://david.socket_communication_rpi/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        client1.stopThread();
        client1.closeConnection();
        client1 = null;
        clientRunning = 0;
        finish();
        System.exit(0);

        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "IP_Selection Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://david.socket_communication_rpi/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
