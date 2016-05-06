package clientThread;

import java.net.*;
import java.util.Iterator;
import java.io.*;

public class ClientThread implements Runnable {

	private Thread t;
	private Socket client;
	private StringBuilder outBuilder;
	private Integer index;
	private boolean waterstatus = false;
	private int wasserTemp = 0;
	private boolean isDead;
	private int portNumber = 50007;
	private int totalMsgs = 0;
	
	private void WAIT(int millis){
		try{
			Thread.sleep(millis);
		}catch(Exception e){
			
		}
	}
	
	public ClientThread(Socket clientSocket, Integer ind){
		super();
		this.client = clientSocket;
		this.index = ind;
		this.isDead = false;
		
		
		Thread checkForDead = new Thread(new Runnable(){
			public void run(){
				while(!isDead){
					if(waterstatus){
						if(wasserTemp < 255) wasserTemp++;
					}else if(wasserTemp > 0){
						wasserTemp--;
					}
					WAIT(250);
				}
			}
		});
		
		checkForDead.start();
		
	}
	
	public boolean isConnected(){
		if(this.isDead) return !this.isDead;
		if(client.isClosed()) return !client.isClosed();
		return true;
	}
	
	private String buildStatus(){
		outBuilder = new StringBuilder("Status ");
		outBuilder.append(totalMsgs);
		outBuilder.append(" Messages");
		outBuilder.append(" Wasser:");
		if(waterstatus){
			outBuilder.append("An");
		}else{
			outBuilder.append("Aus");
		}
		outBuilder.append("<Temperatur>");
		outBuilder.append(Integer.toString(wasserTemp));
		outBuilder.append("</Temperatur>");
		return outBuilder.toString();
	}
	
	@Override
	public void run() {
		
		System.out.println("STARTING");
		
		try ( 
			    PrintWriter out = new PrintWriter(client.getOutputStream(), true);
			    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			) {
				System.out.println("connected");
				client.setSoTimeout(10000);
			    String inputLine, outputLine;
			    
			    outputLine = "connected";
			    //outputLine = condense(outputLine, totalMsgs);
			    out.println(outputLine);
			    totalMsgs++;
			    
			    while (!this.isDead & (inputLine = in.readLine()) != null) {
			    	System.out.println("Input: " + inputLine);
			    	
			    	if(inputLine.equals("SendStatus")){
			    		outputLine = buildStatus();
			    		System.out.println("Sending Status: " + outputLine);
			    	}else if(inputLine.equals("TurnOn")){
			    		waterstatus = !waterstatus;
			    		outputLine = buildStatus();
			    		System.out.println("Sending Status: " + outputLine);
			    	}
			    	else{
				        outputLine = "OK";
				        System.out.println("Sending OK: " +  outputLine);
			    	}
			    	
			        out.println(outputLine);
			        totalMsgs++;
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
