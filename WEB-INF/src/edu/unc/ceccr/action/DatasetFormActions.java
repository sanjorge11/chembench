package edu.unc.ceccr.action;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

//struts2
import com.opensymphony.xwork2.ActionSupport; 
import com.opensymphony.xwork2.ActionContext; 

import org.apache.struts.upload.FormFile;
import org.apache.struts2.interceptor.SessionAware;
import org.hibernate.Session;

import edu.unc.ceccr.global.Constants;
import edu.unc.ceccr.global.ErrorMessages;
import edu.unc.ceccr.jobs.CentralDogma;
import edu.unc.ceccr.persistence.DataSet;
import edu.unc.ceccr.persistence.HibernateUtil;
import edu.unc.ceccr.persistence.Predictor;
import edu.unc.ceccr.persistence.User;
import edu.unc.ceccr.taskObjects.CreateDatasetTask;
import edu.unc.ceccr.taskObjects.QsarModelingTask;
import edu.unc.ceccr.utilities.FileAndDirOperations;
import edu.unc.ceccr.utilities.PopulateDataObjects;
import edu.unc.ceccr.utilities.Utility;
import edu.unc.ceccr.workflows.datasets.DatasetFileOperations;

public class DatasetFormActions extends ActionSupport{
	public String ajaxLoadModeling() throws Exception {
		return SUCCESS;
	}
	public String ajaxLoadPrediction() throws Exception {
		return SUCCESS;
	}
	public String ajaxLoadModelingWithDescriptors() throws Exception {
		ActionContext context = ActionContext.getContext();
		Session session = HibernateUtil.getSession();
		user = (User) context.getSession().get("user");
		userUploadedDescriptorTypes = PopulateDataObjects.populateDatasetUploadedDescriptorTypes(user.getUserName(), true, session);
		return SUCCESS;
	}
	public String ajaxLoadPredictionWithDescriptors() throws Exception {
		return SUCCESS;
	}
	public String ajaxLoadAutoSplit() throws Exception {
		return SUCCESS;
	}
	public String ajaxLoadManualSplit() throws Exception {
		return SUCCESS;
	}
	public String ajaxLoadNFoldSplit() throws Exception {
		return SUCCESS;
	}

	public String loadPage() throws Exception {

		String result = SUCCESS;
		
		//check that the user is logged in
		ActionContext context = ActionContext.getContext();
		
		if(context == null){
			Utility.writeToStrutsDebug("No ActionContext available");
		}
		else{
			user = (User) context.getSession().get("user");
			
			if(user == null){
				Utility.writeToStrutsDebug("No user is logged in.");
				result = LOGIN;
				return result;
			}
		}
		
		//set up any values that need to be populated onto the page (dropdowns, lists, display stuff)

		Session session = HibernateUtil.getSession();
		
		userDatasetNames = PopulateDataObjects.populateDatasetNames(user.getUserName(), true, session);
		userPredictorNames = PopulateDataObjects.populatePredictorNames(user.getUserName(), true, session);
		userPredictionNames = PopulateDataObjects.populatePredictionNames(user.getUserName(), true, session);
		userTaskNames = PopulateDataObjects.populateTaskNames(user.getUserName(), false, session);
		userPredictorList = PopulateDataObjects.populatePredictors(user.getUserName(), true, true, session);

		session.close();
		//log the results
		if(result.equals(SUCCESS)){
			Utility.writeToStrutsDebug("Forwarding user " + user.getUserName() + " to dataset page.");
		}
		else{
			Utility.writeToStrutsDebug("Cannot load page.");
		}
		
		dataTypeModeling = Constants.CONTINUOUS;
		
		//go to the page
		return result;
	}
	
