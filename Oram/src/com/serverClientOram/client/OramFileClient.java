package com.serverClientOram.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;

public class OramFileClient implements FileClient{
	BlockOramClient boc;
	Map<String, ArrayList<Integer>> fileIndex;//FileName: size , otuple1,otuple2,otuple3......
	int tupCounter;
	int tupByteSize; //the size of a logical tuple, which is smaller that the actual size because we need 2 more fileds:leaf and address
	public OramFileClient(BlockOramClient boc){
		this.boc = boc;
		tupCounter = 0;
		tupByteSize = boc.bsize * boc.alpha;
		fileIndex = new HashMap<String, ArrayList<Integer>>();
	}
	@Override
	public void uploadFile(String filePath) {
		// TODO Auto-generated method stub
		Path path = Paths.get(filePath);
		byte[] contentBytes;
		
		try {
			contentBytes = Files.readAllBytes(path);
			//System.out.print(new String(contentBytes));
			String fileName = path.getFileName().toString();
			System.out.println(fileName);
			int fileSize = contentBytes.length;
			System.out.println(fileSize);
			ArrayList<Integer> sizeAndTups = new ArrayList<Integer>();
			sizeAndTups.add(fileSize);
			fileIndex.put(fileName, sizeAndTups);
			int numTups = (int) Math.ceil((contentBytes.length+ 0.0) / (boc.bsize*boc.alpha));
			int numVals = numTups*boc.alpha;
			ArrayList<OValue> ovs = new ArrayList<OValue>();
			//write file content to array of OValues;
			for (int i = 0; i < numVals; i++) {
				byte[] valByte = new byte[boc.bsize];
				for (int j = 0; j < boc.bsize; j++) {
					if(i*boc.bsize + j < contentBytes.length){
						valByte[j] = contentBytes[i * boc.bsize + j];
					}
					else{
						valByte[j] = 0;
					}
				}
				OValue val = new OValue(valByte, boc.bsize);
				ovs.add(val);
			}
			//write otuples to the oram and store the indexes of these tuples;
			for (int i = 0; i < numTups; i++) {
				ArrayList<OValue> vals = new ArrayList<OValue>(ovs.subList(i
						* boc.alpha, (i + 1) * boc.alpha));
				OTuple ot = new OTuple(0, 0, 0, 0, vals);
				boc.writeTuple((tupCounter+i)*boc.alpha, ot);
				fileIndex.get(fileName).add(tupCounter+i);
				//System.out.printf("%d tuple written!\n", i);
			}
			tupCounter += numTups;
		} catch (IOException | OverflowException e) {
			e.printStackTrace();
		}
		
		
	}

	@Override
	public void downloadFile(String fileName, String destPath) {
		byte[] content = downloadFile(fileName);
		//System.out.print(new String(content));
		try {
			FileUtils.writeByteArrayToFile(
					new File(destPath), content);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public byte[] downloadFile(String fileName) {
		int fileSize = fileIndex.get(fileName).get(0);
		int numTups = fileIndex.get(fileName).size()-1;
		byte[] res = new byte[fileSize];
		for (int i = 0; i <numTups ; i++) {
			OTuple rot;
			try {
				rot = boc.readTuple(fileIndex.get(fileName).get(i+1) * boc.alpha);
				for (int j=0;j<boc.alpha;j++) {
					//System.out.print(new String(v.bytes));
					//System.out.print(new String(rot.vals.get(j).bytes));
					for(int k=0;k<boc.bsize;k++){
						if(i*tupByteSize+j*boc.bsize+k<fileSize){
							res[i*tupByteSize+j*boc.bsize+k] = rot.vals.get(j).bytes[k];
						}
					}
				}
			} catch (OverflowException e) {
				e.printStackTrace();
			}
			
		}
		return res;
	}
	
	public static void main(String[] args){
		/*String url = "http://localhost:8000/oram";
		int tupSize = 64;
		BlockOramClient boc = new BlockOramClient(2, 16, 16, tupSize, 8,url);
		OramFileClient ofc = new OramFileClient(boc);
		ofc.uploadFile("C:/Users/Chen/Documents/cornell/documents/startup-tickets.pdf");
		System.out.println("file uploaded");
		ofc.downloadFile("startup-tickets.pdf","C:/Users/Chen/Documents/cornell/documents/startup-tickets2.pdf" );
		System.out.println("file downloaded");*/
		String url = args[0];
		BlockOramClient boc = new BlockOramClient(1, 1024, 16, 64, 8,url);
		OramFileClient ofc = new OramFileClient(boc);
		StringBuilder usageBuider = new StringBuilder();
		usageBuider.append("Usage:\n");
		usageBuider.append("ls: list all the files in the server\n");
		usageBuider.append("upload [FILE]: upload a file to the server\n");
		usageBuider.append("download [FILE] [DESTINATION]: download a file from the server to a specified path\n");
		usageBuider.append("exit: quit from the program\n");
		String usage = usageBuider.toString();
		System.out.printf("Connection to OServer %s established!\n",url);
		System.out.println(usage);
		String workingDir = System.getProperty("user.dir");
		
	     
        Scanner inputReader = new Scanner(System.in);
       
        while(true){
        	System.out.print("OFTP_shell>");
        	String[] tokens = inputReader.nextLine().split("\\s+");
        	if(tokens[0].equalsIgnoreCase("EXIT")){
        		System.out.println("Bye!");
        		break;
        	}
        	else if(tokens[0].equals("ls")){
        		for(String f:ofc.fileIndex.keySet()){
        			System.out.println(f);
        		}
        	}
        	else if(tokens[0].equals("upload") && tokens.length==2){
        		File f = new File(tokens[1]);
        		if(!f.exists()){
        			f = new File(workingDir + File.pathSeparator + tokens[1]);
        		}
        		if(!f.exists()){
        			System.out.printf("Couldn't find %s!\n", tokens[1]);
        			continue;
        		}
        		long t1 = System.currentTimeMillis();
        		ofc.uploadFile(f.getAbsolutePath());
        		long eps = (System.currentTimeMillis()-t1)/1000;
        		System.out.printf("File %s uploaded, using %d seconds\n", tokens[1],eps);
        	}
        	else if(tokens[0].equals("download") && tokens.length==3){
        		if(ofc.fileIndex.containsKey(tokens[1])){
        			long t1 = System.currentTimeMillis();
        			ofc.downloadFile(tokens[1],tokens[2]);
        			long eps = (System.currentTimeMillis()-t1)/1000;
            		System.out.printf("File %s downloaded to %s, using %d seconds\n", tokens[1],tokens[2],eps);
        		}
        		else{
        			System.out.printf("File %s doesn't exist!\n", tokens[1]);
        		}
        	}
        	else{
        		System.out.println(usage);
        	}
        		
        }
	}
	@Override
	public Map<String, ArrayList<Integer>> getFileIndex() {
		return fileIndex;
	}
}
