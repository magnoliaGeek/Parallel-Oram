package com.serverClientOram.client;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

public class POramFileClient implements FileClient {
	PBlockOramClient boc;
	Map<String, ArrayList<Integer>> fileIndex;// FileName: size ,
												// otuple1,otuple2,otuple3......
	int tupCounter;
	int tupByteSize; // the size of a logical tuple, which is smaller that the
						// actual size because we need 2 more fileds:leaf and
						// address

	public POramFileClient(PBlockOramClient boc) {
		this.boc = boc;
		tupCounter = 0;
		tupByteSize = boc.bsize * boc.alpha;
		fileIndex = new HashMap<String, ArrayList<Integer>>();
	}

	@Override
	public void uploadFile(String filePath) {
		Path path = Paths.get(filePath);
		/*int byteSize = 4096;
		byte[] contentBytes = new byte[byteSize];
		ArrayList<byte[]> letterbytes = new ArrayList<byte[]>();
		for (int i = 0; i < byteSize/256; i++) {
			byte[] charArr = ByteBuffer.allocate(2).putChar((char) ('A' + i))
					.array();
			letterbytes.add(charArr);
			for (int j = i * 256; j < (i + 1) * 256; j++) {
				contentBytes[j] = letterbytes.get(i)[j % 2];
			}
		}*/

		try {
			byte[] contentBytes = Files.readAllBytes(path);
			//System.out.println(new String(contentBytes));
			String fileName = path.getFileName().toString();
			System.out.println(fileName);
			int fileSize = contentBytes.length;
			System.out.println(fileSize);
			ArrayList<Integer> sizeAndTups = new ArrayList<Integer>();
			sizeAndTups.add(fileSize);
			fileIndex.put(fileName, sizeAndTups);
			int numTups = (int) Math.ceil((contentBytes.length + 0.0)
					/ (boc.bsize * boc.alpha));
			// number of parallel request batches
			int numBatch = (int) Math.ceil((numTups + 0.0) / boc.numRequest);
			int numVals = numBatch * boc.numRequest * boc.alpha;
			ArrayList<OValue> ovs = new ArrayList<OValue>();
			// write file content to array of OValues;
			for (int i = 0; i < numVals; i++) {
				byte[] valByte = new byte[boc.bsize];
				for (int j = 0; j < boc.bsize; j++) {
					if (i * boc.bsize + j < contentBytes.length) {
						valByte[j] = contentBytes[i * boc.bsize + j];
					} else {
						valByte[j] = 0;
					}
				}
				OValue val = new OValue(valByte, boc.bsize);
				ovs.add(val);
			}
			// write otuples to the oram and store the indexes of these tuples;
			ArrayList<Boolean> wops = new ArrayList<Boolean>();
			for (int i = 0; i < boc.numRequest; i++) {
				wops.add(true);
			}
			for (int h = 0; h < numBatch; h++) {
				ArrayList<OTuple> ots = new ArrayList<OTuple>();
				ArrayList<Integer> adds = new ArrayList<Integer>();
				for (int i = 0; i < boc.numRequest; i++) {
					ArrayList<OValue> vals = new ArrayList<OValue>(ovs.subList(
							(h*boc.numRequest + i) * boc.alpha, (h*boc.numRequest+ i + 1) * boc.alpha));
					OTuple ot = new OTuple(0, 0, 0, 0, vals);
					ots.add(ot);
					adds.add((tupCounter + h * boc.numRequest + i) * boc.alpha);
					fileIndex.get(fileName).add(
							tupCounter + h * boc.numRequest + i);
					// System.out.printf("%d tuple written!\n", i);
				}
				boc.pBlockOramIO(adds, ots, wops);
			}
			tupCounter += numBatch * boc.numRequest;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void downloadFile(String fileName, String destPath) {
		byte[] content = downloadFile(fileName);
		//System.out.println(new String(content));
		try {
			FileUtils.writeByteArrayToFile(new File(destPath), content);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public byte[] downloadFile(String fileName) {
		int fileSize = fileIndex.get(fileName).get(0);
		int numTups = fileIndex.get(fileName).size() - 1;
		int numBatch = numTups / boc.numRequest;
		byte[] res = new byte[fileSize];
		ArrayList<Boolean> rops = new ArrayList<Boolean>();
		for (int i = 0; i < boc.numRequest; i++) {
			rops.add(false);
		}
		try {
			int frontier = 0;
			for (int h = 0; h < numBatch; h++) {
				ArrayList<OTuple> ots = new ArrayList<OTuple>();
				ArrayList<Integer> adds = new ArrayList<Integer>();
				for (int i = 0; i < boc.numRequest; i++) {
					ots.add(null);
					int tupId = fileIndex.get(fileName).get(
							h * boc.numRequest + i + 1);
					adds.add(tupId * boc.alpha);
					//System.out.print(tupId + " ");
				}
				//System.out.println();
				boc.pBlockOramIO(adds, ots, rops);
				for (OTuple rot : ots) {
					for (int j = 0; j < boc.alpha; j++) {
						// System.out.print(new String(v.bytes));
						//System.out.print(new String(rot.vals.get(j).bytes));
						for (int k = 0; k < boc.bsize; k++) {
							if (frontier + j * boc.bsize + k < fileSize) {
								res[frontier + j * boc.bsize + k] = rot.vals
										.get(j).bytes[k];
							}
						}
					}
					frontier += tupByteSize;
					//System.out.println();
					//System.out.println(frontier);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	public static void main(String[] args) {
		String url = "http://10.33.46.51:8000/oram";
		int tupSize = 128;
		PBlockOramClient boc = new PBlockOramClient(16, 1, 1024, 16, tupSize, 8,
				url);
		POramFileClient ofc = new POramFileClient(boc);
		ofc.uploadFile("C:/Users/Chen/Documents/cornell/documents/MedicalError.jpg");
		ofc.uploadFile("C:/Users/Chen/Documents/cornell/documents/startup-tickets.pdf");
		System.out.println("file uploaded");
		
		ofc.downloadFile("MedicalError.jpg",
				"C:/Users/Chen/Documents/cornell/documents/MedicalError2.jpg");
		ofc.downloadFile("startup-tickets.pdf",
				"C:/Users/Chen/Documents/cornell/documents/startup-tickets2.pdf");
		
		
		System.out.println("file downloaded");
	}

	@Override
	public Map<String, ArrayList<Integer>> getFileIndex() {
		return fileIndex;
	}

}
