package org.keychat.net.client;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.Security;

import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallbacks;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.InMemoryKeyring;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs;

/**
 * This starts a KeyChat client.
 */
public class KeyChatClientApp extends KeyChatClientBase implements Runnable
{

	private static final String EXIT = "exit";
	private static final String LIST = "list";
	private static final String HELPSENDMESSAGE = "help-send-message";
	private static final int CLIENT_CONNECT_TIMEOUT = 10000;


	private static final Logger log = LoggerFactory.getLogger(KeyChatClientApp.class);

	private Socket serverSocket; // socket for connecting with main server
	private Scanner scanner = new Scanner(System.in);

	public static InMemoryKeyring keyring = null;
	public static Map<String, String> uidMap = new ConcurrentHashMap<>();

	public static void main(String[] args) throws Exception {

		if(args.length != 9) {
			printUsage();
			return;
		}


		String serverAddress = null;
		int serverPort = -1;
		String user = null;
		String password = null;
		int clientServerPort = -1;
		for (int i=0; i < args.length; i++) {
			switch (args[i]) {
			case "--connect":
				serverAddress = args[++i];
				serverPort = Integer.parseInt(args[++i]);
				break;

			case "--username":
				user = args[++i];
				break;

			case "--password":
				password = args[++i];
				break;

			case "--client-server-port":
				clientServerPort = Integer.parseInt(args[++i]);
				break;

			default:
				printUsage();
			}
		}

		if(serverAddress == null || serverPort < 0 || user == null || password == null || clientServerPort < 0) {
			printUsage();
			return;
		}

		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}

		keyring = KeyringConfigs
				.forGpgExportedKeys(KeyringConfigCallbacks.withUnprotectedKeys());



