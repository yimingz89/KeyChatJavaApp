package org.keychat.net.client;

import java.io.*;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.binary.Base64;
import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.value.ValueFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.lambdaworks.crypto.SCrypt;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;

import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

public class KeybaseLogin {

	// the scrypt parameters here are the same as Keybase's
	static int N = 32768;
	static int r = 8;
	static int p = 1;
	static int len = 256;

	// these two are the parameters
	static String testUsername = "Test_Account";
	static String testPassword = "testpassword";

	static byte[] passwordStream;
	static String login_session;

	public static void main(String[] args) throws Exception {
		String[] returns = login_to_Keybase(testUsername, testPassword);
		System.out.println(returns[0]);
	}

	/**
	 * Logins to Keybase
	 * @param username
	 * @param password
	 * @return secret key (decrypted with go-triplesec script) along with any error message
	 * @throws Exception
	 */
	public static String[] login_to_Keybase(String username, String password) throws Exception {
		String[] returns = new String[2];
		String decryptedSecretKey = "";
		String error = "";
		// getsalt request
		String url = "https://keybase.io/_/api/1.0/getsalt.json?email_or_username=" + username + "&pdpka_login=true";
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

		// parse jSON result and get login_session
		JsonObject resultObj = new JsonParser().parse(result.toString()).getAsJsonObject();
		String salt = resultObj.get("salt").getAsString();
		System.out.println("salt: " + salt);
		login_session = resultObj.get("login_session").getAsString();
		System.out.println("login_session: " + login_session);

		// check we're logged in from response (example of bad login response:
		// result is {"status":{"code":100,"desc":"bad username or
		// email","fields":{"email_or_username":"bad username or
		// email"},"name":"INPUT_ERROR"}}
		if (resultObj.get("status").getAsJsonObject().get("code").getAsString().equals("0")
				&& resultObj.get("status").getAsJsonObject().get("name").getAsString().equals("OK")) {
			System.out.println("good login response.");
		} else {
			System.exit(0);
		}

		// compute passwordStream, v4, and v5
		passwordStream = SCrypt.scrypt(password.getBytes(), DatatypeConverter.parseHexBinary(salt), N, r, p, len);
		System.out.println("passwordStream: " + DatatypeConverter.printHexBinary(passwordStream));
		byte[] v4 = Arrays.copyOfRange(passwordStream, 192, 224);
		byte[] v5 = Arrays.copyOfRange(passwordStream, 224, 256);
		System.out.println("v4: " + DatatypeConverter.printHexBinary(v4));
		System.out.println("v5: " + DatatypeConverter.printHexBinary(v5));

		// compute pdpkas
		String pdpka5 = compute_pdpka(v5, username);
		System.out.println("pdpka5: " + pdpka5);
		String pdpka4 = compute_pdpka(v4, username);
		System.out.println("pdpka4: " + pdpka4);

		// http post request
		HttpClient httpclient = HttpClients.custom()
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build()).build();
		HttpPost httppost = new HttpPost("https://keybase.io/_/api/1.0/login.json");

		// Request parameters and other properties
		List<NameValuePair> params = new ArrayList<NameValuePair>(2);
		params.add(new BasicNameValuePair("email_or_username", username));
		params.add(new BasicNameValuePair("pdpka5", pdpka5));
		params.add(new BasicNameValuePair("pdpka4", pdpka4));
		httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

		// Execute and get the response
		HttpResponse response2 = httpclient.execute(httppost);
		HttpEntity entity = response2.getEntity();

