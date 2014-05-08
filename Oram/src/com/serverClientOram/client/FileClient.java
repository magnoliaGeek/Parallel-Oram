package com.serverClientOram.client;

import java.util.ArrayList;
import java.util.Map;

public interface FileClient {
	public void uploadFile(String filePath);
	public void downloadFile(String fileName, String destPath);
	public byte[] downloadFile(String fileName);
	public Map<String,ArrayList<Integer>> getFileIndex();
}
