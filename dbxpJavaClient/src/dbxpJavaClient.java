import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


/*
The MIT License (MIT)
Copyright (c) 2012 andra.waagmeester@maastrichtuniversity.nl

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


public class dbxpJavaClient {

		

	//User specific credentials old studies 
	/*private static String userName = "";
	private static String password = "";
	private static String host = "old.studies.dbnp.org/";
	private static String baseApiUrl = "http://old.studies.dbnp.org/api/";
	private static String apiKey = "";
   */
	
	//User specific credentials  studies 
	private static String userName = "";
	private static String password = "";
	private static String baseApiUrl = "http://studies.dbnp.org/api/";
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

	public static Map<String, String> authenticate() throws IOException, NoSuchAlgorithmException{
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
		
		Gson gson = new Gson();
		Map<String, String> loginData = gson.fromJson(json, new TypeToken<Map<String, String>>(){}.getType());
		
		token = loginData.get("token");
		sequence = Integer.valueOf(loginData.get("sequence"));	
		return loginData;
	}

	public static String getValidation() throws IOException, NoSuchAlgorithmException {
		sequence++;
		String validation = token+sequence+apiKey;    
		String md5 =  getMD5Sum(validation);
		return md5;
	}

	public static Map<String, Object> getStudies() throws IOException, NoSuchAlgorithmException{
		HashMap<String, String> formvars = new HashMap<String, String>();
		formvars.put("deviceID", getDeviceId());
		formvars.put("validation", getValidation());
		HttpResponse response = postValues(formvars, baseApiUrl+"getStudies");	
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		String json = "";
		String s = "";
		while ((s = stdInput.readLine()) != null) {
			json += s;
		} 
		Gson gson = new Gson();
		Map<String, Object> studiesData = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());		
		return studiesData;
	}

	public static Map<String, Object> getSubjectsForStudy(String studyToken) throws NoSuchAlgorithmException, IOException{
		HashMap<String, String> formvars = new HashMap<String, String>();
		formvars.put("deviceID", getDeviceId());
		formvars.put("validation", getValidation());
		formvars.put("studyToken", studyToken);
		HttpResponse response = postValues(formvars, baseApiUrl+"getSubjectsForStudy");	
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		String json = "";
		String s = "";
		while ((s = stdInput.readLine()) != null) {
			json += s;
		} 
		Gson gson = new Gson();
		Map<String, Object> subjectsData = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
		return subjectsData;
	}

	public static Map<String, Object> getAssaysForStudy(String studyToken) throws NoSuchAlgorithmException, IOException{
		HashMap<String, String> formvars = new HashMap<String, String>();
		formvars.put("deviceID", getDeviceId());
		formvars.put("validation", getValidation());
		formvars.put("studyToken", studyToken);
		HttpResponse response = postValues(formvars, baseApiUrl+"getAssaysForStudy");	
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		String json = "";
		String s = "";
		while ((s = stdInput.readLine()) != null) {
			json += s;
		} 
		Gson gson = new Gson();
		Map<String, Object> subjectsData = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
		return subjectsData;
	}

	public static Map<String, Object> getSamplesForAssay(String assayToken) throws NoSuchAlgorithmException, IOException{
		HashMap<String, String> formvars = new HashMap<String, String>();
		formvars.put("deviceID", getDeviceId());
		formvars.put("validation", getValidation());
		formvars.put("assayToken", assayToken);
		HttpResponse response = postValues(formvars, baseApiUrl+"getAssaysForStudy");	
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		String json = "";
		String s = "";
		while ((s = stdInput.readLine()) != null) {
			json += s;
		} 
		Gson gson = new Gson();
		Map<String, Object> subjectsData = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
		return subjectsData;
	}

	public static Map<String, Object> getMeasurementDataForAssay(String assayToken) throws NoSuchAlgorithmException, IOException{
		HashMap<String, String> formvars = new HashMap<String, String>();
		formvars.put("deviceID", getDeviceId());
		formvars.put("validation", getValidation());
		formvars.put("assayToken", assayToken);
		HttpResponse response = postValues(formvars, baseApiUrl+"getMeasurementDataForAssay");	
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		String json = "";
		String s = "";
		while ((s = stdInput.readLine()) != null) {
			json += s;
		} 
		Gson gson = new Gson();
		Map<String, Object> subjectsData = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
		return subjectsData;
	}



	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		Map<String, String> loginData = authenticate();
		System.out.println(loginData.get("token"));
		authenticate();
		Map<String, Object> studiesInfo = getStudies();
		System.out.println(studiesInfo);
		List studiesArray = (List) studiesInfo.get("studies");
		Map<String, Object> studies = (Map<String, Object>) studiesArray.get(0);
		String studyToken = (String) studies.get("token");
		System.out.println(getSubjectsForStudy(studyToken));
		
		Map<String, Object> assayInfo = getAssaysForStudy(studyToken);
		List assayArray = (List) assayInfo.get("subjects");
		/*Map<String, Object> assays = (Map<String, Object>) assayArray.get(0);
		String assayToken = (String) assays.get("token");
		System.out.println("AssayToken: "+assayToken);
		System.out.println(getMeasurementDataForAssay(assayToken));
		*/
	}


	
}


