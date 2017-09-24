package com.otrftp.net.server;

import static com.otrftp.common.IOUtils.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KeyFilesServerConnection implements Runnable {

    private static final String EXIT = "exit";
    private static final String LIST = "list";
    private static final String SENDMESSAGE = "send-message";
    private static final String HELPSENDMESSAGE = "help-send-message";
    private static final Logger log = LoggerFactory.getLogger(KeyFilesServerConnection.class);


    private Socket socket;
    private BufferedReader br;
    private PrintWriter pw;

    private String userName;
    private String clientAddress;
    private int clientServerPort;

    public KeyFilesServerConnection(Socket socket) {
        this.socket = socket;
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            userName = br.readLine();
            clientServerPort = Integer.parseInt(br.readLine());
            clientAddress = socket.getInetAddress().getCanonicalHostName();
            
            User user =  findUser(userName);
            if(user.getPort() == -1) {
                user.setUser(userName);
                user.setAddress(clientAddress);
                user.setPort(clientServerPort);
                KeyFilesServerApp.usernames.add(user);
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
                String command = getCommandFromClient();
                if(command == null) {
                    log.info("Connection closed by client");
                    return;
                }



                command = command.toLowerCase();
                log.info("Received command '{}'", command);
                switch (command) {
                case LIST:
                    list();
                    break;

                case SENDMESSAGE:
                    sendMessage();
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

    private void helpSendMessage() throws IOException {
       
       // Read sender, receiverName, and message from sender
       
       String sender = br.readLine();
       String receiverName = br.readLine();
       String bytes = br.readLine();
              
       String message = readFromStream(br, Integer.parseInt(bytes));
              
       // Find receiver connection information online
       User user = findUser(receiverName);
       
       // Check if receiver is online
       if(user.getPort() == -1) {
           // receiver not online
           log.error("User not online");
           pw.println("User not online");
           pw.flush();
           return;
       }
       
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
       pw.println("Message sent to " + receiverName + " successfully");
       pw.flush();
       
    }

    private void list() {
        pw.println(KeyFilesServerApp.usernames);
        pw.flush();
    }

    private void sendMessage() throws IOException {
        String receiverName = br.readLine();
        User user = findUser(receiverName);

        pw.println(user.getAddress());
        pw.println(user.getPort());
        pw.flush();

    }

    private User findUser(String receiverName) {
        User userFound = new User(receiverName, "", -1);
        for(User user : KeyFilesServerApp.usernames) {
            if(user.getUser().equalsIgnoreCase(receiverName)) {
                userFound = user;
                break;
            }
        }
        return userFound;
    }

    private String getCommandFromClient() throws IOException {
        return br.readLine();
    }
}
