package com.otrftp.net.server;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class User {
	private String user;
	private String address;
	private int port;
	private List<Socket> sockets = new ArrayList<>();
	
	public User(String user, String address, int port) {
		this.user = user;
		this.address = address;
		this.port = port;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

    public List<Socket> getSocket() {
        return sockets;
    }

    public void setSocket(List<Socket> socket) {
        this.sockets = socket;
    }
	
    @Override
    public String toString() {
        return "Username: " + user + " , Address: " + address + " , Port: " + port;
    }
	
}