	public String execute() throws Exception {
		
		String emailOnCompletion = "false"; //for now
		
		String result = INPUT;

		ActionContext context = ActionContext.getContext();
		user = (User) context.getSession().get("user");
		String userName = user.getUserName();
		if(datasetName != null){
			datasetName = datasetName.replaceAll("\\s+", "_");
			datasetName = datasetName.replaceAll("\\(", "_");
			datasetName = datasetName.replaceAll("\\)", "_");
			datasetName = datasetName.replaceAll("\\[", "_");
			datasetName = datasetName.replaceAll("\\]", "_");
		}
		
		try{
			Utility.writeToDebug("Starting dataset task");
			Utility.writeToDebug("datasetName: " + datasetName);
		}
		catch(Exception ex){
			Utility.writeToDebug(ex);
		}

		Utility.writeToUsageLog("Uploaded dataset " + datasetName, userName);
		
		ArrayList<String> msgs = new ArrayList<String>();
		Utility.writeToDebug("type: " + datasetType);
		
		if(externalCompoundsCountOrPercent.equalsIgnoreCase("percent")){
			double tmp = Double.parseDouble(numExternalCompounds);
			tmp /= 100;
			numExternalCompounds = "" + tmp;
		}
		if(splitType.equals(Constants.NFOLD)){
			useActivityBinning = useActivityBinningNFold;
		}
		
		if(datasetType.equalsIgnoreCase(Constants.MODELING)){
			
			if(sdfFileModeling == null){
				Utility.writeToDebug("sdf file is null");
			}
			else{
				Utility.writeToDebug("sdf file is good");
			}
			if(sdfFileModelingContentType == ""){
				Utility.writeToDebug("sdf file is empty");
			}
			else{
				Utility.writeToDebug("sdf file is " + sdfFileModelingContentType);
			}
			if(sdfFileModelingFileName == ""){
				Utility.writeToDebug("sdf file is empty");
			}
			else{
				Utility.writeToDebug("sdf file is " + sdfFileModelingFileName);
			}
			
			//do file check
			if(sdfFileModeling == null && actFileModeling == null){
				errorStrings.add("File upload failed or no files supplied. If you are using Chrome, try again in a different browser such as Firefox.");
				result = ERROR;
			}
			else if(sdfFileModeling == null){
				errorStrings.add("Missing SDF or file upload error.");
				result = ERROR;
			}
			else if(actFileModeling == null){
				errorStrings.add("Missing Activity file or file upload error. If you do not have an Activity file for this dataset, use the Prediction Set option when uploading.");
				result = ERROR;
			}
			
			if(result.equalsIgnoreCase(INPUT)){
				//verify uploaded files and copy them to the dataset dir
				if(actFileModelingFileName.endsWith(".a")){
					actFileModelingFileName = actFileModelingFileName.substring(0, actFileModelingFileName.lastIndexOf(".")) + ".act";
				}
				else if(! actFileModelingFileName.endsWith(".act")){
					actFileModelingFileName += ".act";
				}
				try{
					msgs = DatasetFileOperations.uploadDataset(userName, sdfFileModeling, sdfFileModelingFileName, 
							actFileModeling, actFileModelingFileName, null, "", datasetName, 
							dataTypeModeling, datasetType, externalCompoundList);
					sdfFileModelingFileName = sdfFileModelingFileName.replaceAll(" ", "_");
					actFileModelingFileName = actFileModelingFileName.replaceAll(" ", "_");
				}
				catch(Exception ex){
					Utility.writeToDebug(ex);
					result = ERROR;
					msgs.add("An exception occurred while uploading this dataset: " + ex.getMessage());
				}
				if(!msgs.isEmpty()){
					errorStrings.addAll(msgs);
					result = ERROR;
				}
			}
			
			if(result.equalsIgnoreCase(INPUT)){
				CreateDatasetTask datasetTask = new CreateDatasetTask(userName, 
						datasetType, //MODELING, PREDICTION, MODELINGWITHDESCRIPTORS, or PREDICTIONWITHDESCRIPTORS
						sdfFileModelingFileName, //sdfFileName
						actFileModelingFileName, //actFileName
						"", //xFileName
						"", //descriptor type, if datasetType is MODELINGWITHDESCRIPTORS or PREDICTIONWITHDESCRIPTORS
						dataTypeModeling, //act file type, Continuous or Category, if datasetType is MODELING or MODELINGWITHDESCRIPTORS. Prediction otherwise.
						standardizeModeling, //used in MODELING and PREDICTION
						splitType, //RANDOM or USERDEFINED
						"", //scaling type
						numExternalCompounds, //if splitType is RANDOM
						numExternalFolds, //if splitType is NFOLD
						useActivityBinning, //if splitType is RANDOM
						externalCompoundList, //if splitType is USERDEFINED
						datasetName,
						paperReference,
						dataSetDescription);
				try{
					Utility.writeToDebug("getting ACT compound count from " + Constants.CECCR_USER_BASE_PATH + userName + "/DATASETS/" + datasetName + "/" + actFileModelingFileName);
					int numCompounds = DatasetFileOperations.getACTCompoundNames(
							Constants.CECCR_USER_BASE_PATH + userName + "/DATASETS/" + datasetName + "/" + actFileModelingFileName).size();
					int numModels = 0;

					CentralDogma centralDogma = CentralDogma.getInstance();
					centralDogma.addJobToIncomingList(userName, datasetName, datasetTask, numCompounds, numModels, emailOnCompletion);
					
				}
				catch(Exception ex){
					Utility.writeToDebug(ex);
				}
			}
		}
		else if(datasetType.equalsIgnoreCase(Constants.PREDICTION)){
			Utility.writeToDebug("got into function");
			//do file check
			if(sdfFilePrediction == null){
				errorStrings.add("File upload failed or no files supplied. If you are using Chrome, try again in a different browser such as Firefox.");
				result = ERROR;
			}
			
			if(result.equalsIgnoreCase(INPUT)){
				//verify uploaded files and copy them to the dataset dir
				try{
					msgs = DatasetFileOperations.uploadDataset(userName, sdfFilePrediction, sdfFilePredictionFileName, null, 
							"", null, "", datasetName, dataTypeModeling, datasetType, externalCompoundList);
					sdfFilePredictionFileName = sdfFilePredictionFileName.replaceAll(" ", "_");
				}
				catch(Exception ex){
					Utility.writeToDebug(ex);
					result = ERROR;
					msgs.add("An exception occurred while uploading this dataset: " + ex.getMessage());
				}
			
				if(!msgs.isEmpty()){
					errorStrings.addAll(msgs);
					result = ERROR;
				}
			}
			if(result.equalsIgnoreCase(INPUT)){
				try{
					Utility.writeToDebug("creating task");
					CreateDatasetTask datasetTask = new CreateDatasetTask(userName, 
							datasetType, //MODELING, PREDICTION, MODELINGWITHDESCRIPTORS, or PREDICTIONWITHDESCRIPTORS
							sdfFilePredictionFileName, //sdfFileName
							"", //actFileName
							"", //xFileName
							"", //descriptor type, if datasetType is MODELINGWITHDESCRIPTORS or PREDICTIONWITHDESCRIPTORS
							Constants.PREDICTION, //act file type, Continuous or Category, if datasetType is MODELING or MODELINGWITHDESCRIPTORS. Prediction otherwise.
							standardizePrediction, //used in MODELING and PREDICTION
							splitType, //RANDOM or USERDEFINED
							"", //scaling type
							numExternalCompounds, //if splitType is RANDOM
							numExternalFolds, //if splitType is NFOLD
							useActivityBinning, //if splitType is RANDOM
							externalCompoundList, //if splitType is USERDEFINED
							datasetName,
							paperReference,
							dataSetDescription);
					
					int numCompounds = DatasetFileOperations.getSDFCompoundNames(
							Constants.CECCR_USER_BASE_PATH + userName + "/DATASETS/" + datasetName + "/" + sdfFilePredictionFileName).size();
					int numModels = 0;
					Utility.writeToDebug("adding task");
					
					CentralDogma centralDogma = CentralDogma.getInstance();
					centralDogma.addJobToIncomingList(userName, datasetName, datasetTask, numCompounds, numModels, emailOnCompletion);
					
				}
				catch(Exception ex){
					Utility.writeToDebug(ex);
					result = ERROR;
					msgs.add("An exception occurred while creating this dataset: " + ex.getMessage());
				}
			}
		}
		else if(datasetType.equalsIgnoreCase(Constants.MODELINGWITHDESCRIPTORS)){
			
			if(xFileModDesc == null || actFileModDesc == null){
				errorStrings.add("File upload failed or no files supplied. If you are using Chrome, try again in a different browser such as Firefox.");
				result = ERROR;
			}
			
			if(result.equalsIgnoreCase(INPUT)){
				//verify uploaded files and copy them to the dataset dir
				try{
					actFileModDescFileName = actFileModDescFileName.replaceAll(" ", "_");
					if(actFileModDescFileName.endsWith(".a")){
						actFileModDescFileName = actFileModDescFileName.substring(0, actFileModDescFileName.lastIndexOf(".")) + ".act";
					}
					else if(! actFileModDescFileName.endsWith(".act")){
						actFileModDescFileName += ".act";
					}
					if(! xFileModDescFileName.endsWith(".x")){
						xFileModDescFileName += ".x";
					}
					
					msgs = DatasetFileOperations.uploadDataset(userName, sdfFileModDesc, sdfFileModDescFileName, actFileModDesc, 
							actFileModDescFileName, xFileModDesc, xFileModDescFileName, datasetName, 
							dataTypeModeling, datasetType, externalCompoundList);
					sdfFileModDescFileName = sdfFileModDescFileName.replaceAll(" ", "_");
					actFileModDescFileName = actFileModDescFileName.replaceAll(" ", "_");
					xFileModDescFileName = xFileModDescFileName.replaceAll(" ", "_");
					descriptorTypeModDesc = descriptorNewName.trim().isEmpty()?selectedDescriptorUsedName:descriptorNewName; 
				}
				catch(Exception ex){
					Utility.writeToDebug(ex);
					result = ERROR;
					msgs.add("An exception occurred while uploading this dataset: " + ex.getMessage());
				}
				
				if(!msgs.isEmpty()){
					errorStrings.addAll(msgs);
					result = ERROR;
				}
			}
			if(result.equalsIgnoreCase(INPUT)){
				try{
					CreateDatasetTask datasetTask = new CreateDatasetTask(userName, 
						datasetType, //MODELING, PREDICTION, MODELINGWITHDESCRIPTORS, or PREDICTIONWITHDESCRIPTORS
						sdfFileModDescFileName, //sdfFileName
						actFileModDescFileName, //actFileName
						xFileModDescFileName, //xFileName
						descriptorTypeModDesc, //descriptor type, if datasetType is MODELINGWITHDESCRIPTORS or PREDICTIONWITHDESCRIPTORS
						dataTypeModDesc, //act file type, Continuous or Category, if datasetType is MODELING or MODELINGWITHDESCRIPTORS. Prediction otherwise.
						standardizeModDesc,
						splitType, //RANDOM or USERDEFINED
						hasBeenScaled, //true or false
						numExternalCompounds, //if splitType is RANDOM
						numExternalFolds, //if splitType is NFOLD
						useActivityBinning, //if splitType is RANDOM
						externalCompoundList, //if splitType is USERDEFINED
						datasetName,
						paperReference,
						dataSetDescription);

					int numCompounds = DatasetFileOperations.getACTCompoundNames(
							Constants.CECCR_USER_BASE_PATH + userName + "/DATASETS/" + datasetName + "/" + actFileModDescFileName).size();
					int numModels = 0;
				
					CentralDogma centralDogma = CentralDogma.getInstance();
					centralDogma.addJobToIncomingList(userName, datasetName, datasetTask, numCompounds, numModels, emailOnCompletion);
				}
				catch(Exception ex){
					Utility.writeToDebug(ex);
					result = ERROR;
					msgs.add("An exception occurred while creating this dataset: " + ex.getMessage());
				}
			}
		}
		else if(datasetType.equalsIgnoreCase(Constants.PREDICTIONWITHDESCRIPTORS)){
			if(xFilePredDesc == null){
				errorStrings.add("File upload failed or no files supplied. If you are using Chrome, try again in a different browser such as Firefox.");
				result = ERROR;
			}
			
			if(result.equalsIgnoreCase(INPUT)){
				//verify uploaded files and copy them to the dataset dir
				try{
					if(! xFilePredDescFileName.endsWith(".x")){
						xFilePredDescFileName += ".x";
					}
					
					msgs = DatasetFileOperations.uploadDataset(userName, sdfFilePredDesc, sdfFilePredDescFileName, null, "", 
							xFilePredDesc, xFilePredDescFileName, datasetName, dataTypeModeling, datasetType, externalCompoundList);
					sdfFilePredDescFileName = sdfFilePredDescFileName.replaceAll(" ", "_");
					xFilePredDescFileName = xFilePredDescFileName.replaceAll(" ", "_");
				}
				catch(Exception ex){
					Utility.writeToDebug(ex);
					result = ERROR;
					msgs.add("An exception occurred while uploading this dataset: " + ex.getMessage());
				}
				
				if(!msgs.isEmpty()){
					errorStrings.addAll(msgs);
					result = ERROR;
				}
			}
			
			if(result.equalsIgnoreCase(INPUT)){
				try{
					CreateDatasetTask datasetTask = new CreateDatasetTask(userName, 
						datasetType, //MODELING, PREDICTION, MODELINGWITHDESCRIPTORS, or PREDICTIONWITHDESCRIPTORS
						sdfFilePredDescFileName, //sdfFileName
						"", //actFileName
						xFilePredDescFileName, //xFileName
						descriptorTypePredDesc, //descriptor type, if datasetType is MODELINGWITHDESCRIPTORS or PREDICTIONWITHDESCRIPTORS
						Constants.PREDICTION, //act file type, Continuous or Category, if datasetType is MODELING or MODELINGWITHDESCRIPTORS. Prediction otherwise.
						standardizePredDesc,
						splitType, //RANDOM or USERDEFINED
						"", //hasBeenScaled
						numExternalCompounds, //if splitType is RANDOM
						numExternalFolds, //if splitType is NFOLD
						useActivityBinning, //if splitType is RANDOM
						externalCompoundList, //if splitType is USERDEFINED
						datasetName,
						paperReference,
						dataSetDescription);
				
					int numCompounds = DatasetFileOperations.getXCompoundNames(
							Constants.CECCR_USER_BASE_PATH + userName + "/DATASETS/" + datasetName + "/" + xFilePredDescFileName).size();
					int numModels = 0;
				
					CentralDogma centralDogma = CentralDogma.getInstance();
					centralDogma.addJobToIncomingList(userName, datasetName, datasetTask, numCompounds, numModels, emailOnCompletion);
					
					//Queue.getInstance().addJob(datasetTask, userName, datasetName, numCompounds, numModels);
				}
				catch(Exception ex){
					Utility.writeToDebug(ex);
					result = ERROR;
					msgs.add("An exception occurred while creating this dataset: " + ex.getMessage());
				}
			}
		}
		
		return result;
	}
	
