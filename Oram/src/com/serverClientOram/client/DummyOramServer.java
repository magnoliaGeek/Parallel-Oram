package com.serverClientOram.client;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DummyOramServer implements OramServerRequester {
	int size;
	byte[] ram;
	int delay = 0;

	public DummyOramServer(int sizeOram) {
		this.size = sizeOram;
		this.ram = new byte[sizeOram];
		// initialize
		byte[] empty = ByteBuffer.allocate(4).putInt(-1).array();
		for (int i = 0; i < size; i++) {
			ram[i] = empty[i % 4];
		}
	}

	public void writeBytes(ArrayList<Integer> adds, int nodeSize,
			ArrayList<Byte> bytes) {
		for (int i = 0; i < adds.size(); i++) {
			for (int j = 0; j < nodeSize; j++) {
				ram[adds.get(i) + j] = bytes.get(i * nodeSize + j);
			}
		}
		if (delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public ArrayList<Byte> readBytes(ArrayList<Integer> adds, int nodeSize) {
		ArrayList<Byte> res = new ArrayList<Byte>();
		for (int i = 0; i < adds.size(); i++) {
			for (int j = 0; j < nodeSize; j++) {
				res.add(ram[adds.get(i) + j]);
			}
		}
		if (delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
		return res;
	}
}
