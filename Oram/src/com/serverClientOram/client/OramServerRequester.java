package com.serverClientOram.client;

import java.util.ArrayList;

public interface OramServerRequester {
	public void writeBytes(ArrayList<Integer> adds, int nodeSize,
			ArrayList<Byte> bytes);
	public ArrayList<Byte> readBytes(ArrayList<Integer> adds, int nodeSize);
}
