package com.serverClientOram.client;

import java.util.ArrayList;

public class OTreeNodeFactory {

	private int k;
	private int alpha;
	private int bsize;
	private OTupleFactory otf;

	public OTreeNodeFactory(int k, int alpha, int bsize) {
		this.k = k;
		this.alpha = alpha;
		this.bsize = bsize;
		otf = new OTupleFactory(alpha,bsize);
	}

	public ArrayList<OTreeNode> createOTreeNodesArrFromBytes(
			ArrayList<Byte> bytes) {
		ArrayList<OTreeNode> otns = new ArrayList<OTreeNode>();
		int nodeSize = k*(alpha+2)*bsize;
		int numOfNodes = bytes.size()/nodeSize;
		for(int i=0;i<numOfNodes;i++){
			ArrayList<Byte> oTreeNodeBytes = new ArrayList<Byte>(bytes.subList(i*nodeSize, (i+1)*nodeSize));
			OTreeNode otn = createOTreeNodeFromByteArr(oTreeNodeBytes);
			otns.add(otn);
		}
		
		return otns;
	}

	public OTreeNode createOTreeNodeFromOTupleArr(ArrayList<OTuple> ots) {
		return new OTreeNode(k,ots);
	}

	public OTreeNode createOTreeNodeFromByteArr(ArrayList<Byte> bytes) {
		ArrayList<OTuple> ots = new ArrayList<OTuple>();
		for(int i=0;i<k;i++){
			ArrayList<Byte> tupleBytes = new ArrayList<Byte>(bytes.subList(i*(alpha+2)*bsize, (i+1)*(alpha+2)*bsize));
			OTuple ot = otf.createOTupleFromByteArr(tupleBytes);
			ots.add(ot);
		}
		return new OTreeNode(k,ots);
	}

	public ArrayList<Byte> getBytesFromTreeNodesArr(ArrayList<OTreeNode> tnodes) {
		ArrayList<Byte> res = new ArrayList<Byte>();
		for(OTreeNode n: tnodes){
			res.addAll(getBytesFromTreeNode(n));
			//System.out.printf("This node has %d ovalues\n",n.ots.size());
		}
		return res;
	}

	public ArrayList<Byte> getBytesFromTreeNode(OTreeNode n) {
		// TODO Auto-generated method stub
		ArrayList<Byte> res = new ArrayList<Byte>();
		for(OTuple t: n.ots){
			res.addAll(otf.getBytesFromTuple(t));
		}
		return res;
	} 
}
