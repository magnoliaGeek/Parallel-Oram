package com.serverClientOram.client;

import java.util.ArrayList;

public class OTreeNode {
	//node id start with 1! The parent nodeId is simply nodeId/2
	ArrayList<OTuple> ots;
	int k;

	OTreeNode(int k,ArrayList<OTuple> ots) { 
		// TODO Auto-generated constructor stub
		this.k = k;
		this.ots = ots;
	}


	public ArrayList<OTuple> getTuples() {
		// TODO Auto-generated method stub
		return ots;
	}

	public void insertTup(OTuple tup) throws OverflowException {
		boolean inserted = false;
		for(int i=0;i<k;i++){
			if(ots.get(i).b == -1){
				ots.set(i, new OTuple(tup));
				inserted = true;
				break;
			}
		}
		if(!inserted){
			throw new OverflowException();
		}
	}

}