	private ArrayList<String> errorStrings = new ArrayList<String>();
	private String datasetName = "";
	private String datasetType = Constants.MODELING;
	private String splitType = Constants.RANDOM;
	private String dataTypeModeling = Constants.CONTINUOUS;
	private String dataTypeModDesc = Constants.CONTINUOUS;
	private String dataSetDescription = "";
	private String externalCompoundList = "";
	private String useActivityBinning = "true";
	private String numExternalCompounds = "20";
	private String externalCompoundsCountOrPercent = "percent";
	private String standardizeModeling = "true";
	private String standardizeModDesc = "true";
	private String standardizePredDesc = "true";
	private String standardizePrediction = "true";
	private String paperReference = "";
	private String descriptorTypeModDesc = "";
	private String descriptorTypePredDesc = "";
	private String hasBeenScaled = "false";
	private String useActivityBinningNFold = "true";
	private String numExternalFolds = "5";

	public ArrayList<String> getErrorStrings() {
		return errorStrings;
	}
	public void setErrorStrings(ArrayList<String> errorStrings) {
		this.errorStrings = errorStrings;
	}
	
	public String getDatasetName() {
		return datasetName;
	}
	public void setDatasetName(String datasetName) {
		this.datasetName = datasetName;
	}
	public String getDatasetType() {
		return datasetType;
	}
	public void setDatasetType(String datasetType) {
		this.datasetType = datasetType;
	}
	public String getSplitType() {
		return splitType;
	}
	public void setSplitType(String splitType) {
		this.splitType = splitType;
	}
	public String getDataTypeModeling() {
		return dataTypeModeling;
	}
	public void setDataTypeModeling(String dataTypeModeling) {
		this.dataTypeModeling = dataTypeModeling;
	}
	public String getDataTypeModDesc() {
		return dataTypeModDesc;
	}
	public void setDataTypeModDesc(String dataTypeModDesc) {
		this.dataTypeModDesc = dataTypeModDesc;
	}
	public String getDataSetDescription() {
		return dataSetDescription;
	}
	public void setDataSetDescription(String dataSetDescription) {
		this.dataSetDescription = dataSetDescription;
	}
	public String getExternalCompoundList() {
		return externalCompoundList;
	}
	public void setExternalCompoundList(String externalCompoundList) {
		this.externalCompoundList = externalCompoundList;
	}
	public String getUseActivityBinning() {
		return useActivityBinning;
	}
	public void setUseActivityBinning(String useActivityBinning) {
		this.useActivityBinning = useActivityBinning;
	}
	public String getNumExternalCompounds() {
		return numExternalCompounds;
	}
	public void setNumExternalCompounds(String numExternalCompounds) {
		this.numExternalCompounds = numExternalCompounds;
	}
	public String getStandardizeModeling() {
		return standardizeModeling;
	}
	public void setStandardizeModeling(String standardizeModeling) {
		this.standardizeModeling = standardizeModeling;
	}
	public String getStandardizeModDesc() {
		return standardizeModDesc;
	}
	public void setStandardizeModDesc(String standardizeModDesc) {
		this.standardizeModDesc = standardizeModDesc;
	}
	public String getStandardizePredDesc() {
		return standardizePredDesc;
	}
	public void setStandardizePredDesc(String standardizePredDesc) {
		this.standardizePredDesc = standardizePredDesc;
	}
	public String getStandardizePrediction() {
		return standardizePrediction;
	}
	public void setStandardizePrediction(String standardizePrediction) {
		this.standardizePrediction = standardizePrediction;
	}
	public String getPaperReference() {
		return paperReference;
	}
	public void setPaperReference(String paperReference) {
		this.paperReference = paperReference;
	}

