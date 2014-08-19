package com.serverClientOram.client;

import java.util.ArrayList;
import java.util.Random;

public class OramClient {
	static final int EMPTY = -1;
	OramServerRequester dos;
	int layers,alpha,k,bsize,nodeSize;
	ArrayList<OramTree> trees;
	Random rd;
	OTreeNodeFactory otnf;
	OTupleFactory otf;
	OValueFactory ovf;
	int[] cpuMap;
	static int log2(Integer n){ 
		int res=0;
		while(n > 1<<res){
			res++;
		}
		return res;
	}
	public OramClient(int layers, int sizeCpuMap, int k, int alpha, int bsize, String url){
		//calculate the number of space needed for the server
		//each tree node contains k tuple
		//each tuple's size is bsize*(alpha+2),in which the 2 extra cell
		//is used to store the leaf position and logical position
		//bsize is usually set as 8=(4+4) for ( r||f_sk(r) xor val) form 
		//OTP with PRF encryption, the size of the actual data in a cell is 4,
		//which could be an integer.
		this.layers = layers;
		this.k = k;
		this.alpha = alpha;
		this.bsize = bsize;
		cpuMap = new int[sizeCpuMap];
		trees = new ArrayList<OramTree>();
		nodeSize = k*bsize*(alpha+2);
		otnf = new OTreeNodeFactory(k,alpha,bsize);
		otf = new OTupleFactory(alpha,bsize);
		ovf = new OValueFactory(bsize);
		
		rd = new Random();
		
		//initialize the map with random value
		//It's a little different from Chung-Pass paper, in which
		//the position map is initialized to empty
		for(int i=0; i<cpuMap.length;i++){
			cpuMap[i] = rd.nextInt(cpuMap.length);
		}
		
		//depth and start of the first tree
		int depth = log2(sizeCpuMap);
		int start = 0;
		
		for(int i=0;i<layers;i++){
			trees.add(new OramTree(start, depth, nodeSize));
			depth += log2(alpha);
			start += (2*sizeCpuMap*Math.pow(alpha, i)-1)*nodeSize;
		}
		
		//get the size of the Oram, initialize the dummy server
		//TODO: replace the dummyserver  to real server
		int sizeOram = start;
		//dos = new DummyOramServer(sizeOram);
		System.out.println("Array size:"+ sizeOram);
		if(url == null)
			dos = new DummyOramServer(sizeOram);
		else
			dos = new HttpOramServerRequester(sizeOram, url);
	}
	
	
	public OValue read(Integer add) throws OverflowException{
		return oramIO(add,null,false);
	}
	
	public void write(Integer add, OValue oval) throws OverflowException{
		oramIO(add,oval,true);
	}
	
	public OValue oramIO(Integer add, OValue oval, Boolean write) throws OverflowException{
		int[] b = new int[layers];
		for(int i=0;i<layers;i++){
			b[i] = add/(int) Math.pow(alpha,layers-i);
		}
		int leaf = cpuMap[b[0]];
		//update the map
		int newLeaf = rd.nextInt(cpuMap.length);
		cpuMap[add/(int) Math.pow(alpha,layers)] = newLeaf;
		OValue val = null;
		for(int i=0;i<layers;i++){
			//ArrayList<Integer> adds = trees.get(i).getPath(leaf);
			ArrayList<Byte> pathBytes = dos.readBytes(trees.get(i).getPath(leaf),nodeSize);
			
			//get val in this layer and replace the block with empty, now the b field of tup is -1
			OTuple tup = getTupFromPath(pathBytes, b[i]);
			//write the path back to the tree, the tup has been replaced with empty
			dos.writeBytes(trees.get(i).getPath(leaf), nodeSize,pathBytes);
			

			
			//if The position haven't been accessed before,
			//it's the first time to write in this positon.
			if(tup==null){
				tup = otf.createOTuple(b[i],newLeaf);
				val = ovf.createOValue(rd.nextInt(cpuMap.length*(int)Math.pow(alpha,i+1)));
				tup.updateVal(add/(int) Math.pow(alpha,layers-i-1) % alpha,val);
			}
			
			//rectify the b field of tup
			tup.b = b[i];

			//update the leaf for the tuple
			tup.updateLeaf(newLeaf);
			
			//get the value from the tuple
			val = tup.getVal(add/(int) Math.pow(alpha,layers-i-1) % alpha);
			
			//if the value is empty,it's the first time to access that value in this tuple
			if(val.toInt() == -1){
				val = ovf.createOValue(rd.nextInt(cpuMap.length*(int)Math.pow(alpha,i+1)));
				tup.updateVal(add/(int) Math.pow(alpha,layers-i-1) % alpha,val);
			}
			

			
			//get the leaf for the next layer if not visiting the last layer
			if(i<layers-1){
				leaf = val.toInt();
				//update the value to a new leaf in the next layer
				newLeaf = rd.nextInt(cpuMap.length*(int)Math.pow(alpha,i+1));
				val.writeInt(newLeaf);
				tup.updateVal(add/(int) Math.pow(alpha,layers-i-1) % alpha,val);
			}
			
			//In the final layer, write the value if the IO operation is 
			//write
			else{
				if(write){
					val.write(oval);
					tup.updateVal(add % alpha,val);
				}
				
			}
			
			//put the tuple to the root 
			
			putRoot(trees.get(i),tup);
			
			
			//flush
			flush(trees.get(i));
			
		}
		return val;
	}
	
