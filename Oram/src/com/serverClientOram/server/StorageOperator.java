package com.serverClientOram.server;

import java.nio.ByteBuffer;

public interface StorageOperator {
	public void set(int[] addresses, int length, byte[] content);

    public byte[] get(int[] addresses, int length);

    public void init(int sizeOram);
}
