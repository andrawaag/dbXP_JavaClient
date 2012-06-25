import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

/*
The MIT License (MIT)
Copyright (c) 2012 andra.waagmeester@maastrichtuniversity.nl

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


public class dbxpJavaClient {

	
	//User specific credentials old studies 
	private static String userName = "";
	private static String password = "";
	private static String host = "";
	private static String baseApiUrl = "";
    private static String apiKey = "";
    
	//Local variables
	private static String authenticate=null;
	private static String token="";
	private static int sequence=0;


	/**
	 * @param args
	 * @throws SocketException 
	 * @throws UnknownHostException 
	 * @throws NoSuchAlgorithmException 
	 */

	public static String getMacAddress() throws SocketException, UnknownHostException{ 
		InetAddress ip = InetAddress.getLocalHost();
		NetworkInterface network = NetworkInterface.getByInetAddress(ip);
		byte[] mac = network.getHardwareAddress();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < mac.length; i++) {
			sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "" : ""));		
		}
		//System.out.println(sb.toString());
		return sb.toString();
	}
	
	public static String getDeviceId() throws NoSuchAlgorithmException, SocketException, UnknownHostException {
		String deviceId = getMacAddress()+userName;
		//System.out.println(deviceId);
		return getMD5Sum(deviceId);
	}

	public static String getMD5Sum(String variable) throws NoSuchAlgorithmException{	
		MessageDigest md=MessageDigest.getInstance("MD5");
		md.update(variable.getBytes());
		return new BigInteger(1,md.digest()).toString(16);
	}
	
	@SuppressWarnings("rawtypes")
	public static HttpResponse postValues(HashMap<String, String> postvars, String url) throws NoSuchAlgorithmException, ClientProtocolException, IOException{
		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(userName, password));

		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		Iterator it = postvars.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry next = (Map.Entry)it.next();
			Map.Entry<String, String> pairs = next;
			formparams.add(new BasicNameValuePair(pairs.getKey(), pairs.getValue()));
		}
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");	
		HttpPost httppost = new HttpPost(url);
		httppost.setEntity(entity);
		HttpContext localContext = new BasicHttpContext();
		return httpclient.execute(httppost, localContext);
	}

	public static void authenticate() throws IOException, NoSuchAlgorithmException{
		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(userName, password));

		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		HashMap<String, String> formvars = new HashMap<String, String>();
		formvars.put("deviceID", getDeviceId());

		HttpResponse response = postValues(formvars, baseApiUrl+"authenticate");	
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		String json = "";
		String s = "";
		while ((s = stdInput.readLine()) != null) {
			json += s;
		}
		System.out.println("JSON: "+json);
		json = json.replace("{", "");
		json = json.replace("}", "");
		String[] elements = json.split(",");
		HashMap<String, String> hm = new HashMap<String, String>();
		for (int i=0; i<elements.length; i++){
			String[] keyValuePair = elements[i].split(":");
			//System.out.println(elements.length);
			hm.put(keyValuePair[0].replace("\"", ""), keyValuePair[1].replace("\"", ""));

		}
		token = hm.get("token");
		sequence = Integer.valueOf(hm.get("sequence"));
		//System.out.println("Token"+hm.get("token"));
		//System.out.print(sequence);
	}

	public static String getValidation() throws IOException, NoSuchAlgorithmException {
		sequence++;
		String validation = token+sequence+apiKey;    
		String md5 =  getMD5Sum(validation);
		return md5;
	}

	public static String getStudies() throws IOException, NoSuchAlgorithmException{
		HashMap<String, String> formvars = new HashMap();
		formvars.put("deviceID", getDeviceId());
		formvars.put("validation", getValidation());
		HttpContext localContext = new BasicHttpContext();
		HttpResponse response = postValues(formvars, baseApiUrl+"getStudies");	
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		String returnValue = "";
		String s = "";
		while ((s = stdInput.readLine()) != null) {
			returnValue += s;
		} 
		System.out.println("Return: "+returnValue);
		return returnValue;
	}

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		// TODO Auto-generated method stub
		authenticate();
		System.out.println(getStudies());

	}

}
