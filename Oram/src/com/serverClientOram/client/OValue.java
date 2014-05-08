package com.serverClientOram.client;

import java.nio.ByteBuffer;

public class OValue {
	byte[] bytes;
	int bsize;
	
	//constructor from an int value
	public OValue(int i, int bsize) {
		// TODO Auto-generated constructor stub
		bytes = new byte[bsize];
		this.bsize = bsize;
		this.writeInt(i);
	}
	
	public OValue(byte[] bytes, int bsize){
		this.bytes = bytes;
		this.bsize = bsize;
	}
	
	public Integer toInt(){
		//get the first 4 byte
		byte[] intArr = new byte[4];
		for(int i=0;i<4;i++){
			intArr[i] = bytes[i];
		}
		return ByteBuffer.wrap(intArr).getInt();
	}
	
	void writeInt(Integer val){
		byte[] intArr = ByteBuffer.allocate(4).putInt(val).array();
		for(int i=0;i<4;i++){
			bytes[i] = intArr[i];
		}
	}
	
	public void write(OValue oval) {
		// write the oval from another to this one
		for(int i=0;i<bytes.length;i++){
			bytes[i] = oval.bytes[i];
		}
		
	}
}
