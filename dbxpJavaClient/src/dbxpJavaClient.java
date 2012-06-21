import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.UUID;

/*
The MIT License (MIT)
Copyright (c) 2012 andra.waagmeester@maastrichtuniversity.nl

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


public class dbxpJavaClient {

	//User specific credentials 
	private static String userName = "";
	private static String password = "";
	private static String baseApiUrl = "http://studies.dbnp.org/api/";
	
	//Local variables
	private static String authenticate=null;
    private static String token="";
    private static int sequence=0;
    
    
	/**
	 * @param args
	 */
	
    public static String getDeviceId() {
    	return UUID.randomUUID().toString();
    }
    
    public static String getMD5Sum(String variable) throws NoSuchAlgorithmException{	
       	MessageDigest md=MessageDigest.getInstance("MD5");
       	md.update(variable.getBytes());
        return new BigInteger(1,md.digest()).toString(16);
    }
    
    public static String getMdDeviceId() throws NoSuchAlgorithmException{
    	return getMD5Sum(getDeviceId());
    } 
    
    public static void authenticate() throws IOException{
    	Process p = Runtime.getRuntime().exec("curl -u "+userName+":"+password+" -d \"deviceId="+getDeviceId()+"\" "+baseApiUrl+"authenticate");
    	BufferedReader stdInput = new BufferedReader(new 
                InputStreamReader(p.getInputStream()));
    	String json = "";
    	String s = "";
    	while ((s = stdInput.readLine()) != null) {
            json += s;
        }
    	json = json.replace("{", "");
    	json = json.replace("}", "");
    	String[] elements = json.split(",");
    	HashMap<String, String> hm = new HashMap();
    	for (int i=0; i<elements.length; i++){
    		String[] keyValuePair = elements[i].split(":");
    		for (int j=0; j<keyValuePair.length; j++){
    			hm.put(keyValuePair[0].replace("\"", ""), keyValuePair[1].replace("\"", ""));
    		}
    	}
    	token = hm.get("token");
    	sequence = Integer.valueOf(hm.get("sequence"));
    }
    
    public static String getValidation() throws IOException, NoSuchAlgorithmException {
    	sequence++;
    	String validation = token+sequence+"e5484d2d-6446-4b5e-abf2-f8748b1c5ff1";    
    	
        String md5 =  getMD5Sum(validation);
        return md5;
    }
    
    public static String getStudies() throws IOException, NoSuchAlgorithmException{
    	String formVars = "deviceID="+
    					getMdDeviceId() +
    					"&validation="+
    					getValidation();
    	System.out.println("curl -d \""+formVars+"\" " +baseApiUrl+"getStudies");
    	Process p = Runtime.getRuntime().exec("curl -d \""+formVars+"\" "+baseApiUrl+"getStudies");
    	BufferedReader stdInput = new BufferedReader(new 
                InputStreamReader(p.getInputStream()));
    	String returnValue = "";
    	String s = "";
    	while ((s = stdInput.readLine()) != null) {
            returnValue += s;
        }
    	return returnValue;
    }
    
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		// TODO Auto-generated method stub
        authenticate();
        System.out.println(token);
        System.out.println(getValidation());
        System.out.println(getStudies());

	}

}
