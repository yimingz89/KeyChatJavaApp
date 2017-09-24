package com.otrftp.net.client;

import static com.otrftp.common.IOUtils.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KeyFilesSecondConnection implements Runnable {

    private static final String HELPSENDMESSAGE = "help-send-message";
    private static final Logger log = LoggerFactory.getLogger(KeyFilesSecondConnection.class);

    private InputStream is;
    private OutputStream os;
    private String user;
    private int clientServerPort;
    
    public KeyFilesSecondConnection(Socket socket, String user, int port) throws IOException {
        this.user = user;
        clientServerPort = port;
        
        is = socket.getInputStream();
        os = socket.getOutputStream();
    }

    public void run() {
        
        try {
            appearOnline(os, is);
            
            while(true) {
                String command = readLine(is);
                
                if(!command.equals(HELPSENDMESSAGE)) {
                    log.error(command + " received, help-send-message command expected");
                    continue;
                }
                
                String sender = readLine(is);
                String bytes = readLine(is);
                String cipherText = readFromStream(is, Integer.parseInt(bytes));
                
                String cipherFileName = RandomStringUtils.randomAlphanumeric(10);
                File cipherFile = new File(cipherFileName);
                FileUtils.write(cipherFile, cipherText);
                
            
                String decryptedMsg = KeybaseCommandLine.decrypt(cipherFileName);
                System.out.print("Message from " + sender + ": " + decryptedMsg);
                
                cipherFile.delete();
               
               
               
            } 
        } catch (IOException e) {
            log.error("Error reading from server", e);
        }
    }
    
    private void appearOnline(OutputStream os, InputStream is) throws IOException {
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
        pw.println(user);
        pw.println(clientServerPort);
        pw.flush();
    }

}
