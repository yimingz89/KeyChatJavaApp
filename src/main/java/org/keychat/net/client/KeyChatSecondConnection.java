package org.keychat.net.client;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a client connection to the main server for communicating between clients through the main server using the HELPSENDMESSAGE command.
 */
public class KeyChatSecondConnection extends KeyChatClientBase implements Runnable {

    private static final String HELPSENDMESSAGE = "help-send-message";
    private static final Logger log = LoggerFactory.getLogger(KeyChatSecondConnection.class);
    private static final String tempFolder = System.getProperty("java.io.tmpdir"); // name of the temporary folder where the file for storing the encrypted message is saved

    private BufferedReader br;
    private PrintWriter pw;
    

    public KeyChatSecondConnection(Socket socket, String user, int port) throws IOException {
        this.user = user;
        clientServerPort = port;
        
        pw = new PrintWriter(socket.getOutputStream());
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void run() {
        
        try {
            appearOnline(pw);
            
            while(true) {
                String command = br.readLine();
                
                if(!command.equals(HELPSENDMESSAGE)) {
                    log.error(command + " received, help-send-message command expected");
                    continue;
                }
                
                String sender = br.readLine(); // read in sender
                String bytes = br.readLine(); // read in the number of bytes the encrypted message is
                
                char[] chars = new char[Integer.parseInt(bytes)];
                IOUtils.readFully(br, chars); // get specified number of bytes from reader
                String cipherText = new String(chars); // store encrypted message as a String
                
                String cipherFileName = tempFolder + RandomStringUtils.randomAlphanumeric(10);
                File cipherFile = new File(cipherFileName);
                FileUtils.write(cipherFile, cipherText); // write encrypted text to a temporary file
                
                // decrypt (using Keybase command line) and display on console
                String decryptedMsg = KeybaseCommandLine.decrypt(cipherFileName);
                System.out.print("Message from " + sender + ": " + decryptedMsg);
                
                cipherFile.delete(); // delete temporary file
               
               
               
            } 
        } catch (IOException e) {
            log.error("Error reading from server", e);
        }
    }

}