	public String getDescriptorTypeModDesc() {
		return descriptorTypeModDesc;
	}
	public void setDescriptorTypeModDesc(String descriptorTypeModDesc) {
		this.descriptorTypeModDesc = descriptorTypeModDesc;
	}
	
	public String getDescriptorTypePredDesc() {
		return descriptorTypePredDesc;
	}
	public void setDescriptorTypePredDesc(String descriptorTypePredDesc) {
		this.descriptorTypePredDesc = descriptorTypePredDesc;
	}
	
	//file upload stuff
	//modeling
	private File sdfFileModeling = null;
	private String sdfFileModelingContentType = "";
	private String sdfFileModelingFileName = ""; 

	private File actFileModeling = null;
	private String actFileModelingContentType = "";
	private String actFileModelingFileName = "";
	
	//prediction
	private File sdfFilePrediction = null;
	private String sdfFilePredictionContentType = "";
	private String sdfFilePredictionFileName = ""; 

	//modeling with descriptors
	private File actFileModDesc = null;
	private String actFileModDescContentType = "";
	private String actFileModDescFileName = "";
	
	private File xFileModDesc = null;
	private String xFileModDescContentType= "";
	private String xFileModDescFileName = "";

	private File sdfFileModDesc = null;
	private String sdfFileModDescContentType = "";
	private String sdfFileModDescFileName = ""; 

