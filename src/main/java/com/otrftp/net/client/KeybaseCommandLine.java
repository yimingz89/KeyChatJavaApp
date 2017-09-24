package com.otrftp.net.client;
import java.io.*;
import java.util.Scanner;

public class KeybaseCommandLine {

    public static void main(String[] args) throws Exception {

        Scanner input = new Scanner(System.in);
        String username;
        do {
            System.out.print("Enter Keybase Username: ");
            username = input.nextLine();
        } while (!KeybaseLogin(username));

        String ciphertext = encrypt("message", "test_account");
        System.out.println("Encrypted message:\n" + ciphertext);
        System.out.println("Decrypted message:\n " + decrypt(ciphertext));
        input.close();
    }

    public static boolean KeybaseLogin(String username) throws IOException {

        boolean isSuccessful = true;
        Process p = Runtime.getRuntime().exec("keybase login " + username);

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        String s;
        // read the output from the command
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        // read any errors from the attempted command
        while ((s = stdError.readLine()) != null) {
            // System.out.println(s);
            isSuccessful = false;
        }
        return isSuccessful;
    }

    public static String encrypt(String message, String recipient) throws IOException {
        String s;

        String result = "";
        Process p = Runtime.getRuntime().exec("keybase pgp encrypt -m " + "\"" + message + "\"" + " " + recipient);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        // read the output from the command
        while ((s = stdInput.readLine()) != null) {
            result += s + "\n";
        }

        // read any errors from the attempted command
        while ((s = stdError.readLine()) != null) {
            result += s;
        }

        return result;
    }

    public static String decrypt(String fileName) throws IOException {
        String s;

        String result = "";
        
        String osName = System.getProperty("os.name");
        
        String shell = "bash";
        String shellOption = "-c";
        
        if (osName.startsWith("Windows")) {
            shell = "cmd";
            shellOption = "/c";
        }

        
        String cmd = "keybase pgp decrypt -i " + fileName;
        

        Process p = Runtime.getRuntime()
                .exec(new String[] { shell, shellOption, cmd });
        
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        // read the output from the command
        while ((s = stdInput.readLine()) != null) {
            result += s;
        }

        // read any errors from the attempted command
        while ((s = stdError.readLine()) != null) {
            result += s;
        }

        return result;
    }
}