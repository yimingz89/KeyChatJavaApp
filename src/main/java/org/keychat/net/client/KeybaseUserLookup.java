package org.keychat.net.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class KeybaseUserLookup {

	public static String testUsername = "keybaseuser123";
	public static void main(String[] args) throws Exception {
		String publicKey = getPublicKey(testUsername);
		System.out.println("public key: " + publicKey);
	}
	
	
	public static String getPublicKey(String username) throws Exception {
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
		String bundle = resultObj.get("them").getAsJsonObject().get("public_keys").getAsJsonObject().get("primary").getAsJsonObject().get("bundle").getAsString();
		
		return bundle;
		
	}

}
