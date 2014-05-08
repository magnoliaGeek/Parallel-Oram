package com.serverClientOram.client;

import java.util.ArrayList;

//simple Oram client for block io
public class BlockOramClient extends OramClient {
	public BlockOramClient(int layers, int sizeCpuMap, int k, int alpha,
			int bsize, String url) {
		super(layers, sizeCpuMap, k, alpha, bsize,url);
	}

	public OTuple readTuple(Integer add) throws OverflowException {
		return oramTupleIO(add, null, false);
	}

	public void writeTuple(Integer add, OTuple ot) throws OverflowException {
		oramTupleIO(add, ot, true);
	}

	public OTuple oramTupleIO(Integer add, OTuple ot, Boolean write)
			throws OverflowException {
		int[] b = new int[layers];
		for (int i = 0; i < layers; i++) {
			b[i] = add / (int) Math.pow(alpha, layers - i);
		}
		int leaf = cpuMap[b[0]];
		// update the map
		int newLeaf = rd.nextInt(cpuMap.length);
		cpuMap[add / (int) Math.pow(alpha, layers)] = newLeaf;
		OValue val = null;
		for (int i = 0; i < layers; i++) {
			// ArrayList<Integer> adds = trees.get(i).getPath(leaf);
			ArrayList<Byte> pathBytes = dos.readBytes(trees.get(i)
					.getPath(leaf), nodeSize);

			// get val in this layer and replace the block with empty, now the b
			// field of tup is -1
			OTuple tup = getTupFromPath(pathBytes, b[i]);
			// write the path back to the tree, the tup has been replaced with
			// empty
			dos.writeBytes(trees.get(i).getPath(leaf), nodeSize, pathBytes);

			// if The position haven't been accessed before,
			// it's the first time to write in this positon.
			if (tup == null) {
				tup = otf.createOTuple(b[i], newLeaf);
				val = ovf.createOValue(rd.nextInt(cpuMap.length
						* (int) Math.pow(alpha, i + 1)));
				tup.updateVal(add / (int) Math.pow(alpha, layers - i - 1)
						% alpha, val);
			}

			// rectify the b field of tup
			tup.b = b[i];

			// update the leaf for the tuple
			tup.updateLeaf(newLeaf);

			// get the leaf for the next layer if not visiting the last layer
			if (i < layers - 1) {

				// get the value from the tuple
				val = tup.getVal(add / (int) Math.pow(alpha, layers - i - 1)
						% alpha);

				// if the value is empty,it's the first time to access that
				// value in this tuple
				if (val.toInt() == -1) {
					val = ovf.createOValue(rd.nextInt(cpuMap.length
							* (int) Math.pow(alpha, i + 1)));
					tup.updateVal(add / (int) Math.pow(alpha, layers - i - 1)
							% alpha, val);
				}

				leaf = val.toInt();
				// update the value to a new leaf in the next layer
				newLeaf = rd.nextInt(cpuMap.length
						* (int) Math.pow(alpha, i + 1));
				val.writeInt(newLeaf);
				tup.updateVal(add / (int) Math.pow(alpha, layers - i - 1)
						% alpha, val);
			}

			// In the final layer, write the value if the IO operation is
			// write
			if (i == layers - 1) {
				if (write) {
					tup.vals = ot.vals;
				} else {
					ot = tup;
				}
			}

			// put the tuple to the root

			putRoot(trees.get(i), tup);

			// flush
			flush(trees.get(i));

		}

		// For the last layer, we only need the OTuple, we don't need to
		// go through the Tuple to find the val

		return ot;
	}

	public static void main(String[] args) throws OverflowException {
		String url = "http://localhost:8000/oram";
		int tupSize = 16;
		BlockOramClient oc = new BlockOramClient(1, 1024, 16, tupSize, 8,url);
		String content = "We reinvestigate the oblivious RAM concept introduced by Goldreich and Ostrovsky, "
				+ "which enables a client, that can store locally only a constant amount of data, to store"
				+ " remotely n data items, and access them while hiding the identities of the items which are"
				+ " being accessed. Oblivious RAM is often cited as a powerful tool, but is also commonly considered"
				+ " to be impractical due to its overhead, which is asymptotically efficient but is quite high."
				+ " We redesign the oblivious RAM protocol using modern tools, namely Cuckoo hashing and a new oblivious"
				+ " sorting algorithm. The resulting protocol uses only O(n) external memory, and replaces each data request by only O(log2 n) requests.";
		byte[] contentBytes = content.getBytes();
		ArrayList<OValue> ovs = new ArrayList<OValue>();
		for (int i = 0; i < contentBytes.length / oc.bsize; i++) {
			byte[] valByte = new byte[oc.bsize];
			for (int j = 0; j < oc.bsize; j++) {
				valByte[j] = contentBytes[i * oc.bsize + j];
			}
			OValue val = new OValue(valByte, oc.bsize);
			ovs.add(val);
		}
		long t1 = System.currentTimeMillis();
		for (int i = 0; i < 5; i++) {
			ArrayList<OValue> vals = new ArrayList<OValue>(ovs.subList(i
					* oc.alpha, (i + 1) * oc.alpha));
			OTuple ot = new OTuple(0, 0, 0, 0, vals);
			oc.writeTuple(i * 64, ot);
			System.out.printf("%d tuple written!\n", i);
		}
		for (int i = 0; i < 5; i++) {
			OTuple rot = oc.readTuple(i * 64);
			for (OValue v : rot.vals) {
				System.out.println(new String(v.bytes));
			}
		}
		long t2 = System.currentTimeMillis();
		System.out.println(t2-t1);
	}
}
