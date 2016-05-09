package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import server.Server;

public class Main {

	//TODO: Update Zeit einstellbar machen
	public static void main(String[] args) { ///home/david/Servertest
		Server server1;
		try {
			File outFile;
			File inFile;
			if (args.length != 1) {
				throw new IllegalArgumentException("Takes single argument: Path to directory");
			} else {
				File f = new File(args[0]);
				if (!f.isDirectory()) {
					throw new IllegalArgumentException("Path must point to a directory");
				} else {
					outFile = new File(f.getAbsolutePath() + File.separator + "AnforderungsStatus.xml");
					inFile = new File(f.getAbsolutePath() + File.separator + "VerfuegbarkeitsStatus.xml");
					if (!outFile.exists())
						outFile.createNewFile();
					else
						System.out.println("OutFile Exists");
					if (!inFile.exists())
						inFile.createNewFile();
					else
						System.out.println("InFile Exists");
				}
			}

			server1 = new Server(inFile, outFile);
			server1.start();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
		}
	}

}
