package com.otrftp.net.client;

import static com.otrftp.common.IOUtils.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KeyFilesSecondConnection implements Runnable {

    private static final String HELPSENDFILE = "help-send-file";
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
                
                if(!command.equals(HELPSENDFILE)) {
                    log.error(command + " received, help-send-file command expected");
                    continue;
                }
                
                String fileName = readLine(is);
                if(fileName == null || fileName.isEmpty()) {
                    log.error("Unable to obtain file name properly");
                }
                
               String stringFileSize = readLine(is);
               int fileSize = Integer.parseInt(stringFileSize);
               byte[] buffer = new byte[4096];
               
               int read = 0;
               int remainingBytes = fileSize;
               
               FileOutputStream fos = new FileOutputStream(fileName);
               while(remainingBytes > 0) {
                   if(remainingBytes < buffer.length) {
                       read = is.read(buffer, 0, remainingBytes);
                   }
                   else {
                       read = is.read(buffer, 0, buffer.length);
                   }
                   
                   fos.write(buffer, 0, read);
                   remainingBytes -= read;
               }
               
               log.info("File received, total byte size: " + fileSize);
               fos.close();
               
               
               
               
               
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
