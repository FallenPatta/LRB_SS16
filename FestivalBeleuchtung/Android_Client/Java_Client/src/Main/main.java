package Main;
import Client.Client;

public class main {
	
	public static void main(String args[]){
		if(args.length == 2){
			int delay = -1;
			try{
				delay = Integer.parseInt(args[0]);
			}catch(Exception e){
				System.out.println("Could not parse delay.");
				return;
			}
			if(delay >= 0){
				Client client1 = new Client();
				client1.start(delay, args[1]);
			}
			else{
				System.out.println("Enter valid delay.");
			}
		}
		else{
			System.out.println("Arguments: [delay, hostIP]");
		}
	}

}
