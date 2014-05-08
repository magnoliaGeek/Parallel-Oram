package com.serverClientOram.client;

public class OValueFactory {

	private int bsize;

	public OValueFactory(int bsize) {
		this.bsize = bsize;
	}

	public OValue createOValue(int i) {
		// TODO Auto-generated method stub
		return new OValue(i, bsize);
	}

}
