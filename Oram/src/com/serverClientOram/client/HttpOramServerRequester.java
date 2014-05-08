package com.serverClientOram.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class HttpOramServerRequester implements OramServerRequester {
	int size;
	String url;
	public String sendData(String jsonStr) throws Exception {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost;
		if(url==null){
			httpPost = new HttpPost("http://localhost:8000/oram");
		}
		else{
			httpPost = new HttpPost(url);
		}
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("data", jsonStr));
		httpPost.setEntity(new UrlEncodedFormEntity(nvps));
		CloseableHttpResponse response2 = httpclient.execute(httpPost);

		StringBuilder result = new StringBuilder();

		try {
			HttpEntity entity2 = response2.getEntity();
			// do something useful with the response body
			// and ensure it is fully consumed

			BufferedReader br = new BufferedReader(new InputStreamReader(
					(entity2.getContent())));

			String output;

			while ((output = br.readLine()) != null) {
				result.append(output);
			}

		} finally {
			response2.close();
		}
		return result.toString();
	}

	public HttpOramServerRequester(int sizeOram) {
		this.size = sizeOram;
		JSONObject obj = new JSONObject();
		obj.put("request_type", "init");
		obj.put("size", this.size);
		try {
			sendData(obj.toJSONString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public HttpOramServerRequester(int sizeOram, String url) {
		this.size = sizeOram;
		this.url = url;
		JSONObject obj = new JSONObject();
		obj.put("request_type", "init");
		obj.put("size", this.size);
		try {
			sendData(obj.toJSONString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeBytes(ArrayList<Integer> adds, int nodeSize,
			ArrayList<Byte> bytes) {
		JSONObject obj = new JSONObject();
		obj.put("request_type", "write");
		JSONArray addr = new JSONArray();
		for (Integer i : adds) {
			addr.add(i);
		}
		obj.put("addresses", addr);
		obj.put("length", nodeSize);

		byte[] byteArray = new byte[bytes.size()];
		for (int i = 0; i < byteArray.length; i++) {
			byteArray[i] = bytes.get(i);
		}
		//String content = new String(byteArray);
		obj.put("content", new String(Base64.encodeBase64(byteArray)));
		//System.out.println(content);
		//byte[] encodeByte = content.getBytes();
		//if(bytes.size()!=encodeByte.length){
			//System.out.println("encoding error here!");
		//}
		System.out.printf("sending %d bytes to the server\n",bytes.size());
		// TODO Auto-generated catch block
		try {
			sendData(obj.toJSONString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		/*testing purpose
		ArrayList<Byte> readArray = readBytes(adds,nodeSize);
		for(int i=0;i<readArray.size();i++){
			if(!bytes.get(i).equals(readArray.get(i))){
				System.out.printf("write error here: %d, %d becomes %d",i,byteArray[i],readArray.get(i));
				System.out.println(Arrays.toString(byteArray));
			}
		}*/
	}

	public ArrayList<Byte> readBytes(ArrayList<Integer> adds, int nodeSize) {
		JSONObject obj = new JSONObject();
		obj.put("request_type", "read");
		JSONArray addr = new JSONArray();
		for (Integer i : adds) {
			addr.add(i);
		}
		obj.put("addresses", addr);
		obj.put("length", nodeSize);

		ArrayList<Byte> ba = new ArrayList<Byte>();
		try {
			String response = sendData(obj.toJSONString());
			JSONObject job = (JSONObject) JSONValue.parse(response);
			response = (String) job.get("response");
			byte[] responseBytes = Base64.decodeBase64(response.getBytes());
			for (byte b : responseBytes) {
				ba.add(b);
			}
			System.out.printf("reading %d bytes from the server\n",responseBytes.length);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return ba;
	}

	public static void main(String[] args) {
		DummyOramServer dos = new DummyOramServer(100);
		ArrayList<Integer> address = new ArrayList<Integer>();
		address.add(10);
		address.add(40);
		ArrayList<Byte> b = new ArrayList<Byte>();
		for (int i = 0; i < 20; i++) {
			b.add((byte) (0 + i));
		}
		dos.writeBytes(address, 10, b);
		ArrayList<Byte> ba = dos.readBytes(address, 10);
		for (int i = 0; i < 20; i++) {
			System.out.print(ba.get(i));
		}
	}
}
