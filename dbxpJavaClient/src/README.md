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

Below is the Java implementation of authenticate http://studies.dbnp.org/api#authenticate
it returns a Map containing both the token and the sequence.

### getStudies ###
getStudies returns all the available studies. It requires the deviceID and the validation value from getValidation()
for details see: http://studies.dbnp.org/api#getStudies 
   
### getSubjectsForStudy ###
getSubjectsForStudy returns all the available studies. It requires the deviceID, the validation value from getValidation(), and one of 
the studytoken returned by getStudies
for details see: http://studies.dbnp.org/api#getSubjectsForStudy 

### getAssaysForStudy ###
   getAssaysForStudy returns all the available studies. It requires the deviceID, the validation value from getValidation(), and one of 
	 the studytoken returned by getStudies
	 for details see: http://studies.dbnp.org/api#getAssayForStudy 
### getSamplesForAssay ###
  getSamplesForAssay returns all the available studies. It requires the deviceID, the validation value from getValidation(), and one of 
	 the assaytoken returned by getAssaysForStudy
	 for details see: http://studies.dbnp.org/api#getSamplesForAssay
	 
### getMeasurementDataForAssay ###
  getMeasurementDataForAssay returns all the available studies. It requires the deviceID, the validation value from getValidation(), and one of the assaytoken returned by getAssaysForStudy
	 for details see: http://studies.dbnp.org/api#getMeasurementDataForAssay
	 