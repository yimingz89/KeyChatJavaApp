package org.keychat.net.client;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;

/**
 * This is a client connection to the main server for communicating between clients through the main server using the HELPSENDMESSAGE command.
 */
public class KeyChatSecondConnection extends KeyChatClientBase implements Runnable {

    private static final String HELPSENDMESSAGE = "help-send-message";
    private static final Logger log = LoggerFactory.getLogger(KeyChatSecondConnection.class);
    private static final String tempFolder = System.getProperty("java.io.tmpdir"); // name of the temporary folder where the file for storing the encrypted message is saved

    private BufferedReader br;
    private PrintWriter pw;
    

    public KeyChatSecondConnection(Socket socket, String user, String uid, int port, Path publicKeyPath) throws IOException {
        this.user = user;
        this.uid = uid;
        clientServerPort = port;
        this.publicKeyPath = publicKeyPath;

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
                
                
                // decrypt using secret key
                
                
                String plainText = decrypt(cipherText, KeyChatClientApp.uidMap.get(sender));
                System.out.println("Message from " + sender + ": " + plainText);
               
               
               
            } 
        } catch (Exception e) {
            log.error("Error reading from server", e);
        }
    }
    
    private String decrypt(String cipherText, String senderUid) throws Exception {
        System.out.println("Decrypting ... ");
        ByteArrayInputStream cipherTextStream = new ByteArrayInputStream(cipherText.getBytes());

        final InputStream decryptedInputStream = BouncyGPG.decryptAndVerifyStream()
            .withConfig(KeyChatClientApp.keyring)
            //.andIgnoreSignatures()
            .andRequireSignatureFromAllKeys(senderUid)
            .fromEncryptedInputStream(cipherTextStream);

        byte[] plain = new byte[2048];
        int len = decryptedInputStream.read(plain);

        String plainText = new String(plain, 0, len);
        return plainText;
    }

}
