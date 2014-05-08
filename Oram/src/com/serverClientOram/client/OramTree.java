package com.serverClientOram.client;

import java.util.ArrayList;
import java.util.Random;

public class OramTree {
	public Integer start;
	public Integer depth;
	public Integer sizeNode;

	public OramTree(Integer start, Integer depth, Integer sizeNode) {
		this.start = start;
		this.depth = depth;
		this.sizeNode = sizeNode;
	}

	// get addresses for a path towards a leaf
	public ArrayList<Integer> getPath(Integer leaf) {
		ArrayList<Integer> path = new ArrayList<Integer>();
		int nodeId = leaf + (int) Math.pow(2, depth);
		while (nodeId > 0) {
			path.add(0, start + (nodeId - 1) * sizeNode);
			nodeId /= 2;
		}
		return path;
	}
	
	//get address for a nodeId
	public Integer getNodeAdd(Integer nodeId){
		return start + (nodeId-1)*sizeNode;
	}
	
	public Integer getNodeId(Integer nodeAdd){
		return (nodeAdd-start)/sizeNode +1;
	}
	
	//get a random leaf
	public Integer getRandomLeaf() {
		return new Random().nextInt((int) Math.pow(2, depth));
	}

	public ArrayList<Integer> getRoot() {
		// Only one address, we make the return value as array such that the
		// read/write operation is unified.
		ArrayList<Integer> root = new ArrayList<Integer>();
		root.add(start);
		return root;
	}

	// get the node id for a path, in which the root has id 1, and the
	// leaf has id leaf + 2^depth;
	public ArrayList<Integer> getIds(Integer leaf) {
		// TODO Auto-generated method stub
		int curr = leaf + (int) Math.pow(2, depth);
		ArrayList<Integer> nodeIds = new ArrayList<Integer>();
		for (int i = 0; i <= depth; i++) {
			nodeIds.add(null);
		}
		for (int i = depth; i >= 0; i--) {
			nodeIds.set(i, curr);
			curr /= 2;
		}
		return nodeIds;
	}
	
	//get node ram address for super path
	public ArrayList<ArrayList<Integer>> getSuperPath(ArrayList<Integer> leafs) {
		ArrayList<ArrayList<Integer>> spath = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < leafs.size(); i++) {
			ArrayList<Integer> path = new ArrayList<Integer>();
			int nodeId = leafs.get(i) + (int) Math.pow(2, depth);
			while (nodeId > 0) {
				path.add(0, start + (nodeId - 1) * sizeNode);
				nodeId /= 2;
			}
			spath.add(path);
		}
		return spath;
	}
}
