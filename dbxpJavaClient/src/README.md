gscf-JAVA-client
===============

JAVA client to interface with the GSCF api

  This is java client to connect to http://studies.dbnp.org or http://old.studies.dbnp.org
	 it contains calls for the following api calls
	 * authenticate - set up / synchronize client-server session
	 * getStudies - fetch all (readable) studies
   * getSubjectsForStudy - fetch all subjects in a given study
   * getAssaysForStudy - fetch all assays in a given study
   * getSamplesForAssay - fetch all samples in a given assay
   * getMeasurementDataForAssay - fetch all measurement data for a given assay
	For a detailed description of all api calls please consult: http://studies.dbnp.org/api

### Authenticate ###

```Java
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
```
### getStudies ###
### getSubjectsForStudy ###
### getAssaysForStudy ###
### getSamplesForAssay ###
### getMeasurementDataForAssay ###