	//prediction with descriptors
	private File xFilePredDesc = null;
	private String xFilePredDescContentType= "";
	private String xFilePredDescFileName = "";

	private File sdfFilePredDesc = null;
	private String sdfFilePredDescContentType = "";
	private String sdfFilePredDescFileName = ""; 
	
	
	public File getSdfFileModeling() {
		return sdfFileModeling;
	}
	public void setSdfFileModeling(File sdfFileModeling) {
		this.sdfFileModeling = sdfFileModeling;
	}
	public String getSdfFileModelingContentType() {
		return sdfFileModelingContentType;
	}
	public void setSdfFileModelingContentType(String sdfFileModelingContentType) {
		this.sdfFileModelingContentType = sdfFileModelingContentType;
	}
	public String getSdfFileModelingFileName() {
		return sdfFileModelingFileName;
	}
	public void setSdfFileModelingFileName(String sdfFileModelingFileName) {
		this.sdfFileModelingFileName = sdfFileModelingFileName;
	}
	public File getSdfFileModDesc() {
		return sdfFileModDesc;
	}
	public void setSdfFileModDesc(File sdfFileModDesc) {
		this.sdfFileModDesc = sdfFileModDesc;
	}
	public String getSdfFileModDescContentType() {
		return sdfFileModDescContentType;
	}
	public void setSdfFileModDescContentType(String sdfFileModDescContentType) {
		this.sdfFileModDescContentType = sdfFileModDescContentType;
	}
	public String getSdfFileModDescFileName() {
		return sdfFileModDescFileName;
	}
	public void setSdfFileModDescFileName(String sdfFileModDescFileName) {
		this.sdfFileModDescFileName = sdfFileModDescFileName;
	}
	public File getSdfFilePrediction() {
		return sdfFilePrediction;
	}
	public void setSdfFilePrediction(File sdfFilePrediction) {
		this.sdfFilePrediction = sdfFilePrediction;
	}
	public String getSdfFilePredictionContentType() {
		return sdfFilePredictionContentType;
	}
	public void setSdfFilePredictionContentType(String sdfFilePredictionContentType) {
		this.sdfFilePredictionContentType = sdfFilePredictionContentType;
	}
	public String getSdfFilePredictionFileName() {
		return sdfFilePredictionFileName;
	}
	public void setSdfFilePredictionFileName(String sdfFilePredictionFileName) {
		this.sdfFilePredictionFileName = sdfFilePredictionFileName;
	}
	public File getActFileModeling() {
		return actFileModeling;
	}
	public void setActFileModeling(File actFileModeling) {
		this.actFileModeling = actFileModeling;
	}
	public String getActFileModelingContentType() {
		return actFileModelingContentType;
	}
	public void setActFileModelingContentType(String actFileModelingContentType) {
		this.actFileModelingContentType = actFileModelingContentType;
	}
	public String getActFileModelingFileName() {
		return actFileModelingFileName;
	}
	public void setActFileModelingFileName(String actFileModelingFileName) {
		this.actFileModelingFileName = actFileModelingFileName;
	}
	public File getActFileModDesc() {
		return actFileModDesc;
	}
	public void setActFileModDesc(File actFileModDesc) {
		this.actFileModDesc = actFileModDesc;
	}
	public String getActFileModDescContentType() {
		return actFileModDescContentType;
	}
	public void setActFileModDescContentType(String actFileModDescContentType) {
		this.actFileModDescContentType = actFileModDescContentType;
	}
	public String getActFileModDescFileName() {
		return actFileModDescFileName;
	}
	public void setActFileModDescFileName(String actFileModDescFileName) {
		this.actFileModDescFileName = actFileModDescFileName;
	}
	public File getXFileModDesc() {
		return xFileModDesc;
	}
	public void setXFileModDesc(File xFileModDesc) {
		this.xFileModDesc = xFileModDesc;
	}
	public String getXFileModDescContentType() {
		return xFileModDescContentType;
	}
	public void setXFileModDescContentType(String fileContentType) {
		xFileModDescContentType = fileContentType;
	}
	public String getXFileModDescFileName() {
		return xFileModDescFileName;
	}
	public void setXFileModDescFileName(String fileName) {
		xFileModDescFileName = fileName;
	}public File getXFilePredDesc() {
		return xFilePredDesc;
	}
	public void setXFilePredDesc(File filePredDesc) {
		xFilePredDesc = filePredDesc;
	}
	public String getXFilePredDescContentType() {
		return xFilePredDescContentType;
	}
	public void setXFilePredDescContentType(String filePredDescContentType) {
		xFilePredDescContentType = filePredDescContentType;
	}
	public String getXFilePredDescFileName() {
		return xFilePredDescFileName;
	}
	public void setXFilePredDescFileName(String filePredDescFileName) {
		xFilePredDescFileName = filePredDescFileName;
	}
	public File getSdfFilePredDesc() {
		return sdfFilePredDesc;
	}
	public void setSdfFilePredDesc(File sdfFilePredDesc) {
		this.sdfFilePredDesc = sdfFilePredDesc;
	}
	public String getSdfFilePredDescContentType() {
		return sdfFilePredDescContentType;
	}
	public void setSdfFilePredDescContentType(String sdfFilePredDescContentType) {
		this.sdfFilePredDescContentType = sdfFilePredDescContentType;
	}
	public String getSdfFilePredDescFileName() {
		return sdfFilePredDescFileName;
	}
	public void setSdfFilePredDescFileName(String sdfFilePredDescFileName) {
		this.sdfFilePredDescFileName = sdfFilePredDescFileName;
	}

