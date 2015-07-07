package com.example.rssitester;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.IOException;
import java.net.*;

public class UPDwork {
	 InetAddress local = null;
	    int server_port = 12345;
	    
		DatagramSocket s = null;
		
		public UPDwork(){
		    try {  
		        s = new DatagramSocket();  
		    } catch (SocketException e) {  
		        e.printStackTrace();  
		    } 
		}
	    
	    public void setHost(String host){
	        try {  
	            local = InetAddress.getByName(host);  
	        } catch (UnknownHostException e) {  
	            e.printStackTrace();  
	        }  
	    }
	    
	    public void setPort(int port) {
	    	server_port = port;
	    }
	    
		public void send(final String message) {
	        new Thread(){
	        	@Override
	        	public void run(){
	                try {  
	                    int msg_length = message.length();  
	                    byte[] messageByte = message.getBytes("iso-8859-1");  
	                    DatagramPacket p = new DatagramPacket(messageByte, msg_length, local, server_port); 
	                    s.send(p);
	                } catch (Exception e) {  
	                    e.printStackTrace();  
	                }
	        	}
	        }.start();
	    }
}

