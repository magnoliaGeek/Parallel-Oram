package com.serverClientOram.server;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: hongtaocai
 * Date: 4/22/14
 * Time: 8:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class RamOperator implements StorageOperator{
    byte[] data;

    public void set(int[] addresses, int length, byte[] content) {
        int j = 0;
        for (int i = 0; i < addresses.length; i++) {
            int start = addresses[i];
            for (int t = 0; t < length; t++) {
                data[start + t] = content[j];
                j++;
            }
        }
        //printAll();
    }

    public byte[] get(int[] addresses, int length) {
        byte[] content = new byte[length*addresses.length];
        int j=0;
        for (int i=0;i<addresses.length;i++){
            int start = addresses[i];
            for(int t=0;t<length;t++){
                content[j] = data[start+t];
                j++;
            }
        }
        return content;
    }

    public void init(int sizeOram) {
        this.data = new byte[sizeOram];
        // initialize write "-1" to every 4 byte array
        byte[] empty = ByteBuffer.allocate(4).putInt(-1).array();
        for (int i = 0; i < this.data.length; i++) {
            data[i] = empty[i % 4];
        }
    }

    void printAll(){
        for(int i=0;i<data.length;i++){
            System.out.print(data[i]);
            if(i%10==0){
                System.out.println();
            }
        }
    }

}
