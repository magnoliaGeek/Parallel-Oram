package com.serverClientOram.server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HttpOramHandler  implements HttpHandler {

    StorageOperator ro;

    HttpOramHandler(StorageOperator ro){
        super();
        this.ro = ro;
    }

    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }


    @Override
    public void handle(HttpExchange t) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
        InputStream is = t.getRequestBody();

        Map<String, Object> params =
                (Map<String, Object>)t.getAttribute("parameters");


        String response = "done";


        JSONObject jo = (JSONObject) JSONValue.parse( params.get("data").toString());
        String requestType = (String) jo.get("request_type");


        if ( requestType.equals("init") ){
            long size = (Long) (jo.get("size")) ;
            ro.init((int)size);
        } else if ( requestType.equals("read")) {
            JSONArray addr_arr = (JSONArray) jo.get("addresses");
            int[] addresses = new int[addr_arr.size()];
            for(int i=0;i<addr_arr.size();i++){
                long tmp = ((Long) addr_arr.get(i));
                addresses[i] = (int) tmp;
            }
            long length = (Long) jo.get("length");
            response = new String(Base64.encodeBase64(ro.get(addresses, (int) length)));
        } else if ( requestType.equals("write") ){
            JSONArray addr_arr = (JSONArray) jo.get("addresses");
            int[] addresses = new int[addr_arr.size()];
            for(int i=0;i<addr_arr.size();i++){
                long tmp = ((Long) addr_arr.get(i));
                addresses[i] = (int) tmp;
            }
            long length = (Long) jo.get("length");
            Object o = jo.get("content");

            String contentStr =  jo.get("content").toString();
            ro.set(addresses, (int) length, Base64.decodeBase64(contentStr.getBytes()));
        }


        JSONObject job = new JSONObject();
        job.put("response", response);
        response = job.toJSONString();

        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }



    public static void main(String args[]) throws  Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000),100);
        //System.out.println("testing...");
        HttpContext context;
        if( args.length>0 && args[0].equalsIgnoreCase("D")){
        	String path = args[1];//"C:/Users/Chen/Documents/cornell/classes/crypto2/ObliDrive";
        	context = server.createContext("/oram", new HttpOramHandler(new DiskOperator(path,Integer.parseInt(args[2]))));//16640
        }
        else{
        	context = server.createContext("/oram",new HttpOramHandler(new RamOperator()));
        }
        context.getFilters().add(new ParameterFilter());
        server.setExecutor(null); // creates a default executor
        server.start();
    }
}

