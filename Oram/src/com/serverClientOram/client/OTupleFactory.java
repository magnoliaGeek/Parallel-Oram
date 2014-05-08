package com.serverClientOram.client;

import java.util.ArrayList;

public class OTupleFactory {

	private int alpha;
	private int bsize;

	public OTupleFactory(int alpha, int bsize) {
		this.alpha = alpha;
		this.bsize = bsize;
	}

	public OTuple createOTuple(int b, int leaf){
		ArrayList<OValue> vals = new ArrayList<OValue>();
		for(int i=0;i<alpha;i++){
			byte[] bytes = new byte[bsize];
			OValue v = new OValue(bytes,bsize);
			v.writeInt(-1);
			vals.add(v);
		}
		return new OTuple(b,leaf,alpha,bsize,vals);
	}
	public OTuple createOTupleFromByteArr(ArrayList<Byte> byteArrL){
		//first get the b field of the tuple from first Ovalue
		byte[] bByteArr = new byte[bsize];
		for(int i=0;i<bsize;i++){
			bByteArr[i] = byteArrL.get(i);
		}
		OValue bOValue = new OValue(bByteArr,bsize);
		int b = bOValue.toInt();
		ArrayList<OValue> vals = new ArrayList<OValue>();
		
		//Add the alpha OValues
		for(int i=0;i<alpha;i++){
			byte[] bytes = new byte[bsize];
			for(int j=0;j<bsize;j++){
				bytes[j] = byteArrL.get((i+1)*bsize+j);
			}
			vals.add(new OValue(bytes,bsize));
		}
		
		//add the leaf field of the tuple as the last value
		byte[] leafByteArr = new byte[bsize];
		for(int i=0;i<bsize;i++){
			leafByteArr[i] = byteArrL.get((alpha+1)*bsize+i);
		}
		OValue leafOValue = new OValue(leafByteArr,bsize);
		int leaf = leafOValue.toInt();
		
		//construct and return the tuple
		return new OTuple(b,leaf,alpha,bsize,vals);
	}

	public ArrayList<Byte> getBytesFromTuple(OTuple t) {
		ArrayList<Byte> res = new ArrayList<Byte>();
		
		//add bytes from b field
		OValue bOValue = new OValue(t.b,bsize);
		for(int i=0;i<bsize;i++){
			res.add(bOValue.bytes[i]);
		}
		
		//add bytes from OValue array
		for(OValue v: t.vals){
			for(int i=0;i<bsize;i++){
				res.add(v.bytes[i]);
			}
		}
		
		//add bytes from leaf field
		OValue leafOValue = new OValue(t.leaf,bsize);
		for(int i=0;i<bsize;i++){
			res.add(leafOValue.bytes[i]);
		}
		return res;
	}
}
