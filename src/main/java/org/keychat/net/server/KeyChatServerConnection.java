package org.keychat.net.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the connection between a user and the main server.
 */
public class KeyChatServerConnection implements Runnable {

	private static final String EXIT = "exit";
	private static final String LIST = "list";
	private static final String HELPSENDMESSAGE = "help-send-message";
	private static final Logger log = LoggerFactory.getLogger(KeyChatServerConnection.class);


	private Socket socket;
	private BufferedReader br;
	private PrintWriter pw;

	private String userName;
	private String clientAddress;
	private int clientServerPort;

	public KeyChatServerConnection(Socket socket) {
		this.socket = socket;
		try {
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			// read user's username and server port, and get their address
			userName = br.readLine();
			clientServerPort = Integer.parseInt(br.readLine());
			//int publicKeyBytes = Integer.parseInt(br.readLine());

			//char[] chars = new char[publicKeyBytes];
			//IOUtils.readFully(br, chars);
			//String publicKey = new String(chars);
			//String uid = br.readLine();
			clientAddress = socket.getInetAddress().getCanonicalHostName();


			KeyChatUser user =  findUser(userName); 

			// get user information
			if(user.getPort() == -1) {
				user.setUser(userName);
				user.setAddress(clientAddress);
				user.setPort(clientServerPort);
//				user.setPublicKey(publicKey);
//				user.setUid(uid);
				KeyChatServerApp.onlineUsers.add(user);
			}

			user.getSocket().add(socket);
		} catch (IOException e) {
			log.error("Unhandled exception: ", e);
		}

	}

	public void run() {
		try {	


			boolean repeat = true;
			while (repeat) {
				String command = br.readLine();
				if(command == null) {
					log.info("Connection closed by client");
					return;
				}



				command = command.toLowerCase();
				log.info("Received "
						+ "command '{}'", command);
				switch (command) {
				case LIST:
					list();
					break;

				case EXIT:
					repeat = false;
					break;

				case HELPSENDMESSAGE:
					helpSendMessage();
					break;

				default:
					break;
				}

				log.info("Finished command");
			}

		} catch (SocketException se) {
			log.info("User " + userName +  " has disconnected");
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
	 * Used when receiving the HELPSENDMESSAGE command from a user, relays the sender name, number of bytes in the message, and the message to the recipient
	 * @throws IOException
	 */
	private void helpSendMessage() throws IOException {

		// Read sender, receiverName, and message from sender

		String sender = br.readLine();
		String receiverName = br.readLine();

		// Find receiver connection information online
		KeyChatUser user = findUser(receiverName);

		// Check if receiver is online
		if(user.getPort() == -1) {
			// receiver not online
			log.info("User " + receiverName + " not online");
			pw.println("offline");
			pw.flush();
			return;
		}
		else {
			log.info("User " + receiverName + " is online");
			pw.println("online");
			pw.flush();
		}

		String bytes = br.readLine();
		char[] chars = new char[Integer.parseInt(bytes)];
		IOUtils.readFully(br, chars);
		String message = new String(chars);

		// Open output stream to write to the receiver
		OutputStream os = user.getSocket().get(1).getOutputStream();

		// Write HELPSENDMESSAGE command and forward message to receiver
		PrintWriter pwReceiver = new PrintWriter(os);
		pwReceiver.println(HELPSENDMESSAGE);
		pwReceiver.println(sender);
		pwReceiver.println(bytes);
		pwReceiver.print(message);
		pwReceiver.flush();

		// Report to sender message was forward successfully
		//       pw.println("Message sent to " + receiverName + " successfully");
		pw.flush();

	}

	/**
	 * Sends all users currently online to a client
	 */
	private void list() {
		pw.println(KeyChatServerApp.onlineUsers);
		pw.flush();
	}

	/**
	 * Searches for a KeyChat user given the username in the online directory
	 * @param receiverName
	 * @return the desired user, or a null user if that user is not online
	 */
	private KeyChatUser findUser(String receiverName) {
		KeyChatUser userFound = new KeyChatUser(receiverName, "", -1);
		for(KeyChatUser user : KeyChatServerApp.onlineUsers) {
			if(user.getUser().equalsIgnoreCase(receiverName)) {
				userFound = user;
				break;
			}
		}
		return userFound;
	}
}
