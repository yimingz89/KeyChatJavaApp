package org.keychat.net.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

/**
 * This is a superclass for both KeyChatClientApp and KeyChatSecondConnection.
 */
public class KeyChatClientBase {

    protected String user;
    protected String password;
    protected int clientServerPort;
    protected Path publicKeyPath;
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
        String publicKey = IOUtils.toString(new FileInputStream(publicKeyPath.toFile()));
        //pw.println(publicKey.getBytes().length);
        //pw.print(publicKey);
        //pw.println(uid);
        pw.flush();
    }

}