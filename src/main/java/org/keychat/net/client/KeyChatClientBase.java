package org.keychat.net.client;

import java.io.IOException;
import java.io.PrintWriter;


/**
 * This is a superclass for both KeyChatClientApp and KeyChatSecondConnection.
 */
public class KeyChatClientBase {

    protected String user;
    protected String password;
    protected int clientServerPort;
    protected String uid;

    public KeyChatClientBase() {
        super();
    }

    /**
     * Given a PrintWriter, writes the user and their server port
     * @param pw
     * @throws IOException
     */
    protected void appearOnline(PrintWriter pw) throws IOException {
    	pw.println(user);
        pw.println(clientServerPort);
        pw.flush();
    }

}