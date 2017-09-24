package com.otrftp.net.client;

import static com.otrftp.common.IOUtils.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyFilesClientConnection implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(KeyFilesClientConnection.class);


	private Socket socket;


	public KeyFilesClientConnection(Socket socket) {
		this.socket = socket;
	}

	public void run() {
		try {	
			displayMessage(socket);

			log.info("Finished command");
		} catch(Throwable e) {
			log.error("Unhandled exception: ", e);
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				log.error("Closing connection failed: " + e.getMessage());
			}
		}
	}

	private void displayMessage(Socket clientSock) throws IOException {
		InputStream dis = clientSock.getInputStream();	
		
		String sender = readLine(dis);
		String cipherText = readFromStream(dis);
		
		String cipherFileName = RandomStringUtils.randomAlphanumeric(10);
		File cipherFile = new File(cipherFileName);
		FileUtils.write(cipherFile, cipherText);
		
	
		String decryptedMsg = KeybaseCommandLine.decrypt(cipherFileName);
		System.out.print("Message from " + sender + ": " + decryptedMsg);
		
		cipherFile.delete();
		
	}


}

