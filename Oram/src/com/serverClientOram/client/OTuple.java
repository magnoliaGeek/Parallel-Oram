package com.serverClientOram.client;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class OTuple {
	
	//The logical address 'b' of the tuple 
	public int b;
	ArrayList<OValue> vals;
	int alpha;
	int bsize;
	
	//The leaf for this tuple
	public Integer leaf;

	public OTuple(int b, int leaf, int alpha, int bsize, ArrayList<OValue> vals) {
		// TODO Auto-generated constructor stub
		this.b = b;
		this.leaf = leaf;
		this.alpha = alpha;
		this.bsize = bsize;
		this.vals = vals;
	}
	public OTuple(OTuple ot){
		b = ot.b;
		leaf = ot.leaf;
		alpha = ot.alpha;
		bsize = ot.bsize;
		vals = new ArrayList<OValue>();
		for(OValue val:ot.vals){
			vals.add(val);
		}
		
	}

	public void updateVal(int i, OValue val) {
		// TODO Auto-generated method stub
		vals.get(i).write(val);
		
	}

	public void updateLeaf(int newLeaf) {
		// TODO Auto-generated method stub
		leaf = newLeaf;
	}
	
	public void printVals(){
		System.out.print(b + ":");
		for(OValue v: vals){
			System.out.print(v.toInt() + ",");
		}
		System.out.println(leaf + ":");
	}

	public OValue getVal(int i) {
		// TODO Auto-generated method stub
		return vals.get(i);
	}

}
