package com.serverClientOram.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HttpOramServer implements HttpHandler{
	int size;
	byte[] ram;
	int delay = 0;

	public HttpOramServer(int sizeOram) {
		this.size = sizeOram;
		this.ram = new byte[sizeOram];
		// initialize
		byte[] empty = ByteBuffer.allocate(4).putInt(-1).array();
		for (int i = 0; i < size; i++) {
			ram[i] = empty[i % 4];
		}
	}

	public void writeBytes(ArrayList<Integer> adds, int nodeSize,
			ArrayList<Byte> bytes) {
		for (int i = 0; i < adds.size(); i++) {
			for (int j = 0; j < nodeSize; j++) {
				ram[adds.get(i) + j] = bytes.get(i * nodeSize + j);
			}
		}
		if (delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public ArrayList<Byte> readBytes(ArrayList<Integer> adds, int nodeSize) {
		ArrayList<Byte> res = new ArrayList<Byte>();
		for (int i = 0; i < adds.size(); i++) {
			for (int j = 0; j < nodeSize; j++) {
				res.add(ram[adds.get(i) + j]);
			}
		}
		if (delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
		return res;
	}
	public void handle(HttpExchange t) throws IOException {
		//TODO: handle read write request through http connection
		InputStream is = t.getRequestBody();
		
        String response = "This is the response";
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
	public static void main(String[] args){
		
	}
}
