package org.keychat.net.client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.keychat.common.exceptions.UnhandledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This starts a client's server, allowing other users to directly connect and send messages to this client.
 */
public class KeyChatClientServer implements Runnable {

    private boolean isStarted = false;
	private ServerSocket ss;
    private Semaphore serverStarted = new Semaphore(0);
    private Thread clientServerThread;
    private AtomicBoolean hasBeenStopped = new AtomicBoolean(false);
    private static final Logger log = LoggerFactory.getLogger(KeyChatClientServer.class);

	public KeyChatClientServer(int port) {
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
            // we need to close the socket so that the accept call exits
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
                
                // launch the connection thread for receiving messages from other clients
                new Thread(new KeyChatClientConnection(socket), "Connection server thread").start();

 
            } catch (IOException e) {
                // when the server is stopped accept() will throw an error, but we
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