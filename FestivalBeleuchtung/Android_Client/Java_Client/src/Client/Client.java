package Client;

import java.net.*;
import java.util.Random;
import java.util.Scanner;
import java.lang.*;
import java.io.*;

public class Client implements Runnable {
	Thread t;
//	Thread answerThread;
	int pingDelay = 1000;
	String hostName = "192.168.0.2";
	Socket clientsoc;
	boolean updaterRunning = false;

	public Client() {
	}

	private void WAIT(int millis) {
		try {
			Thread.sleep(millis);
		} catch (Exception e) {

		}
	}

	long abs(long num) {
		return num >= 0 ? num : -num;
	}

	private String timedRead(Scanner sc, int timeout) {
		StringBuilder ret = new StringBuilder();
		long start = System.currentTimeMillis();
		while (sc.hasNext() & abs(System.currentTimeMillis() - start) < timeout) {
			String next = sc.next();
			if (next.equals("\n") | next.equals("\r"))
				break;
			ret.append(next);
		}
		return !ret.toString().isEmpty() ? ret.toString() : null;
	}

	private String hexColor(int r, int g, int b) {
		StringBuilder sb = new StringBuilder("");
		String rHex = Integer.toHexString(r).toUpperCase();
		String gHex = Integer.toHexString(g).toUpperCase();
		String bHex = Integer.toHexString(b).toUpperCase();
		if (rHex.indexOf("0x") >= 0)
			rHex = rHex.substring(rHex.lastIndexOf("0x") + 2);
		if (gHex.indexOf("0x") >= 0)
			gHex = gHex.substring(gHex.lastIndexOf("0x") + 2);
		if (bHex.indexOf("0x") >= 0)
			bHex = bHex.substring(bHex.lastIndexOf("0x") + 2);

		if (rHex.length() < 2)
			rHex = "0" + rHex;
		if (gHex.length() < 2)
			gHex = "0" + gHex;
		if (bHex.length() < 2)
			bHex = "0" + bHex;
		
		sb.append(rHex);
		sb.append(gHex);
		sb.append(bHex);

		return sb.toString();
	}

	@Override
	public void run() {
		int portNumber = 5000;
		int totalMsgs = 0;
		Random r = new Random();
		
		while (true) {
			System.out.println("start");
			try(
					Socket socket = new Socket(hostName, portNumber);
					PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
				clientsoc = socket;
				clientsoc.setSoTimeout(5000);
				System.out.println("Server found on IP: ");
				System.out.println(clientsoc.getInetAddress().getHostAddress());
				
				while(updaterRunning);
				
				Thread updater = new Thread (new Runnable(){
					public void run(){
						updaterRunning = true;
						while(clientsoc != null){
							int red = r.nextInt(256);
							int green = r.nextInt(256);
							int blue = r.nextInt(256);
							switch(r.nextInt(3)){
							case 0:
								red = 255;
								break;
							case 1:
								green = 255;
								break;
							case 2:
								blue = 255;
								break;
							}
//							String fromUser = hexColor(r.nextInt(150-60)+60,r.nextInt(150-60)+60,r.nextInt(150-60)+60);
							String fromUser = hexColor(red, green, blue);
							System.out.println(fromUser);
							if (fromUser != null) {
								out.print(fromUser);
								out.print("\n");
								out.flush();
							}
							WAIT(1000);
						}
						updaterRunning = false;
					}
				});
				updater.start();

				String fromServer;
				long msgTime = System.currentTimeMillis();
				while (clientsoc != null) {
					if((fromServer = in.readLine()) == null) clientsoc = null;
					System.out.println("Server: " + fromServer);
					totalMsgs++;
					if (abs(System.currentTimeMillis() - msgTime) > 1000) {
						msgTime = System.currentTimeMillis();
						totalMsgs = 0;
					}
				}
				updater.join();

			} catch (Exception e) {
				e.printStackTrace(System.out);
				clientsoc = null;
			}

			WAIT(500);
		}
	}

	public void start(int millis, String host) {
		pingDelay = millis;
		t = new Thread(this);
		t.start();
	}

}
