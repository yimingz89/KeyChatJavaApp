package org.keychat.net.client;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class KeybaseMerkleTreeApp {

	public static void main(String[] args) throws IOException {
		String username = "lauritzt";// "test_account";
		String leafHash = "";
		// seqno=1547059
		// String hashFrom =
		// "7c48325006a18697c3dff6bdbeddaf7f4427e2ae10610d54752c3601dca48d39cccfbdc3c719f054b3f23de24e99f6fd13aa994e8d1a02196eee64c0c8f80834";
		// seqno=1547069
		// String hashTo =
		// "f02de9630791817bdf09be35f27b9ce46245efd8f367678d208db44ab251578321619a07f9c4b9d5c73d3ddfe587188eb8c8b37b17f31663dcdd15cd5b4be0c6";

		// seqno=408242
		String hashFrom = "8473d5c518e570d00e287159dc1552ffa989902b6113991d05e31cb7b32280d8e3df99d5d3e98a14a32a712f1ce9dc0b6582c4f8ec9b49313f99deb735bddff7";
		// seqno=408245
		String hashTo = "d6ffed9dcd8674c18e3b06eef820a4898e101875b587a9ebf6872dedbca3c280655a49d7eedf0253d646dd310643e7df75247a96b61ddcf5df9b718f577ec778";
		
		String rootHash = hashTo;
		
		monitorKids(username, hashFrom, hashTo);

		System.out.println("The kid is: " + getKid(username, rootHash));
	}

	private static String getKid(String username, String rootHash) throws IOException {
		String leafHash = "";

		// compute uid (also found here:
		// https://keybase.io/_/api/1.0/user/lookup.json?)
		String uid = org.apache.commons.codec.digest.DigestUtils.sha256Hex(username).substring(0, 30) + "19";
		System.out.println("uid: " + uid);

		String hash = rootHash; // current hash, changes as we traverse the
		                        // merkle tree
		boolean leafnode = false;
		int indexLength = 1;

		// traverse down tree according to uid until leaf node is reached
		while (!leafnode) {
			// merkle/block get request
			String url = "https://keybase.io/_/api/1.0/merkle/block.json?hash=" + hash;
			HttpClient client = HttpClients.custom()
			        .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
			        .build();
			HttpGet request = new HttpGet(url);
			HttpResponse response = client.execute(request);
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			// parse jSON result
			JsonObject fullResultObj = new JsonParser().parse(result.toString()).getAsJsonObject();

			// WARNING: some nodes have value strings that don't match the
			// main json (missing children)
			// in these cases, the hash matches the hash of the value
			// string, not the main json
			// resultObj has contents of value_string
			JsonObject resultObj = new JsonParser()
			        .parse(fullResultObj.get("value_string").getAsJsonPrimitive().getAsString()).getAsJsonObject();

			// verify node's hash against children
			if (!fullResultObj.get("hash").getAsString()
			        .equals(org.apache.commons.codec.digest.DigestUtils.sha512Hex(resultObj.toString()))) {
				throw new RuntimeException("**Could not verify node's hash**");
			}
			// check if leaf node
			if (fullResultObj.get("type").getAsInt() == 2) {
				leafnode = true;
				// get kid if account exists in this merkle tree
				if (fullResultObj.get("value").getAsJsonObject().get("tab").getAsJsonObject().has(uid)) {
					return fullResultObj.get("value").getAsJsonObject().get("tab").getAsJsonObject().get(uid)
					        .getAsJsonArray().get(3).toString();
				} else {
					return "No node exists for uid -- account not yet created.";
				}
			} else {
				// get correct child hash according to uid
				// WARNING: some old nodes have 256 children (00 to ff)
				// rather than 16 (0 to f)
				if (fullResultObj.get("value").getAsJsonObject().get("tab").getAsJsonObject()
				        .has(uid.substring(0, indexLength))) {
					// if 16 children
					hash = fullResultObj.get("value").getAsJsonObject().get("tab").getAsJsonObject()
					        .get(uid.substring(0, indexLength)).getAsString();
					indexLength++;
				} else {
					// if 256 children
					indexLength++;
					hash = fullResultObj.get("value").getAsJsonObject().get("tab").getAsJsonObject()
					        .get(uid.substring(0, indexLength)).getAsString();
					indexLength++;
				}

			}
		}
		return null;
	}

	public static void monitorKids(String username, String hashFrom, String hashTo) throws IOException {

		String leafHash = "";

		// compute uid (also found here:
		// https://keybase.io/_/api/1.0/user/lookup.json?)
		String uid = org.apache.commons.codec.digest.DigestUtils.sha256Hex(username).substring(0, 30) + "19";
		System.out.println("uid: " + uid);

		String hash = ""; // current hash, changes as we traverse the merkle
		                  // tree
		String prevHash = ""; // previous hash of merkle root
		String lastHash = "lastHash"; // hash of merkle root
		String kid = "";

		// loop from hashTo to hashFrom
		while (!lastHash.equals(hashFrom)) {
			if (hash.equals("")) {
				// if first loop
				lastHash = hash = hashTo;
			} else {
				lastHash = hash = prevHash;
			}
			boolean leafnode = false;
			int indexLength = 1;

			// traverse down tree according to uid until leaf node is reached
			while (!leafnode) {
				// merkle/block get request
				String url = "https://keybase.io/_/api/1.0/merkle/block.json?hash=" + hash;
				HttpClient client = HttpClients.custom()
				        .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
				        .build();
				HttpGet request = new HttpGet(url);
				HttpResponse response = client.execute(request);
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				StringBuffer result = new StringBuffer();
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
				// parse jSON result
				JsonObject fullResultObj = new JsonParser().parse(result.toString()).getAsJsonObject();

				// WARNING: some nodes have value strings that don't match the
				// main json (missing children)
				// in these cases, the hash matches the hash of the value
				// string, not the main json
				// resultObj has contents of value_string
				JsonObject resultObj = new JsonParser()
				        .parse(fullResultObj.get("value_string").getAsJsonPrimitive().getAsString()).getAsJsonObject();

				if (indexLength == 1) {
					// set prevHash after looking up root block
					prevHash = resultObj.get("prev_root").getAsString();
					System.out.println("prevHash: " + prevHash);
				}
				// verify node's hash against children
				if (!fullResultObj.get("hash").getAsString()
				        .equals(org.apache.commons.codec.digest.DigestUtils.sha512Hex(resultObj.toString()))) {
					throw new RuntimeException("**Could not verify node's hash**");
				}
				// check if leaf node
				if (fullResultObj.get("type").getAsInt() == 2) {
					leafnode = true;
					if (leafHash.equals("")) {
						// if first time (don't have a leafhash yet)
						leafHash = hash;
						System.out.println("leafHash is: " + leafHash);

						// get kid if account exists in this merkle tree

						if (fullResultObj.get("value").getAsJsonObject().get("tab").getAsJsonObject().has(uid)) {
							kid = fullResultObj.get("value").getAsJsonObject().get("tab").getAsJsonObject().get(uid)
							        .getAsJsonArray().get(3).toString();
						} else {
							throw new RuntimeException("No node exists for uid -- account not yet created.");
						}

						System.out.println("kid: " + kid);
					} else if (leafHash.equals(hash)) {
						// if leafHash didn't change
						System.out.println("leafHash did not change.");
					} else {
						// if leafHash changed
						leafHash = hash;
						System.out.println("leafHash used to be: " + leafHash);
						String currentKid = kid;
						// get kid
						kid = fullResultObj.get("value").getAsJsonObject().get("tab").getAsJsonObject().get(uid)
						        .getAsJsonArray().get(3).toString();

						if (!currentKid.equals(kid)) {
							// get type of pk change (eldest or pgp_update)

							System.out.println("Was your kid " + kid + " before, \nbut was changed to " + currentKid
							        + " at ctime " + fullResultObj.get("ctime").getAsString() + "? (y/n)");
							Scanner in = new Scanner(System.in);
							String answer = in.nextLine();

							if (!answer.equalsIgnoreCase("y") && !answer.equalsIgnoreCase("yes")) {
								in.close();
								throw new RuntimeException("You've been impersonated!");
							}
						}

						System.out.println("kid: " + kid);

					}

					// System.out.println(fullResultObj.get("value").getAsJsonObject().get("tab").getAsJsonObject().toString());
				} else {
					// get correct child hash according to uid
					// WARNING: some old nodes have 256 children (00 to ff)
					// rather than 16 (0 to f)
					if (fullResultObj.get("value").getAsJsonObject().get("tab").getAsJsonObject()
					        .has(uid.substring(0, indexLength))) {
						// if 16 children
						hash = fullResultObj.get("value").getAsJsonObject().get("tab").getAsJsonObject()
						        .get(uid.substring(0, indexLength)).getAsString();
						indexLength++;
					} else {
						// if 256 children
						indexLength++;
						hash = fullResultObj.get("value").getAsJsonObject().get("tab").getAsJsonObject()
						        .get(uid.substring(0, indexLength)).getAsString();
						indexLength++;
					}

				}
			}
		}
	}

	public static String toPrettyFormat(String jsonString) {
		JsonParser parser = new JsonParser();
		JsonObject json = parser.parse(jsonString).getAsJsonObject();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String prettyJson = gson.toJson(json);
		return prettyJson;
	}

}