	protected void flush(OramTree ot) throws OverflowException {
		Integer leaf = ot.getRandomLeaf();
		ArrayList<Byte> pathBytes = dos.readBytes(ot.getPath(leaf),nodeSize);
		ArrayList<Integer> nodeIds = ot.getIds(leaf);
		ArrayList<OTreeNode> tnodes = otnf.createOTreeNodesArrFromBytes(pathBytes);
		for(int i=0;i<tnodes.size()-1;i++){
			ArrayList<OTuple> ots = tnodes.get(i).getTuples();
			for(int j= ots.size()-1;j>=0;j--){
				if(ots.get(j).b!=-1 && (ots.get(j).leaf + (int)Math.pow(2,ot.depth)) >> (ot.depth-i-1) == nodeIds.get(i+1) ){
					tnodes.get(i+1).insertTup(ots.get(j));
					ots.get(j).b= -1;
				}
			}
			tnodes.set(i, otnf.createOTreeNodeFromOTupleArr(ots));
		}
		pathBytes = otnf.getBytesFromTreeNodesArr(tnodes);
		dos.writeBytes(ot.getPath(leaf), nodeSize,pathBytes);
	}
	
	protected void putRoot(OramTree oramTree, OTuple tup) throws OverflowException {
		OTreeNode r = getRoot(oramTree);
		r.insertTup(tup);
		putRootNode(oramTree, r);
	}
	protected void putRootNode(OramTree ot, OTreeNode r) {
		ArrayList<Byte> rootBytes = otnf.getBytesFromTreeNode(r);
		dos.writeBytes(ot.getRoot(), nodeSize,rootBytes);
	}
	protected OTreeNode getRoot(OramTree ot) {
		// TODO Auto-generated method stub
		return otnf.createOTreeNodeFromByteArr(dos.readBytes(ot.getRoot(),nodeSize));
		
	}
	protected OTuple getTupFromPath(ArrayList<Byte> pathBytes, int b) {
		// TODO Auto-generated method stub
		ArrayList<OTreeNode> tnodes = otnf.createOTreeNodesArrFromBytes(pathBytes);
		for(OTreeNode n: tnodes){
			for(OTuple t: n.getTuples()){
				if(t.b == b){
					//set the tuple to "empty"
					t.b = EMPTY;
					//update the byteArr
					ArrayList<Byte> newPathBytes = otnf.getBytesFromTreeNodesArr(tnodes);
					pathBytes.clear();
					pathBytes.addAll(newPathBytes);
					return new OTuple(t);
				}
			}
		}
		
		return null;
	}

	public static void main(String[] args) throws OverflowException{
		String url = args[4];
		int recursion = Integer.parseInt(args[0]);
		int leafNum = Integer.parseInt(args[1]);
		int securePara = Integer.parseInt(args[2]);
		int blockSize = Integer.parseInt(args[3]);
		OramClient oc = new OramClient(recursion,leafNum,securePara,blockSize/8,8,url);
		System.out.println(oc.nodeSize);
		System.out.println("ORAM initialized!");
		for(OramTree t:oc.trees){
			System.out.print(t.start);
			System.out.print(t.start+t.sizeNode*Math.pow(2, t.depth));
		}
		long t1= System.currentTimeMillis();
		for(int i=0;i<32;i++){
			oc.write(i*256,new OValue(i,8));
			System.out.println(i + " has been written!");
		}
		for(int i=0;i<32;i++){
			OValue ov = oc.read(i*256);
			int value = ov.toInt();
			System.out.println(value);
		}
		long t2 = System.currentTimeMillis();
		System.out.println(t2-t1);
	}
}