		// Print the response
		if (entity != null) {
			InputStream inputStream = entity.getContent();
			try {
				Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
				String result2 = scanner.hasNext() ? scanner.next() : "";
				System.out.println(result2);
				JsonObject resultObj2 = new JsonParser().parse(result2.toString()).getAsJsonObject();

				String sessionCookie = resultObj2.get("session").getAsString();
				System.out.println("session cookie: " + sessionCookie);
				String bundle = resultObj2.get("me").getAsJsonObject().get("private_keys").getAsJsonObject()
						.get("primary").getAsJsonObject().get("bundle").getAsString();
				System.out.println("bundle: " + bundle);

				byte[] decoded = Base64.decodeBase64(bundle);
				MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(decoded);

				MessageFormat format = unpacker.getNextFormat();
				System.out.println("type " + format.getValueType().name());
				int length = unpacker.unpackMapHeader();
				System.out.println("length " + length);
				format = unpacker.getNextFormat();
				System.out.println("type " + format.getValueType().name());
				System.out.println("string: " + unpacker.unpackString());
				format = unpacker.getNextFormat();
				System.out.println("type " + format.getValueType().name());
				length = unpacker.unpackMapHeader();
				System.out.println("length " + length);
				format = unpacker.getNextFormat();
				System.out.println("type " + format.getValueType().name());
				System.out.println("string: " + unpacker.unpackString());
				format = unpacker.getNextFormat();
				System.out.println("type " + format.getValueType().name());
				length = unpacker.unpackMapHeader();
				System.out.println("length " + length);
				format = unpacker.getNextFormat();
				System.out.println("type " + format.getValueType().name());
				System.out.println("string: " + unpacker.unpackString());
				format = unpacker.getNextFormat();
				System.out.println("type " + format.getValueType().name());
				length = unpacker.unpackBinaryHeader();
				System.out.println("length " + length);

				byte[] data = new byte[length];
				unpacker.readPayload(data);
				System.out.println("data: " + DatatypeConverter.printHexBinary(data).toLowerCase());

				System.out.println("\n\nKey Decrypted with Triplesec:\n");
				// using the Runtime exec method:
				Process p = Runtime.getRuntime()
						//.exec("triplesec -k " + password + " dec " + DatatypeConverter.printHexBinary(data).toLowerCase());
						.exec("./go/src/t/t " + password + " " + DatatypeConverter.printHexBinary(data).toLowerCase());

				BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

				BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

				String s = null;
				// read the output from the command
				while ((s = stdInput.readLine()) != null) {
					decryptedSecretKey += s;
					System.out.println(s);
				}

				// read any errors from the attempted command
				while ((s = stdError.readLine()) != null) {
					error += s;
					System.out.println(s);
				}
				scanner.close();
			} finally {
				inputStream.close();
			}
		}
		returns[0] = decryptedSecretKey;
		returns[1] = error;
		return returns;
	}

	public static String compute_pdpka(byte[] v, String username) throws IOException, GeneralSecurityException {
		// generate nonce
		SecureRandom random = new SecureRandom();
		byte[] nonce = new byte[16];
		random.nextBytes(nonce);
		System.out.println("nonce: " + DatatypeConverter.printHexBinary(nonce).toLowerCase());

		// compute edDSA public and private keys using v5/v4 as private key seed
		// example code:
		// http://www.programcreek.com/java-api-examples/index.php?source_dir=HAP-Java-master/src/main/java/com/beowulfe/hap/impl/crypto/EdsaSigner.java
		EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("Ed25519");
		EdDSAPrivateKeySpec privateKeySpec = new EdDSAPrivateKeySpec(v, spec);
		EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(privateKeySpec.getA(), spec);
		EdDSAPublicKey publicKey = new EdDSAPublicKey(pubKeySpec);
		EdDSAPrivateKey privateKey = new EdDSAPrivateKey(privateKeySpec);

		// construct kid
		byte[] pubKey = publicKey.getAbyte();
		byte[] a = { (byte) 0x01, (byte) 0x20 };
		byte[] b = { (byte) 0x0a };
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(a);
		outputStream.write(pubKey);
		outputStream.write(b);
		byte[] kid = outputStream.toByteArray();

		// construct jSON blob
		JsonObject jsonBlob = new JsonObject();
		JsonObject bodyObj = new JsonObject();
		JsonObject authObj = new JsonObject();
		authObj.addProperty("nonce", DatatypeConverter.printHexBinary(nonce).toLowerCase());
		authObj.addProperty("session", login_session);
		JsonObject keyObj = new JsonObject();
		keyObj.addProperty("host", "keybase.io");
		keyObj.addProperty("kid", DatatypeConverter.printHexBinary(kid).toLowerCase());
		keyObj.addProperty("username", username);
		bodyObj.add("auth", authObj);
		bodyObj.add("key", keyObj);
		bodyObj.addProperty("type", "auth");
		bodyObj.addProperty("version", 1);
		jsonBlob.add("body", bodyObj);
		jsonBlob.addProperty("ctime", (int) (System.currentTimeMillis() / 1000));
		jsonBlob.addProperty("expire_in", 157680000);
		jsonBlob.addProperty("tag", "signature");
		System.out.println(jsonBlob.toString());

		// compute sig parameter
		Signature sgr = new EdDSAEngine(MessageDigest.getInstance("SHA-512"));
		sgr.initSign(privateKey);
		sgr.update(jsonBlob.toString().getBytes());
		byte[] sig = sgr.sign();

		// construct Keybase-style signature
		// https://keybase.io/docs/api/1.0/sigs
		ValueFactory.MapBuilder keybaseStyleSig = ValueFactory.newMapBuilder();
		ValueFactory.MapBuilder body = ValueFactory.newMapBuilder();
		body.put(ValueFactory.newString("detached"), ValueFactory.newBoolean(true));
		body.put(ValueFactory.newString("hash_type"), ValueFactory.newInteger(10));
		body.put(ValueFactory.newString("key"), ValueFactory.newBinary(kid));
		body.put(ValueFactory.newString("payload"), ValueFactory.newBinary(jsonBlob.toString().getBytes()));
		body.put(ValueFactory.newString("sig"), ValueFactory.newBinary(sig));
		body.put(ValueFactory.newString("sig_type"), ValueFactory.newInteger(32));
		keybaseStyleSig.put(ValueFactory.newString("body"), body.build());
		keybaseStyleSig.put(ValueFactory.newString("tag"), ValueFactory.newInteger(514));
		keybaseStyleSig.put(ValueFactory.newString("version"), ValueFactory.newInteger(1));

		// encode Keybase-style signature as the byte array ser using
		// messagepack
		MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
		packer.packValue(keybaseStyleSig.build());
		byte[] ser = packer.toByteArray();

		// encode the messagepacked Keybase-style signature in base 64
		return Base64.encodeBase64String(ser);
	}

	public static String toPrettyFormat(String jsonString) {
		JsonParser parser = new JsonParser();
		JsonObject json = parser.parse(jsonString).getAsJsonObject();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String prettyJson = gson.toJson(json);
		return prettyJson;
	}

	// convert byte array to int array with values(0-255)
	public static int[] byteArrToHexArr(byte[] byteArr) {
		int[] arr = new int[byteArr.length];
		for (int i = 0; i < byteArr.length; i++) {
			arr[i] = (byteArr[i] + 256) % 256;
		}
		return arr;
	}

	static String toBinary(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
		for (int i = 0; i < Byte.SIZE * bytes.length; i++)
			sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
		return sb.toString();
	}
}
