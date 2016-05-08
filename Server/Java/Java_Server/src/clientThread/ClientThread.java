package clientThread;

import java.net.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.io.*;
import server.Server;

public class ClientThread implements Runnable {

	private Thread t;
	private Socket client;
	private Server parent;
	private StringBuilder outBuilder;
	private Integer index;
	private boolean isDead;
	private int portNumber = 50007;
	private int totalMsgs = 0;
	private SocketAddress adr = null;
	
	private void WAIT(int millis){
		try{
			Thread.sleep(millis);
		}catch(Exception e){
			
		}
	}
	
	public ClientThread(Socket clientSocket, Server server, Integer ind){
		super();
		this.client = clientSocket;
		this.index = ind;
		this.isDead = false;
		this.parent = server;
		this.adr = this.client.getRemoteSocketAddress();
	}
	
	public SocketAddress getAdr(){
		return this.adr;
	}
	
	public boolean isConnected(){
		if(this.isDead) return !this.isDead;
		if(client.isClosed()) return !client.isClosed();
		return true;
	}
	
	private String buildStatus(){
		outBuilder = new StringBuilder("<Status>");
		outBuilder.append("<Messages>");
		outBuilder.append(totalMsgs);
		outBuilder.append("</Messages>");
		outBuilder.append("<Wasser>");
		if(parent.isWaterstatus()){
			outBuilder.append("An");
		}else{
			outBuilder.append("Aus");
		}
		outBuilder.append("</Wasser>");
		outBuilder.append("<Temperatur>");
		if(parent.getWasserTemp() <= 255) outBuilder.append(Integer.toString(parent.getWasserTemp()));
		else outBuilder.append(Integer.toString(0));
		outBuilder.append("</Temperatur>");
		outBuilder.append("</Status>");
		return outBuilder.toString();
	}
	
	@Override
	public void run() {
		
		System.out.println("STARTING");
		
		try ( 
			    PrintWriter out = new PrintWriter(client.getOutputStream(), true);
			    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				Scanner sc = new Scanner(client.getInputStream());
			) {
				System.out.println("connected");
				client.setSoTimeout(10000);
			    String inputLine, outputLine;
			    inputLine = "";
			    outputLine = "connected";
			    //outputLine = condense(outputLine, totalMsgs);
			    out.println(outputLine);
			    totalMsgs++;
			    
			    long lastRead = System.currentTimeMillis();
			    
			    while (!this.isDead) {
			    	if(client.getInputStream().available() > 0){
			    		lastRead = System.currentTimeMillis();
			    		while(client.getInputStream().available() > 0) inputLine += sc.next();
			    		
			    		//System.out.println("Input: " + inputLine);
			    	
				    	if(inputLine.contains("TurnOn")){
				    		int lastTon = inputLine.lastIndexOf("TurnOn") + "TurnOn".length();
				    		parent.updateAnforderung(this.index, client.getRemoteSocketAddress().toString());
				    		outputLine = buildStatus();
				    		out.println(outputLine);
				    		inputLine = inputLine.substring(lastTon);
				    		totalMsgs++;
				    	}else if(inputLine.contains("SendStatus")){
				    		int lastStat = inputLine.lastIndexOf("SendStatus") + "SendStatus".length();
				    		outputLine = buildStatus();
				    		out.println(outputLine);
				    		inputLine = inputLine.substring(lastStat);
				    		totalMsgs++;
				    	}
				    	else if (inputLine.contains("\n")){
				    	}
			    	}
			    	if(System.currentTimeMillis() - lastRead > 5000){
			    		break;
			    	}
			    	WAIT(20);
			    }
			    
			}catch(Exception e){
				System.out.println(e.getMessage());
				System.out.println("Client Crashed!");
			}
			try {
				this.isDead = true;
				this.client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	public void start(){
		t = new Thread(this);
		t.start();
	}
	
	public void disconnect(){
		this.isDead = true;
		try {
			this.client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String toString(){
		return this.index.toString();
	}

}
