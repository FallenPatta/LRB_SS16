package clientThread;

import java.net.*;
import java.io.*;

public class ClientThread implements Runnable {

	private Thread t;
	private Socket client;
	private StringBuilder outBuilder;
	private Integer index;
	boolean isDead;
	
	public ClientThread(Socket clientSocket, Integer ind){
		super();
		this.client = clientSocket;
		this.index = ind;
		this.isDead = false;
	}
	
	public boolean isConnected(){
		if(this.isDead) return !this.isDead;
		if(client.isClosed()) return !client.isClosed();
		return true;
	}
	
	@Override
	public void run() {
		
		System.out.println("STARTING");
		
		int portNumber = 50007;
		int totalMsgs = 0;
		boolean waterstatus = false;
		
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
			    		outBuilder = new StringBuilder("Status ");
			    		outBuilder.append(totalMsgs);
			    		outBuilder.append(" Messages");
			    		outBuilder.append(" Wasser:");
			    		if(waterstatus){
			    			outBuilder.append("An");
			    		}else{
			    			outBuilder.append("Aus");
			    		}
			    		outputLine = outBuilder.toString();
			    		System.out.println("Sending Status: " + outputLine);
			    	}else if(inputLine.equals("TurnOn")){
			    		outBuilder = new StringBuilder("Status ");
			    		outBuilder.append(totalMsgs);
			    		outBuilder.append(" Messages");
			    		outBuilder.append(" Wasser:");
			    		waterstatus = !waterstatus;
			    		if(waterstatus){
			    			outBuilder.append("An");
			    			System.out.println("Wasser: an");
			    		}else{
			    			outBuilder.append("Aus");
			    			System.out.println("Wasser: aus");
			    		}
			    		outputLine = outBuilder.toString();
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
