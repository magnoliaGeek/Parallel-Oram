package com.serverClientOram.server;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

public class DiskOperator implements StorageOperator {
	// int size;
	int nodeSize;
	static int limit = 1024 * 1024 * 1024; // 1G
	String folder;
	public DiskOperator(String folder, int nodeSize){
		this.folder = folder;
		this.nodeSize = nodeSize;
	}
	@Override
	public void set(int[] addresses, int length, byte[] content) {
		// TODO Auto-generated method stub
		for (int i = 0; i < addresses.length; i++) {
			try {
				FileUtils.writeByteArrayToFile(
						new File(folder + File.separator + "adds"
								+ addresses[i]),
						Arrays.copyOfRange(content, i * length, (i + 1)
								* length));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public byte[] get(int[] addresses, int length) {
		// TODO Auto-generated method stub
		int j = 0;
		byte[] content = new byte[length * addresses.length];
		try {
			for (int i = 0; i < addresses.length; i++) {
				Path path = Paths.get(folder + File.separator + "adds"
						+ addresses[i]);
				byte[] readBytes = Files.readAllBytes(path);
				for (int t = 0; t < length; t++) {
					content[j] = readBytes[t];
					j++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return content;
	}

	@Override
	public void init(int sizeOram) {
		// TODO Auto-generated method stub
		int numFiles = sizeOram / nodeSize;
		if (sizeOram > limit)
			throw new RuntimeException(new DiskLimitException());
		byte[] initBytes = new byte[nodeSize];
		// initialize write "-1" to every 4 byte array
		byte[] empty = ByteBuffer.allocate(4).putInt(-1).array();
		for (int i = 0; i < initBytes.length; i++) {
			initBytes[i] = empty[i % 4];
		}
		for (int i = 0; i < numFiles; i++) {
			try {
				File file = new File(folder + File.separator + "adds" + i
						* nodeSize);
				if (!file.exists()) {
					file.createNewFile();
				}
				FileUtils.writeByteArrayToFile(file, initBytes);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
