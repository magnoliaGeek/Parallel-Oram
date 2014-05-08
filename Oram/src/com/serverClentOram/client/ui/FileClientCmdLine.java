package com.serverClentOram.client.ui;

import java.io.File;
import java.util.Scanner;

import com.serverClientOram.client.BlockOramClient;
import com.serverClientOram.client.FileClient;
import com.serverClientOram.client.OramFileClient;
import com.serverClientOram.client.PBlockOramClient;
import com.serverClientOram.client.POramFileClient;

public class FileClientCmdLine {

	public static void main(String[] args) {
		/*
		 * String url = "http://localhost:8000/oram"; int tupSize = 64;
		 * BlockOramClient boc = new BlockOramClient(2, 16, 16, tupSize, 8,url);
		 * OramFileClient ofc = new OramFileClient(boc); ofc.uploadFile(
		 * "C:/Users/Chen/Documents/cornell/documents/startup-tickets.pdf");
		 * System.out.println("file uploaded");
		 * ofc.downloadFile("startup-tickets.pdf"
		 * ,"C:/Users/Chen/Documents/cornell/documents/startup-tickets2.pdf" );
		 * System.out.println("file downloaded");
		 */
		FileClient ofc;
		String url = "http://"+args[0]+"/oram";
		if (args.length == 3 && args[1].equals("-p")) {
			int numRequest = Integer.parseInt(args[2]);
			PBlockOramClient pboc = new PBlockOramClient(numRequest, 1,
					1024 * 4, 16, 64, 8, url);
			ofc = new POramFileClient(pboc);
			System.out.printf("Parallel mode, %d request per batch.\n",numRequest);
		} else {
			BlockOramClient boc = new BlockOramClient(1, 1024 * 4, 16, 64, 8,
					url);
			ofc = new OramFileClient(boc);
		}
		StringBuilder usageBuider = new StringBuilder();
		usageBuider.append("Usage:\n");
		usageBuider.append("ls: list all the files in the server\n");
		usageBuider.append("upload [FILE]: upload a file to the server\n");
		usageBuider
				.append("download [FILE] [DESTINATION]: download a file from the server to a specified path\n");
		usageBuider.append("exit: quit from the program\n");
		String usage = usageBuider.toString();
		System.out.printf("Connection to Oram Server %s established!\n", url);
		System.out.println(usage);
		String workingDir = System.getProperty("user.dir");

		Scanner inputReader = new Scanner(System.in);

		while (true) {
			System.out.print("OFTP_shell>");
			String[] tokens = inputReader.nextLine().split("\\s+");
			if (tokens[0].equalsIgnoreCase("EXIT")) {
				System.out.println("Bye!");
				break;
			} else if (tokens[0].equals("ls")) {
				for (String f : ofc.getFileIndex().keySet()) {
					System.out.println(f);
				}
			} else if (tokens[0].equals("upload") && tokens.length == 2) {
				File f = new File(tokens[1]);
				if (!f.exists()) {
					f = new File(workingDir + File.pathSeparator + tokens[1]);
				}
				if (!f.exists()) {
					System.out.printf("Couldn't find %s!\n", tokens[1]);
					continue;
				}
				long t1 = System.currentTimeMillis();
				ofc.uploadFile(f.getAbsolutePath());
				long eps = (System.currentTimeMillis() - t1) / 1000;
				System.out.printf("File %s uploaded, using %d seconds\n",
						tokens[1], eps);
			} else if (tokens[0].equals("download") && tokens.length == 3) {
				if (ofc.getFileIndex().containsKey(tokens[1])) {
					long t1 = System.currentTimeMillis();
					ofc.downloadFile(tokens[1], tokens[2]);
					long eps = (System.currentTimeMillis() - t1) / 1000;
					System.out.printf(
							"File %s downloaded to %s, using %d seconds\n",
							tokens[1], tokens[2], eps);
				} else {
					System.out.printf("File %s doesn't exist!\n", tokens[1]);
				}
			} else {
				System.out.println(usage);
			}

		}
	}
}
