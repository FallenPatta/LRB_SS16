package main;
import java.io.FileNotFoundException;
import java.io.IOException;

import server.Server;

public class Main {
	
	public static void main(String[] args){
		
		Server server1;
		try {
			server1 = new Server();
			server1.start();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
