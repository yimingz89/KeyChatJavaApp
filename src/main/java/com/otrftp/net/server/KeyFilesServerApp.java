package com.otrftp.net.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.otrftp.common.exceptions.UnhandledException;

public class KeyFilesServerApp implements Runnable
{

    public static List<User> usernames = Collections.synchronizedList(new ArrayList<User>());

    private static final Logger log = LoggerFactory.getLogger(KeyFilesServerApp.class);

    private boolean isStarted = false;
    private Thread serverThread;
    private AtomicBoolean hasBeenStopped = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private Semaphore serverStarted = new Semaphore(0);


    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            printUsage();
            return;
        }

        KeyFilesServerApp server = new KeyFilesServerApp();
        if (args[0].equalsIgnoreCase("--port")) {
            try {
                server.listen(Integer.parseInt(args[1]));
            } catch (Exception e) {
                printUsage();
                return;
            }
            server.start();
            //server.stop();
        }
        else {
            printUsage();
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java com.otrftp.net.server.KeyFilesServerApp --port port_number");
    }

    public void listen(int port) throws IOException {
        log.info("Binding server on port [{}] ...", port);
        serverSocket = new ServerSocket(port);
        log.info("Server is listening on port [{}] ...", port);
    }

    public void start() {
        if(!isStarted) {
            serverThread = new Thread(this, "KeyFiles server thread");
            serverThread.start();
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
            serverSocket.close();
        } catch (IOException e) {
            log.error("Exception while closing server socket: ", e);
        }
    }

    @Override
    public void run()
    {
        while(!hasBeenStopped.get())
        {
            Socket socket;

            try {
                serverStarted.release();
                socket = serverSocket.accept();
                log.info("Server accepted client from [" + socket.getInetAddress().getCanonicalHostName() + "]");

                // TODO: start a thread that handles this client!
                new Thread(new KeyFilesServerConnection(socket), "Server connection thread").start();


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
            serverSocket.close();
        } catch (IOException e) {
            log.error("Failed closing down the server: ", e);
        }

        isStarted = false;
    }
}