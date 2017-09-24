package org.keychat.net.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the connection between a sender and a recipient's server.
 */
public class KeyChatClientConnection implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(KeyChatClientConnection.class);
	private static final String tempFolder = System.getProperty("java.io.tmpdir"); // name of the temporary folder where the file for storing the encrypted message is saved

	private Socket socket;


	public KeyChatClientConnection(Socket socket) {
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

	/**
	 * Decrypts and displays the message on the console
	 * @param clientSock
	 * @throws IOException
	 */
	private void displayMessage(Socket clientSock) throws IOException {
		InputStream dis = clientSock.getInputStream(); // InputStream from sender	
		
		BufferedReader br = new BufferedReader(new InputStreamReader(dis));
		String sender = br.readLine(); // read in sender
		String cipherText = IOUtils.toString(br); // get encrypted text from sender as a String
		
		String cipherFileName = tempFolder + RandomStringUtils.randomAlphanumeric(10);
		File cipherFile = new File(cipherFileName);
		FileUtils.write(cipherFile, cipherText); // write encrypted text to a temporary file
		
		// decrypt (using Keybase command line) and display on console
		String decryptedMsg = KeybaseCommandLine.decrypt(cipherFileName); 
		System.out.print("Message from " + sender + ": " + decryptedMsg);
		
		cipherFile.delete(); // delete temporary file
		
	}


}