		try {
			// launch the client thread
			Socket firstClientSocket = connect(serverAddress, serverPort);
			Thread clientThread = new Thread(new KeyChatClientApp(firstClientSocket, user, password, clientServerPort), "KeyChat client thread");
			clientThread.start();

			// launch second client connection for receiving commands/messages from server
			Socket secondClientSocket = connect(serverAddress, serverPort);
			Thread secondConnection = new Thread(new KeyChatSecondConnection(secondClientSocket, user, clientServerPort), "Second connection thread");
			secondConnection.setDaemon(true);
			secondConnection.start();


			// launch the server thread of this client for receiving messages from other clients
			Thread clientServer = new Thread(new KeyChatClientServer(clientServerPort), "Client server thread");
			clientServer.setDaemon(true);
			clientServer.start();


			clientThread.join();

			return;


		} catch (Exception e) {
			log.error("Error starting ClientApp", e);
			return;
		}


	}

	/**
	 * Constructor for KeyChatClientApp
	 * @param socket is the socket for connecting with the main server
	 * @param user is the Keybase username
	 * @param clientServerPort is the port of the server this KeyChat Client will run
	 * @throws IOException
	 */
	public KeyChatClientApp(Socket socket, String user, String password, int clientServerPort) throws IOException {
		this.user = user;
		this.password = password;
		this.clientServerPort = clientServerPort;
		serverSocket = socket;
	}

	/**
	 * Prints usage
	 */
	private static void printUsage() {
		System.err.println("Usage: java KeyChatClientApp --connect server_address server_port --username user_name --password password  --client-server-port client_server_port --public-key-path public_key_path --user-id user_id");
	}


	/**
	 * Connects to server given the address and port
	 * @param serverAddress
	 * @param serverPort
	 * @return the connection socket
	 * @throws IOException
	 */
	public static Socket connect(String serverAddress, int serverPort) throws IOException {
		InetSocketAddress sockAddr = new InetSocketAddress(InetAddress.getByName(serverAddress), serverPort);

		Socket socket = new Socket();
		socket.connect(sockAddr, CLIENT_CONNECT_TIMEOUT);

		return socket;
	}

	@Override
	public void run() {

		// login to Keybase
		try {
			//KeybaseCommandLine.KeybaseLogin(user);

			String decryptedSecretKey = KeybaseLogin.login_to_Keybase(this.user, this.password)[0];
			byte[] decryptedSecretKeyBytes = DatatypeConverter.parseHexBinary(decryptedSecretKey);
			keyring.addSecretKey(decryptedSecretKeyBytes);
			String userID = keyring.getSecretKeyRings().getKeyRings().next().getPublicKey().getUserIDs().next();
			
			int start = userID.indexOf('<');
			uid = userID.substring(start+1, userID.length()-1);
		}
		catch(Throwable e) {
			log.error("Unable to login to Keybase");
		}

		// set PrintWriter for writing to the main server and BufferedReader for reading from the main server
		try (PrintWriter pw = new PrintWriter(serverSocket.getOutputStream());
				BufferedReader br = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()))) {

			appearOnline(pw);

			boolean repeat = true;
			while (repeat) {
				String command = getCommandFromUser();
				command = command.toLowerCase();
				switch (command) {
				case LIST:
					list(pw, br);
					break;

				case EXIT:
					repeat = false;
					break;

				case HELPSENDMESSAGE:
					helpSendMessage(pw, br);
					break;

				default:
					break;
				}
			}

		} catch(Throwable e) {
			log.error("Unhandled exception: ", e);
		} finally {
			try {
				serverSocket.close();
			} catch (IOException e) {
				log.error("Closing  failed: " + e.getMessage());
			}
		}
	}

	/**
	 * Asks the server to help relay a message to another user
	 * @param pw
	 * @param br
	 * @throws IOException
	 */
	private void helpSendMessage(PrintWriter pw, BufferedReader br) throws Exception {

		// get receiver's username from console
		System.out.print("Enter the receiver's username: ");
		String receiverName = scanner.nextLine();

		// get message from console
		System.out.print("Enter message: ");
		String message = scanner.nextLine();

		String receiverPK;
		
		// create tempKeyring to retrieve receiver's uid
		InMemoryKeyring tempKeyring = KeyringConfigs
				.forGpgExportedKeys(KeyringConfigCallbacks.withUnprotectedKeys());
		// check if username is in hash map
		if(!uidMap.containsKey(receiverName)) {
			receiverPK = getPublicKey(receiverName);
			if(receiverPK == null) {
				log.info("Unable to get public key for " + receiverName);
				return;
			}
			tempKeyring.addPublicKey(receiverPK.getBytes());
			keyring.addPublicKey(receiverPK.getBytes());
			String receiverUid = tempKeyring.getPublicKeyRings().getKeyRings().next().getPublicKey().getUserIDs().next();
			
			int start = receiverUid.indexOf('<');
			String receiverEmail = receiverUid.substring(start+1, receiverUid.length()-1);
			uidMap.put(receiverName, receiverEmail);
		}


		// send over HELPSENDMESSAGE command
		pw.println(HELPSENDMESSAGE);
		pw.flush();

		// tell the main server the HELPSENDMESSAGE command and give the receiverName and message              
		String encryptedMsg;
		try {
			pw.println(user);
			pw.println(receiverName);
			pw.flush();
			// get status back from main server
			String status = br.readLine();
			log.info("User " + receiverName + " is " + status);
			
			if(status.equals("offline")) {
				return;
			}

			encryptedMsg = encryptMessage(message, uidMap.get(receiverName));
			//            encryptedMsg = KeybaseCommandLine.encrypt(message, receiverName);
			pw.println(encryptedMsg.getBytes().length);
			pw.print(encryptedMsg);
			pw.flush();
		}
		catch (Exception e){
			log.error("Could not encrypt and send message");
		}



	}

	
	private String getPublicKey(String username) throws Exception {
		String url = "https://keybase.io/_/api/1.0/user/lookup.json?username=" + username;
		HttpClient client = HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build()).build();
		HttpGet request = new HttpGet(url);
		HttpResponse response = client.execute(request);
		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
				
		JsonObject resultObj = new JsonParser().parse(result.toString()).getAsJsonObject();
		int status = resultObj.get("status").getAsJsonObject().get("code").getAsInt();
		if(status != 0) {
			return null;
		}
		String bundle = resultObj.get("them").getAsJsonObject().get("public_keys").getAsJsonObject().get("primary").getAsJsonObject().get("bundle").getAsString();
		
		/** 
		TODO: Check this key against the one in Catena by getting the keymaterial of this key, applying SHA-256 to it, 
		then checking this hash against the payload in the Keybase KID (bytes 3 to 34)
		**/
		return bundle;
		
	}


	/**
	 * Connects to another user's server given the address and port of that server
	 * @param address
	 * @param port
	 * @return the connection socket 
	 * @throws IOException
	 */
	public Socket connectToReceiver(String address, int port) throws IOException {
		InetSocketAddress sockAddr = new InetSocketAddress(InetAddress.getByName(address), port);
		Socket receiverSocket = new Socket();
		receiverSocket.connect(sockAddr, CLIENT_CONNECT_TIMEOUT);	
		return receiverSocket;
	}
	
	/**
	 * Encrypts a message under a given public key
	 * @param message is the plaintext
	 * @return the encrypted message
	 */
	private String encryptMessage(String message, String receiverUid) throws Exception {
		System.out.println("Encrypting ... ");

		ByteArrayOutputStream encrypted = new ByteArrayOutputStream();

		final OutputStream outputStream = BouncyGPG.encryptToStream()
				.withConfig(keyring)
				.withStrongAlgorithms()
				.toRecipient(receiverUid)
				//.andDoNotSign()
				.andSignWith(uid)
				.armorAsciiOutput()
				.andWriteTo(encrypted);
		
		System.out.println("User ID: " + uid);
		System.out.println("Receiver UID: " + receiverUid);

		outputStream.write(message.getBytes());
		outputStream.close();
		encrypted.close();

		final String cipherText = encrypted.toString();
		System.out.println("Encrypted:");
		System.out.println(cipherText);
		return cipherText;
	}

	/**
	 * Asks server to return all users currently online
	 * @param pw
	 * @param br
	 * @throws IOException
	 */
	private void list(PrintWriter pw, BufferedReader br) throws IOException {
		pw.println(LIST);
		pw.flush();
		System.out.println(br.readLine());
	}

	/**
	 * Gets a command from the console
	 * @return
	 */
	private String getCommandFromUser() {
		System.out.print("Command: ");
		String command = scanner.nextLine();
		return command;
	}

}