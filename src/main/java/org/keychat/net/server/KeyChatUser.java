package org.keychat.net.server;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that represents a KeyChat user
 */
public class KeyChatUser {
	private String user; // the user's username
	private String address; // the user's server's address
	private int port; // the user's server's port
	private String publicKey; // the user's (GPG-formatted) public key
	private String uid; // typically this is the user's email
	private List<Socket> sockets = new ArrayList<>(); // a List of sockets for the two connections with the main server
	
	public KeyChatUser(String user, String address, int port, String uid, String publicKey) {
		this.user = user;
		this.address = address;
		this.port = port;
		this.uid = uid;
		this.publicKey = publicKey;
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
	
	public String getPublicKey() {
		return publicKey;
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
	
	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}

    public List<Socket> getSocket() {
        return sockets;
    }

    public void setSocket(List<Socket> socket) {
        this.sockets = socket;
    }
	
    @Override
    public String toString() {
        return "Username: " + user + " , Address: " + address + " , Port: " + port + " , Public Key: " + publicKey;
    }

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}
	
}
