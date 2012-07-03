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




public class dbxpJavaClient {

	/* This is java client to connect to http://studies.dbnp.org or http://old.studies.dbnp.org
	 * it contains calls for the following api calls
	 * authenticate;
	 * authenticate - set up / synchronize client-server session
	 * getStudies - fetch all (readable) studies
     * getSubjectsForStudy - fetch all subjects in a given study
     * getAssaysForStudy - fetch all assays in a given study
     * getSamplesForAssay - fetch all samples in a given assay
     * getMeasurementDataForAssay - fetch all measurement data for a given assay
	 * For a detailed description of all api calls please consult: http://studies.dbnp.org/api
	 */

	/* The block below contains the login credentials for studies and oldstudies. These credentials can be obtained 
	 * by going to studies.dbnp.org and old.studied.dbnp.org. 
	 * In the right corner you can Login or Register
	 * Once logged in you can obtain the api Key the profile menu.
	 * 
	 * Switching between studies and old.studies can be done by uncommenting one and uncommenting the other.
	 */
	//User specific credentials old studies 
	/*private static String userName = "";
	private static String password = "";
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

	/* getMacAddress retrieves the unique MAC address of the client computer. 
	 * This is needed to create a unique device ID. 
	 * The deviceID is created in getDeviceID.
	 * 
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

	/* getMD5Sum returns the MD5 sum of the variable parameteren */
	
	public static String getMD5Sum(String variable) throws NoSuchAlgorithmException{	
		MessageDigest md=MessageDigest.getInstance("MD5");
		md.update(variable.getBytes());
		return new BigInteger(1,md.digest()).toString(16);
	}

   /* Post values submit the POST variables to url and returns the respons *?    */
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
	
    /* Below is the Java implementation of authenticate http://studies.dbnp.org/api#authenticate
     * it returns a Map containing both the token and the sequence.
     */
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
    /* Except for authentication, do all other calls require a validation parameter. This is the MD5 sum of the token, 
     *  the sequence and the api key. The token is returned by authenticate. Each time an api call is sent, the sequence needs to be increased by 
     * one. The api key can be retrieved in the use profile on the website (see above) 
     */
	public static String getValidation() throws IOException, NoSuchAlgorithmException {
		sequence++;
		String validation = token+sequence+apiKey;    
		String md5 =  getMD5Sum(validation);
		return md5;
	}

	/* getStudies returns all the available studies. It requires the deviceID and the validation value from getValidation()
	 * for details see: http://studies.dbnp.org/api#getStudies 
	 */
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

	/* getSubjectsForStudy returns all the available studies. It requires the deviceID, the validation value from getValidation(), and one of 
	 * the studytoken returned by getStudies
	 * for details see: http://studies.dbnp.org/api#getSubjectsForStudy 
	 */
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

	/* getAssaysForStudy returns all the available studies. It requires the deviceID, the validation value from getValidation(), and one of 
	 * the studytoken returned by getStudies
	 * for details see: http://studies.dbnp.org/api#getAssayForStudy 
	 */
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

	/* getSamplesForAssay returns all the available studies. It requires the deviceID, the validation value from getValidation(), and one of 
	 * the assaytoken returned by getAssaysForStudy
	 * for details see: http://studies.dbnp.org/api#getSamplesForAssay
	 */
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

	/* getMeasurementDataForAssay returns all the available studies. It requires the deviceID, the validation value from getValidation(), and one of 
	 * the assaytoken returned by getAssaysForStudy
	 * for details see: http://studies.dbnp.org/api#getMeasurementDataForAssay
	 */
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


