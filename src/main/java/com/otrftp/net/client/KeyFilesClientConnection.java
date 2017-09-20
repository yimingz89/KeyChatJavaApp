package com.otrftp.net.client;

import static com.otrftp.common.IOUtils.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
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
			saveFile(socket);

			log.info("Finished command");
			//assert filePath != null;

			// TODO: send file here!
			//succeeded = true;
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

	private void saveFile(Socket clientSock) throws IOException {
		InputStream dis = clientSock.getInputStream();
		//BufferedReader br = new BufferedReader(new InputStreamReader(dis));
		
		String message = readLine(dis);
	
	
		String decryptedMsg = KeybaseCommandLine.decrypt(message);
		System.out.println("Message: " + decryptedMsg);
		
	}


}

