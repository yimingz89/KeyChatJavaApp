package com.otrftp.common;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;

/**
 * This is a utility class for handling i/o.
 */
public class IOUtils {
    
    static final int MAX_FILE_SIZE = 1000000;
    
    /**
     * Reads a line from provided input stream
     * 
     * @param is
     * @return a string with the line terminator removed
     * @throws IOException
     */
    public static String readLine(InputStream is) throws IOException {
        int read = 0;
        
        byte[] bytes = new byte[100];
        int index = 0;
        while((read = is.read()) != '\n') {
            bytes[index++] = (byte) read;
        }
        
        int length = index;
        if (bytes[index-1] == '\r') {
            length--;
        }
        
        
        return new String(bytes, 0, length);
    }
    
    /**
     * Reads a series of lines from an input stream into a single string
     * @param is
     * @return a single string
     * @throws IOException
     */
    public static String readFromStream(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = br.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }
        
        return sb.toString().substring(0, sb.length()-1);
    }
    
    /**
     * Reads a series of lines with a specified length in bytes from an input stream into a single string 
     * @param is
     * @param length
     * @return 
     * @throws IOException
     */
    public static String readFromStream(InputStream is, int length) throws IOException {
        byte[] bytes = new byte[length];
        
        int bytesRead = 0;
        int bytesRemaining = length;
        while(bytesRead < length) {
            bytesRead += is.read(bytes, bytesRead, bytesRemaining);
            bytesRemaining -= bytesRead;
        }
        
        return new String(bytes);
    }
    
    
    /**
     * Reads a series of lines with a specified length in bytes from a reader into a single string 
     * @param is
     * @param length
     * @return 
     * @throws IOException
     */
    public static String readFromStream(Reader r, int length) throws IOException {
        char[] bytes = new char[length];
        
        int bytesRead = 0;
        int bytesRemaining = length;
        while(bytesRead < length) {
            bytesRead += r.read(bytes, bytesRead, bytesRemaining);
            bytesRemaining -= bytesRead;
        }
        
        return new String(bytes);
    }
    
    /**
     * Copies specified number of bytes from the provided input stream to the provided output stream
     * 
     * @param is
     * @param os
     * @param totalBytes
     * @throws IOException
     */
    public static void copyStream(InputStream is, OutputStream os, long totalBytes) throws IOException {
        byte[] buffer = new byte[4096];

        long remainingBytes = totalBytes;
        int bytesRead = 0;
        while (remainingBytes > 0) {
            if(remainingBytes < buffer.length) {
                bytesRead = is.read(buffer, 0, (int) remainingBytes);
            }
            else {
                bytesRead = is.read(buffer, 0, buffer.length);
            }
            os.write(buffer, 0, bytesRead);
            remainingBytes -= bytesRead;
        }

    }
    
    public static byte[] read(File file) throws IOException {
        if (file.length() > MAX_FILE_SIZE) {
            throw new IOException();
        }
        ByteArrayOutputStream ous = null;
        InputStream ios = null;
        try {
            byte[] buffer = new byte[4096];
            ous = new ByteArrayOutputStream();
            ios = new FileInputStream(file);
            int read = 0;
            while ((read = ios.read(buffer)) != -1) {
                ous.write(buffer, 0, read);
            }
        }finally {
            try {
                if (ous != null)
                    ous.close();
            } catch (IOException e) {
            }

            try {
                if (ios != null)
                    ios.close();
            } catch (IOException e) {
            }
        }
        return ous.toByteArray();
    }
}
