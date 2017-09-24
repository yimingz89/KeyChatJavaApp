package org.keychat.net.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This starts a KeyChat client.
 */
public class KeyChatClientApp extends KeyChatClientBase implements Runnable
{

    private static final String EXIT = "exit";
    private static final String LIST = "list";
    private static final String SENDMESSAGE = "send-message";
    private static final String HELPSENDMESSAGE = "help-send-message";
    private static final int CLIENT_CONNECT_TIMEOUT = 10000;

    private static final Logger log = LoggerFactory.getLogger(KeyChatClientApp.class);

    private Socket serverSocket; // socket for connecting with main server
    private Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws Exception {

        if(args.length != 7) {
            printUsage();
            return;
        }


        String serverAddress = null;
        int serverPort = -1;
        String user = null;
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

            case "--client-server-port":
                clientServerPort = Integer.parseInt(args[++i]);
                break;

            default:
                printUsage();
            }
        }

        if(serverAddress == null || serverPort < 0 || user == null || clientServerPort < 0) {
            printUsage();
            return;
        }	

        try {
            // launch the client thread
            Socket firstClientSocket = connect(serverAddress, serverPort);
            Thread clientThread = new Thread(new KeyChatClientApp(firstClientSocket, user, clientServerPort), "KeyChat client thread");
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
    public KeyChatClientApp(Socket socket, String user, int clientServerPort) throws IOException {
        this.user = user;
        this.clientServerPort = clientServerPort;
        
        serverSocket = socket;
   }

    /**
     * Prints usage
     */
    private static void printUsage() {
        System.err.println("java KeyChatClientApp --connect server_address server_port --username user_name  --client-server-port client_server_port");
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
            KeybaseCommandLine.KeybaseLogin(user);
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

                case SENDMESSAGE:
                    sendMessage(pw, br);
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
    private void helpSendMessage(PrintWriter pw, BufferedReader br) throws IOException {

        // get receiver's username from console
        System.out.print("Enter the receiver's username: ");
        String receiverName = scanner.nextLine();
        
        // get message from console
        System.out.print("Enter message: ");
        String message = scanner.nextLine();
        
        // tell the main server the HELPSENDMESSAGE command and give the receiverName and message              
        String encryptedMsg;
        try {
            encryptedMsg = KeybaseCommandLine.encrypt(message, receiverName);
            pw.println(HELPSENDMESSAGE);
            pw.println(user);
            pw.println(receiverName);
            pw.println(encryptedMsg.getBytes().length);
            pw.print(encryptedMsg);
            pw.flush();
        }
        catch (Exception e){
            log.error("Could not encrypt and send message");
        }
        
        // get status back from main server
        String status = br.readLine();
        log.info(status);
        
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
     * Sends a message by connecting directly to the receiver without relaying through the main server
     * @param pw
     * @param br
     * @throws IOException
     */
    public void sendMessage(PrintWriter pw, BufferedReader br) throws IOException {
        String receiverName;
        String message;
        String receiverAddress;
        int receiverPort;

        // get message from console
        System.out.print("Enter message: ");
        message = scanner.nextLine();

        // get receiver's username from console
        System.out.print("Enter the receiver's username: "); 
        receiverName = scanner.nextLine();

        // send over the SENDMESSAGE command and the receiverName to main server
        pw.println(SENDMESSAGE);
        pw.println(receiverName);
        pw.flush();

        // receive the address and port of recipient
        receiverAddress = br.readLine(); 
        receiverPort = Integer.parseInt(br.readLine());

        if(receiverPort == -1) {
            log.info("User " + receiverName + " not online");
            return;
        }

        // connect to receiver's server here
        Socket socket;
        try {
            socket = connectToReceiver(receiverAddress, receiverPort);
        } catch (SocketTimeoutException e) {
            log.error("Could not connect to receiver's server");
            return;
        } 

        // set PrintWriter for writing directly to recipient 
        OutputStream output = socket.getOutputStream();
        PrintWriter clientpw = new PrintWriter(new OutputStreamWriter(output));

        // encrypt (using Keybase command line) and send over message 
        String encryptedMsg;
        try {
            encryptedMsg = KeybaseCommandLine.encrypt(message, receiverName);
            clientpw.println(user);
            clientpw.println(encryptedMsg);
            clientpw.flush();
        }
        catch (Exception e){
            log.error("Could not encrypt and send message");
        }
        
        
        output.close();
        
        
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