package com.serverClientOram.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class POramClient extends OramClient {
	int numRequest;
	
	public POramClient(int numRequest, int layers, int sizeCpuMap, int k,
			int alpha, int bsize, String url) {
		super(layers, sizeCpuMap, k, alpha, bsize,url);
		this.numRequest = numRequest;
	}

	public ArrayList<OValue> poramIO(ArrayList<Integer> adds,
			ArrayList<OValue> ovals, ArrayList<Boolean> write)
			throws Exception {
		// The block address for each level of map
		ArrayList<ArrayList<Integer>> bs = new ArrayList<ArrayList<Integer>>();
		//System.out.println();
		for (int l = 0; l < layers; l++) {
			bs.add(new ArrayList<Integer>());
			for (Integer addr : adds) {
				bs.get(l).add(addr / (int) Math.pow(alpha, layers - l));
			}
			//printLeaf(bs.get(l),"bs"+l);
		}
		ArrayList<Integer> leafs = new ArrayList<Integer>();
		ArrayList<Integer> newLeafs = new ArrayList<Integer>();
		
		// A Hash Map between block id and new leafs to prevent different new
		// leafs
		// for a single old block id
		Map<Integer, Integer> leafMap = new HashMap<Integer, Integer>();
		ArrayList<OValue> vals = new ArrayList<OValue>();

		for (int i = 0; i < adds.size(); i++) {
			leafs.add(cpuMap[bs.get(0).get(i)]);
			if (!leafMap.containsKey(bs.get(0).get(i))) {
				int newl = rd.nextInt(cpuMap.length);
				newLeafs.add(newl);
				leafMap.put(bs.get(0).get(i), newl);
			} else {
				newLeafs.add(leafMap.get(bs.get(0).get(i)));
			}
			vals.add(null);
		}
		//printLeaf(leafs,"leafs");
		//printLeaf(newLeafs,"new leafs");
		//update the cpuMap
		for(Integer k : leafMap.keySet()){
			cpuMap[k] = leafMap.get(k);
		}

		for (int l = 0; l < layers; l++) {
			//perform super path reading and write back, using dummy leaf to get identical ram access pattern
			
			// get val in this layer and replace the block with empty, now the b
			// field of all tups extracted is -1
			ArrayList<OTuple> tups = getTupsFromLeafs(l,leafs,
					bs.get(l));
			

			// if The position haven't been accessed before,
			// it's the first time to write in this positon.
			for (int i = 0; i < adds.size(); i++) {
				if (tups.get(i) == null) {
					/*if(l>=layers-2){
						System.out.printf("tuple %d not found from path to leaf %d\n",bs.get(l).get(i),leafs.get(i));
					}*/
					OTuple tup = otf.createOTuple(bs.get(l).get(i),
							newLeafs.get(i));
					OValue val = ovf.createOValue(rd.nextInt(cpuMap.length
							* (int) Math.pow(alpha, l + 1)));
					tup.updateVal(
							adds.get(i) / (int) Math.pow(alpha, layers - l - 1)
									% alpha, val);
					tups.set(i, tup);
					// find other tups that share the same block address with
					// this one
					for (int j = i+1; j < tups.size(); j++) {
						if (bs.get(l).get(j).equals(bs.get(l).get(i)))
							tups.set(j, tup);
					}
				}
				tups.get(i).b = bs.get(l).get(i);
				tups.get(i).updateLeaf(newLeafs.get(i));
				vals.set(
						i,
						tups.get(i).getVal(
								adds.get(i)
										/ (int) Math.pow(alpha, layers - l - 1 )
										% alpha));
				if (vals.get(i).toInt() == -1) {
					/*if(l==layers-2){
						System.out.printf("val for tuple %d is not available!\n", bs.get(layers-1).get(i));
					}*/
					vals.set(
							i,
							ovf.createOValue(rd.nextInt(cpuMap.length
									* (int) Math.pow(alpha, l + 1))));
					tups.get(i).updateVal(
							adds.get(i) / (int) Math.pow(alpha, layers - l - 1)
									% alpha, vals.get(i));
				}

			}

			// get the leaf for the next layer if not visiting the last layer
			if(l < layers - 1){
				for(int i=0; i<tups.size(); i++){
					leafs.set(i,vals.get(i).toInt());
				}
			}
			
			//Assign new leafs use a map to prevent assign different leaf for the same block.
			leafMap = new HashMap<Integer,Integer>();
			for (int i = 0; i < tups.size(); i++) {
				if (l < layers - 1) {
					// update the value to a new leaf in the next layer
					if(!leafMap.containsKey(bs.get(l+1).get(i))){
						int newl = rd.nextInt(cpuMap.length*(int)Math.pow(alpha, l+1));
						newLeafs.set(i, newl);
						leafMap.put(bs.get(l+1).get(i), newl);
					}
					else{
						newLeafs.set(i,leafMap.get(bs.get(l+1).get(i)));
					}
					
					vals.get(i).writeInt(newLeafs.get(i));
					tups.get(i).updateVal(adds.get(i) / (int) Math.pow(alpha, layers - l - 1)
							% alpha, new OValue(newLeafs.get(i),bsize));
				}

				// In the final layer, write the value if the IO operation is
				// write
				else {
					if (write.get(i)) {
						vals.get(i).write(ovals.get(i));
						//System.out.printf("write %d, leaf %d\n",ovals.get(i).toInt(),tups.get(i).leaf);
						tups.get(i).updateVal(adds.get(i) % alpha, vals.get(i));
					}
					else{
						//System.out.printf("read %d, leaf %d\n",vals.get(i).toInt(),tups.get(i).leaf);
					}

				}
				/*if(i==tups.size()-1){
					printLeaf(leafs,"leafs here");
					printLeaf(newLeafs,"new leafs here");
					for(OTuple t: tups){
						System.out.print(t.leaf+",");
					}
					System.out.print("\nValue:");
					for(OValue val:vals){
						System.out.print(val.toInt() + ",");
					}
					System.out.println();
				}*/
			}

				// put the tuple to the root

				putLevel(trees.get(l), tups);

				// flush
				flushLevel(trees.get(l));

		}
		return vals;
	}

	protected ArrayList<OTuple> getTupsFromLeafs(int layer, ArrayList<Integer> leafs,
			ArrayList<Integer> bs) {
		ArrayList<OTuple> res = new ArrayList<OTuple>();
		Set<Integer> uleafs = new TreeSet<Integer>();
		for(Integer l: leafs){
			uleafs.add(l);
		}
		while(uleafs.size()<numRequest){
			Integer l = trees.get(layer).getRandomLeaf();
			uleafs.add(l);
		}
		
		ArrayList<Integer> readNodeAdds = flatten(trees.get(layer).getSuperPath(new ArrayList<Integer>(uleafs)));
		ArrayList<Byte> readBytes = dos.readBytes(readNodeAdds, nodeSize);
		ArrayList<OTreeNode> readNodes = otnf.createOTreeNodesArrFromBytes(readBytes);
		
		//Map between node address and node
		Map<Integer,OTreeNode> nodeMap = new HashMap<Integer, OTreeNode>();
		for(int i=0;i<readNodes.size();i++){
			nodeMap.put(readNodeAdds.get(i),readNodes.get(i));
		}
		
		//get the superpath for leaf
		ArrayList<ArrayList<Integer>> usedNodeAdds = trees.get(layer).getSuperPath(leafs);
		//Map for fetched tuple to prevent double fetch for the same tuple from the super path
		Map<Integer,OTuple> fetchedTuple = new HashMap<Integer,OTuple>();
		for(int i=0;i<bs.size();i++){
			res.add(null);
			if(fetchedTuple.containsKey(bs.get(i))){
				res.set(i,fetchedTuple.get(bs.get(i)));
				continue;
			}
			for(Integer add: usedNodeAdds.get(i)){
				for(OTuple t:nodeMap.get(add).getTuples()){
					if(t.b==bs.get(i)){
						OTuple ot = new OTuple(t);
						fetchedTuple.put(bs.get(i), ot);
						res.set(i,ot);
						t.b = EMPTY;
					}
				}
			}
		}
		ArrayList<Byte> updatedBytes = otnf.getBytesFromTreeNodesArr(readNodes);
		dos.writeBytes(readNodeAdds, nodeSize, updatedBytes);
		return res;
	}

	

	protected ArrayList<Integer> flatten(ArrayList<ArrayList<Integer>> superPath) {
		ArrayList<Integer> res = new ArrayList<Integer>();
		Set<Integer> intSet = new TreeSet<Integer>();
		for(ArrayList<Integer> path: superPath){
			intSet.addAll(path);
		}
		res.addAll(intSet);
		return res;
	}


	protected void flushLevel(OramTree tree) throws Exception {
		//generate numRequest random leafs
		Set<Integer> leafs = new TreeSet<Integer>();
		while(leafs.size()<numRequest){
			Integer leaf = tree.getRandomLeaf();
			leafs.add(leaf);
		}
		ArrayList<ArrayList<Integer>> superPathAdds = tree.getSuperPath(new ArrayList<Integer>(leafs));
		ArrayList<Integer> readNodeAdds = flatten(superPathAdds);
		ArrayList<Byte> readBytes = dos.readBytes(readNodeAdds,nodeSize);
		ArrayList<OTreeNode> readNodes = otnf.createOTreeNodesArrFromBytes(readBytes);
		
		//Map between node address and position in the node array
		Map<Integer,Integer> nodeMap = new HashMap<Integer,Integer>();
		for(int i=0;i<readNodeAdds.size();i++){
			nodeMap.put(readNodeAdds.get(i), i);
		}
		
		//Create node path;
		for(ArrayList<Integer> pathAdds:superPathAdds){
			ArrayList<Integer> nodePath = new ArrayList<Integer>();
			for(Integer p: pathAdds){
				nodePath.add(nodeMap.get(p));
			}
			//flush nodePath
			for(int i=0;i<nodePath.size()-1;i++){
				ArrayList<OTuple> ots = readNodes.get(nodePath.get(i)).getTuples();
				for(int j= ots.size()-1;j>=0;j--){
					if(ots.get(j).b!=-1 && (ots.get(j).leaf + (int)Math.pow(2,tree.depth)) >> (tree.depth-i-1) == tree.getNodeId(pathAdds.get(i+1)) ){
						readNodes.get(nodePath.get(i+1)).insertTup(ots.get(j));
						ots.get(j).b= -1;
					}
				}
				readNodes.set(nodePath.get(i), otnf.createOTreeNodeFromOTupleArr(ots));
			}
			//put nodes in node path back to the node array.
			
		}
		ArrayList<Byte> updatedBytes = otnf.getBytesFromTreeNodesArr(readNodes);
		dos.writeBytes(readNodeAdds, nodeSize, updatedBytes);
	}

	protected void putLevel(OramTree tree, ArrayList<OTuple> tups) throws Exception {
		//get the node address for the level nodes, the id for these nodes
		//are numRequest, numRequest+1...numRequest+numRequest-1
		ArrayList<Integer> nodeIds = new ArrayList<Integer>();
		ArrayList<Integer> nodeAdds = new ArrayList<Integer>();
		for(int i=numRequest;i<2*numRequest;i++){
			nodeIds.add(i);
			nodeAdds.add(tree.getNodeAdd(i));
		}
		ArrayList<Byte> readBytes = dos.readBytes(nodeAdds,nodeSize);
		ArrayList<OTreeNode> levelNodes = otnf.createOTreeNodesArrFromBytes(readBytes);
		//get unique tups
		Set<OTuple> tupSet = new HashSet<OTuple>();
		
		for(OTuple tup:tups){
			tupSet.add(tup);
		}
		//System.out.println(tupSet.size());
		Integer level = log2(numRequest);
		for(OTuple tup:tupSet){
			for(int i=0;i<numRequest;i++){
				if((tup.leaf+(int)Math.pow(2, tree.depth))>>(tree.depth-level)==nodeIds.get(i)){
					levelNodes.get(i).insertTup(tup);
					break;
				}
			}
		}
		ArrayList<Byte> updateBytes = otnf.getBytesFromTreeNodesArr(levelNodes);
		dos.writeBytes(nodeAdds, nodeSize, updateBytes);
	}
	
	public static void main(String[] args){
		System.out.println(java.lang.Runtime.getRuntime().maxMemory()/(1024*1024*1024)); 
		int blockSize = 1024;
		int nReq = 1024;
		int nBatch = 1024/nReq;
		String url = "http://localhost:8000/oram";
		POramClient oc = new POramClient(nReq,1,1024*4,16,blockSize/8,8,null);
		System.out.println("oram initialized!");
//		System.out.println(oc.dos.ram.length);
		ArrayList<Boolean> wops = new ArrayList<Boolean>();
		ArrayList<Boolean> rops = new ArrayList<Boolean>();
		ArrayList<OValue> vals= new ArrayList<OValue>();
		ArrayList<OValue> rvals;
		for(int i=0;i<nReq;i++){
			rops.add(false);
			wops.add(true);
		}
		
		ArrayList<ArrayList<OValue>> input = new ArrayList<ArrayList<OValue>>();
		ArrayList<ArrayList<Integer>> adds = new ArrayList<ArrayList<Integer>>();
		for(int i=0;i<nBatch;i++){
			ArrayList<OValue> batch = new ArrayList<OValue>();
			ArrayList<Integer> batchAdds  = new ArrayList<Integer>();
			for(int j=0;j<nReq;j++){
				batch.add(new OValue(nReq*i+j,8));
				batchAdds.add((nReq*i+j)*8);
			}
			input.add(batch);
			adds.add(batchAdds);
		}
		System.out.println("test begins!");
		long t1 = System.currentTimeMillis();
			try {
				for(int i=0;i<nBatch;i++){
					oc.poramIO(adds.get(i),input.get(i),wops);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	
			try {
				for(int i=0;i<nBatch;i++){
				rvals = oc.poramIO(adds.get(i),vals,rops);
				
				for(int j=0;j<nReq;j++){
				System.out.println(rvals.get(j).toInt());
				}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long t2 = System.currentTimeMillis();
			System.out.println(t2-t1);
			
		}
	public static void printLeaf(ArrayList<Integer> leafs,String name){
		System.out.print(name+":");
		for(Integer l: leafs){
			System.out.print(l+",");
		}
		System.out.println();
	}

}
