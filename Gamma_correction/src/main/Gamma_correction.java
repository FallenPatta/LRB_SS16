package main;

import java.lang.Math;

public class Gamma_correction {

	public static void main(String[] args) {

		double gamma   = 2.35;
		int   max_in  = 255,
		      max_out = 255;
 
	  System.out.print("const uint8_t gammatable[] = {");
	  for(int i=0; i<=max_in; i++) {
	    if(i > 0) System.out.print(',');
	    if((i & 15) == 0) System.out.print("\n  ");
	    for(int j = 3; j>=Integer.toString((int)(Math.pow((double)i / (double)max_in, gamma) * max_out + 0.5)).length(); j--)
	    	System.out.print(" ");
	    System.out.print((int)(Math.pow((double)i / (double)max_in, gamma) * max_out + 0.5));
	  }
	  System.out.println(" };");

	}

}
