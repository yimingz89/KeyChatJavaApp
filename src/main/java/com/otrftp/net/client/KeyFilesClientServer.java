package com.otrftp.net.client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.otrftp.common.exceptions.UnhandledException;

public class KeyFilesClientServer implements Runnable {

    private boolean isStarted = false;
	private ServerSocket ss;
    private Semaphore serverStarted = new Semaphore(0);
    private Thread clientServerThread;
    private AtomicBoolean hasBeenStopped = new AtomicBoolean(false);
    private static final Logger log = LoggerFactory.getLogger(KeyFilesClientServer.class);

	public KeyFilesClientServer(int port) {
		try {
	        log.info("Binding client server on port [{}] ...", port);
			ss = new ServerSocket(port);
	        log.info("Client server is listening on port [{}] ...", port);
		} catch (IOException e) {
			log.error("Server could not start: ", e);
		}
	}
	
    public void start() {
        if(!isStarted) {
        	clientServerThread = new Thread(this, "KeyFiles client server thread");
        	clientServerThread.start();
            try {
                serverStarted.acquire();
            } catch (InterruptedException e) {
                throw new UnhandledException("Interrupted while waiting for server to start", e);
            }
        }
    }
	
	public void stop() {
        hasBeenStopped.set(true);
 
        try {
            // We need to close the socket so that the accept call exits
            // throwing an exception
            ss.close();
        } catch (IOException e) {
            log.error("Exception while closing server socket: ", e);
        }
    }
	
    @Override
	public void run() {
        while(!hasBeenStopped.get())
        {
            Socket socket;
 
            try {
                serverStarted.release();
                socket = ss.accept();
                log.info("Server accepted client from [" + socket.getInetAddress().getCanonicalHostName() + "]");
                
                new Thread(new KeyFilesClientConnection(socket), "Connection server thread").start();

 
            } catch (IOException e) {
                // When the server is stopped accept() will throw an error, but we
                // don't want it displayed
                if(hasBeenStopped.get() == false) {
                    log.error("Error accepting client: ", e);
                } else {
                    log.info("Server's been shutdown gracefully");
                }
            }
        }
        
        try {
            ss.close();
        } catch (IOException e) {
            log.error("Failed closing down the server: ", e);
        }
 
        isStarted = false;

	}


	


}