	//end file upload stuff
	
	
	//====== variables used for display on the JSP =====//
	private User user;
	
	private List<String> userDatasetNames;
	private List<String> userPredictorNames;
	private List<String> userPredictionNames;
	private List<String> userTaskNames;
	private List<String> userUploadedDescriptorTypes;
	private List<Predictor> userPredictorList;
	private String selectedDescriptorUsedName="";
	private String descriptorNewName="";

	

	public User getUser(){
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	
	public List<String> getUserDatasetNames(){
		return userDatasetNames;
	}
	public void setUserDatasetNames(List<String> userDatasetNames) {
		this.userDatasetNames = userDatasetNames;
	}
	
	public List<String> getUserPredictorNames(){
		return userPredictorNames;
	}
	public void setUserPredictorNames(List<String> userPredictorNames) {
		this.userPredictorNames = userPredictorNames;
	}
	
	public List<String> getUserPredictionNames(){
		return userPredictionNames;
	}
	public void setUserPredictionNames(List<String> userPredictionNames) {
		this.userPredictionNames = userPredictionNames;
	}
	
	public List<String> getUserTaskNames(){
		return userTaskNames;
	}
	public void setUserTaskNames(List<String> userTaskNames) {
		this.userTaskNames = userTaskNames;
	}
	
	
	public List<Predictor> getUserPredictorList(){
		return userPredictorList;
	}
	public void setUserPredictorList(List<Predictor> userPredictorList) {
		this.userPredictorList = userPredictorList;
	}
	
	public String getExternalCompoundsCountOrPercent() {
		return externalCompoundsCountOrPercent;
	}
	public void setExternalCompoundsCountOrPercent(
			String externalCompoundsCountOrPercent) {
		this.externalCompoundsCountOrPercent = externalCompoundsCountOrPercent;
	}
	
	public String getUseActivityBinningNFold() {
		return useActivityBinningNFold;
	}
	public void setUseActivityBinningNFold(String useActivityBinningNFold) {
		this.useActivityBinningNFold = useActivityBinningNFold;
	}
	
	public String getNumExternalFolds() {
		return numExternalFolds;
	}
	public void setNumExternalFolds(String numExternalFolds) {
		this.numExternalFolds = numExternalFolds;
	}
	
	public String getHasBeenScaled() {
		return hasBeenScaled;
	}
	public void setHasBeenScaled(String hasBeenScaled) {
		this.hasBeenScaled = hasBeenScaled;
	}
	public List<String> getUserUploadedDescriptorTypes() {
		Utility.writeToDebug("--------LIST:"+userUploadedDescriptorTypes);
		return userUploadedDescriptorTypes;
	}
	public void setUserUploadedDescriptorTypes(
			List<String> userUploadedDescriptorTypes) {
		Utility.writeToDebug("--------LIST SET:"+userUploadedDescriptorTypes);
		this.userUploadedDescriptorTypes = userUploadedDescriptorTypes;
	}
	public String getDescriptorNewName() {
		return descriptorNewName;
	}
	public void setDescriptorNewName(String descriptorNewName) {
		this.descriptorNewName = descriptorNewName;
	}
	public String getSelectedDescriptorUsedName() {
		return selectedDescriptorUsedName;
	}
	public void setSelectedDescriptorUsedName(String selectedDescriptorUsedName) {
		this.selectedDescriptorUsedName = selectedDescriptorUsedName;
	}
	
	
	
	
	
}