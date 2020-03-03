package co.zzyun.wsocks.client.core.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import io.vertx.core.Handler;

public class SimpleUdp extends Thread {
    private DatagramSocket socket;
    private byte[] buf = new byte[1500];
    private Handler<DatagramPacket> handler;
    public SimpleUdp(int port) {
        try {
            socket = new DatagramSocket(new InetSocketAddress("0.0.0.0",port));
            System.out.println("Listen at "+port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        if(handler==null)
            throw new IllegalStateException("Handler cannot be null");
        while (true) {
            DatagramPacket packet
                    = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                System.out.println("Udp Receive Timeout");
                continue;
            }

            handler.handle(packet);
        }
    }

    public SimpleUdp send(byte[] bytes,int port, InetAddress address){
        DatagramPacket packet = new DatagramPacket(bytes,bytes.length,address,port);
        try {
            this.socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public SimpleUdp handler(Handler<DatagramPacket> handler){
        this.handler = handler;
        return this;
    }
}