package server;

import java.net.*;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.management.modelmbean.XMLParseException;

import java.io.*;
import clientThread.ClientThread;

public class Server implements Runnable {
	Thread t;
	StringBuilder outBuilder;
	private boolean waterstatus = false;
	private int wasserTemp = 0;

	private File outFile;
	private File inFile;

	public CopyOnWriteArrayList<ClientThread> clients;

	public Server(File in, File out) throws FileNotFoundException, IOException {
		clients = new CopyOnWriteArrayList<ClientThread>();
		outFile = out;
		inFile = in;

		if (!outFile.exists()) {
			try {
				outFile.createNewFile();
			} catch (IOException e) {
				throw new IOException(e.toString() + "\nOUTPUT File could not be created.");
			}
		}

		if (!inFile.exists()) {
			throw new FileNotFoundException(
					"INPUT File could not be found.\nMake sure you are creating it in the same directory as the output File.");
		}

		Thread updateWater = new Thread(new Runnable() {
			public void run() {
				while (true) {
					if (waterstatus) {
						if (wasserTemp < 255)
							wasserTemp++;
					} else if (wasserTemp > 0) {
						wasserTemp--;
					}
					WAIT(100);
				}
			}
		});

		Thread statusThread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						FileReader r = new FileReader(inFile);
						Scanner sc = new Scanner(r);
						String stat = "";
						while (sc.hasNext())
							stat += sc.next();
						int lastStat[] = { stat.lastIndexOf("<Status>"), stat.lastIndexOf("</Status>") };
						if (lastStat[0] >= lastStat[1])
							throw new XMLParseException("Could not parse last Status\nXML Format is not compatible\n"
									+ "Make sure you are writing into the correct file" + lastStat[0] + "!"
									+ lastStat[1]);
						String lastStatus = stat.substring(lastStat[0], lastStat[1]);
						if (lastStatus.lastIndexOf("<WasserStatus>an</WasserStatus>") > lastStatus
								.lastIndexOf("<WasserStatus>aus</WasserStatus>"))
							waterstatus = true;
						else
							waterstatus = false;

						int itemp[] = { lastStatus.lastIndexOf("<Temperatur>"),
								lastStatus.lastIndexOf("</Temperatur>") };
						if (itemp[0] > 0 && itemp[1] > itemp[0]) {
							itemp[0] += "<Temperatur>".length();
							try {
								int tmp = Integer.parseInt(lastStatus.substring(itemp[0], itemp[1]));
								if (tmp < 0 | tmp > 255)
									throw new IllegalArgumentException(
											"Watertemperature Value must be integer between 0 and 255\nwas: "
													+ Integer.toHexString(tmp));
								wasserTemp = tmp;
							} catch (NumberFormatException e) {
								throw new IllegalArgumentException(
										"Watertemperature Value must be integer between 0 and 255\nwas: NAN\nCheck Format");
							}
						}

					} catch (XMLParseException | FileNotFoundException e) {
						e.printStackTrace();
					}
					WAIT(25000);
				}
			}
		});

		statusThread.start();
		// updateWater.start();
	}

	public boolean isWaterstatus() {
		return waterstatus;
	}

	public void setWaterstatus(boolean waterstatus) {
		this.waterstatus = waterstatus;
	}

	public int getWasserTemp() {
		return wasserTemp;
	}

	private void WAIT(int millis) {
		try {
			Thread.sleep(millis);
		} catch (Exception e) {

		}
	}

	public boolean updateAnforderung(int clientID, String clientIP) throws FileNotFoundException {
		try {
			FileWriter out = new FileWriter(outFile);
			if (!outFile.exists()) {
				out.close();
				throw new FileNotFoundException("OUTPUT File does not exist");
			}
			LocalDateTime cTime = LocalDateTime.now();
			out.write("<Anforderung>" + "<Zustand>an</Zustand>" + "<Zeitpunkt>" + "<Jahr>" + cTime.getYear() + "</Jahr>"
					+ "<Tag>" + cTime.getDayOfYear() + "</Tag>" + "<Stunde>" + cTime.getHour() + "</Stunde>"
					+ "<Minute>" + cTime.getMinute() + "</Minute>" + "<Sekunde>" + cTime.getSecond() + "</Sekunde>"
					+ "</Zeitpunkt>" + "<MaskedClientID>" + clientID + "</MaskedClientID>" + "<ClientIP>" + clientIP
					+ "</ClientIP>" + "</Anforderung>");
			out.flush();
			setWaterstatus(true);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void run() {
		int portNumber = 50007;
		Integer numClients = 0;

		Thread checkForDead = new Thread(new Runnable() {
			public void run() {
				while (true) {
					Iterator<ClientThread> clientIterator = clients.iterator();
					while (clientIterator.hasNext()) {
						ClientThread check = clientIterator.next();
						if (!check.isConnected()) {
							SocketAddress ad = check.getAdr();
							System.out.println("Client with index: " + check.toString() + " and Address: "
									+ ad.toString() + " decayed");
							check.disconnect();
							clients.remove(check);
						}
					}
					WAIT(5000);
				}
			}
		});

		checkForDead.start();

		while (true) {
			System.out.println("connecting...");
			try (ServerSocket serverSocket = new ServerSocket(portNumber);) {
				System.out.println("Opening Socket Nr.:" + numClients);
				Socket clientSocket = serverSocket.accept();
				ClientThread client = new ClientThread(clientSocket, this, numClients);
				clients.add(0, client);
				clients.get(0).start();
				numClients++;

			} catch (Exception e) {
				System.out.println("could not connect...");
				WAIT(500);
			}
		}

	}

	public void start() {
		t = new Thread(this);
		t.start();
	}

}
