package com.serverClientOram.client;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PBlockOramClient extends POramClient {
	public PBlockOramClient(int numRequest, int layers, int sizeCpuMap, int k,
			int alpha, int bsize, String url) {
		super(numRequest, layers, sizeCpuMap, k, alpha, bsize, url);
	}

	public ArrayList<OTuple> pBlockOramIO(ArrayList<Integer> adds,
			ArrayList<OTuple> ots, ArrayList<Boolean> write) throws Exception {
		// The block address for each level of map
		ArrayList<ArrayList<Integer>> bs = new ArrayList<ArrayList<Integer>>();
		// System.out.println();
		for (int l = 0; l < layers; l++) {
			bs.add(new ArrayList<Integer>());
			for (Integer addr : adds) {
				bs.get(l).add(addr / (int) Math.pow(alpha, layers - l));
			}
			// printLeaf(bs.get(l),"bs"+l);
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
		// printLeaf(leafs,"leafs");
		// printLeaf(newLeafs,"new leafs");
		// update the cpuMap
		for (Integer k : leafMap.keySet()) {
			cpuMap[k] = leafMap.get(k);
		}

		for (int l = 0; l < layers; l++) {
			// perform super path reading and write back, using dummy leaf to
			// get identical ram access pattern

			// get val in this layer and replace the block with empty, now the b
			// field of all tups extracted is -1
			ArrayList<OTuple> tups = getTupsFromLeafs(l, leafs, bs.get(l));
			if (l == layers - 1) {
				for (int i = 0; i < adds.size(); i++) {
					if (tups.get(i) == null) {
						/*
						 * if(l>=layers-2){ System.out.printf(
						 * "tuple %d not found from path to leaf %d\n"
						 * ,bs.get(l).get(i),leafs.get(i)); }
						 */
						OTuple tup = otf.createOTuple(bs.get(l).get(i),
								newLeafs.get(i));

						tups.set(i, tup);
						tups.get(i).b = bs.get(l).get(i);
					}
				}
			}

			// if The position haven't been accessed before,
			// it's the first time to write in this positon.
			// get the leaf for the next layer if not visiting the last layer
			if (l < layers - 1) {
				for (int i = 0; i < adds.size(); i++) {
					if (tups.get(i) == null) {
						/*
						 * if(l>=layers-2){ System.out.printf(
						 * "tuple %d not found from path to leaf %d\n"
						 * ,bs.get(l).get(i),leafs.get(i)); }
						 */
						OTuple tup = otf.createOTuple(bs.get(l).get(i),
								newLeafs.get(i));
						OValue val = ovf.createOValue(rd.nextInt(cpuMap.length
								* (int) Math.pow(alpha, l + 1)));
						tup.updateVal(
								adds.get(i)
										/ (int) Math.pow(alpha, layers - l - 1)
										% alpha, val);
						tups.set(i, tup);
						// find other tups that share the same block address
						// with
						// this one
						for (int j = i + 1; j < tups.size(); j++) {
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
											/ (int) Math.pow(alpha, layers - l
													- 1) % alpha));
					if (vals.get(i).toInt() == -1) {
						/*
						 * if(l==layers-2){
						 * System.out.printf("val for tuple %d is not available!\n"
						 * , bs.get(layers-1).get(i)); }
						 */
						vals.set(
								i,
								ovf.createOValue(rd.nextInt(cpuMap.length
										* (int) Math.pow(alpha, l + 1))));
						tups.get(i).updateVal(
								adds.get(i)
										/ (int) Math.pow(alpha, layers - l - 1)
										% alpha, vals.get(i));
					}

				}

				for (int i = 0; i < tups.size(); i++) {
					leafs.set(i, vals.get(i).toInt());
				}
			}

			// Assign new leafs use a map to prevent assign different leaf for
			// the same block.
			leafMap = new HashMap<Integer, Integer>();
			for (int i = 0; i < tups.size(); i++) {
				if (l < layers - 1) {
					// update the value to a new leaf in the next layer
					if (!leafMap.containsKey(bs.get(l + 1).get(i))) {
						int newl = rd.nextInt(cpuMap.length
								* (int) Math.pow(alpha, l + 1));
						newLeafs.set(i, newl);
						leafMap.put(bs.get(l + 1).get(i), newl);
					} else {
						newLeafs.set(i, leafMap.get(bs.get(l + 1).get(i)));
					}

					vals.get(i).writeInt(newLeafs.get(i));
					tups.get(i)
							.updateVal(
									adds.get(i)
											/ (int) Math.pow(alpha, layers - l
													- 1) % alpha,
									new OValue(newLeafs.get(i), bsize));
				}

				// In the final layer, write the value if the IO operation is
				// write
				else {
					if (write.get(i)) {
						tups.get(i).vals = ots.get(i).vals;
						// System.out.printf("write %d, leaf %d\n",ovals.get(i).toInt(),tups.get(i).leaf);
					} else {
						ots.set(i, tups.get(i));// System.out.printf("read %d, leaf %d\n",vals.get(i).toInt(),tups.get(i).leaf);
					}

				}
				/*
				 * if(i==tups.size()-1){ printLeaf(leafs,"leafs here");
				 * printLeaf(newLeafs,"new leafs here"); for(OTuple t: tups){
				 * System.out.print(t.leaf+","); } System.out.print("\nValue:");
				 * for(OValue val:vals){ System.out.print(val.toInt() + ","); }
				 * System.out.println(); }
				 */
			}

			// put the tuple to the root

			putLevel(trees.get(l), tups);

			// flush
			flushLevel(trees.get(l));

		}
		return ots;
	}

	public static void main(String[] args) {
		String url = "http://localhost:8000/oram";
		int tupSize = 16;
		PBlockOramClient boc = new PBlockOramClient(4, 3, 32, 16, tupSize, 8,
				url);
		int tupByteSize = tupSize*boc.bsize;
		int fileSize = 4096;
		byte[] contentBytes = new byte[fileSize];
		ArrayList<byte[]> letterbytes = new ArrayList<byte[]>();
		for (int i = 0; i < fileSize/256; i++) {
			byte[] charArr = ByteBuffer.allocate(2).putChar((char) ('A' + i))
					.array();
			letterbytes.add(charArr);
			for (int j = i * 256; j < (i + 1) * 256; j++) {
				contentBytes[j] = letterbytes.get(i)[j % 2];
			}
		}
		try {
			System.out.println(new String(contentBytes));
			int numTups = (int) Math.ceil((contentBytes.length + 0.0)
					/ (boc.bsize * boc.alpha));
			// number of parallel request batches
			int numBatch = (int) Math.ceil((numTups + 0.0) / boc.numRequest);
			int numVals = numBatch * boc.numRequest * boc.alpha;
			ArrayList<OValue> ovs = new ArrayList<OValue>();
			// write file content to array of OValues;
			for (int i = 0; i < numVals; i++) {
				byte[] valByte = new byte[boc.bsize];
				for (int j = 0; j < boc.bsize; j++) {
					if (i * boc.bsize + j < contentBytes.length) {
						valByte[j] = contentBytes[i * boc.bsize + j];
					} else {
						valByte[j] = 0;
					}
				}
				OValue val = new OValue(valByte, boc.bsize);
				//System.out.print(new String(val.bytes));
				ovs.add(val);
			}
			System.out.println();
			// write otuples to the oram and store the indexes of these tuples;
			ArrayList<Boolean> wops = new ArrayList<Boolean>();
			for (int i = 0; i < boc.numRequest; i++) {
				wops.add(true);
			}
			//int tupCounter = 0;
			System.out.printf("numBatch: %d", numBatch);
			for (int h = 0; h < numBatch; h++) {
				ArrayList<OTuple> ots = new ArrayList<OTuple>();
				ArrayList<Integer> adds = new ArrayList<Integer>();
				for (int i = 0; i < boc.numRequest; i++) {
					ArrayList<OValue> vals = new ArrayList<OValue>(ovs.subList(
							( h*boc.numRequest + i) * boc.alpha, ( h*boc.numRequest+ i + 1) * boc.alpha));
					OTuple ot = new OTuple(0, 0, 0, 0, vals);
					ots.add(ot);
					adds.add(( h * boc.numRequest + i) * boc.alpha);
				}
				boc.pBlockOramIO(adds, ots, wops);
			}
			//tupCounter += numBatch * boc.numRequest;

			byte[] res = new byte[fileSize];
			ArrayList<Boolean> rops = new ArrayList<Boolean>();
			for (int i = 0; i < boc.numRequest; i++) {
				rops.add(false);
			}
			int frontier = 0;
			for (int h = 0; h < numBatch; h++) {
				ArrayList<OTuple> ots = new ArrayList<OTuple>();
				ArrayList<Integer> adds = new ArrayList<Integer>();
				for (int i = 0; i < boc.numRequest; i++) {
					ots.add(null);
					int add = (h*boc.numRequest+i)*boc.alpha;
					System.out.print(add + " " );
					adds.add(add);
				}
				System.out.println();
				boc.pBlockOramIO(adds, ots, rops);
				for (OTuple rot : ots) {
					for (int j = 0; j < boc.alpha; j++) {
						// System.out.print(new String(v.bytes));
						System.out.print(new String(rot.vals.get(j).bytes));
						for (int k = 0; k < boc.bsize; k++) {
							if (frontier + j * boc.bsize + k < fileSize) {
								res[frontier + j * boc.bsize + k] = rot.vals
										.get(j).bytes[k];
							}
						}
					}
					frontier += tupByteSize;
					System.out.println();
					System.out.println(frontier);
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
