package edu.unc.ceccr.taskObjects;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Expression;

import edu.unc.ceccr.global.Constants;
import edu.unc.ceccr.global.Constants.DescriptorEnumeration;
import edu.unc.ceccr.global.Constants.ScalingTypeEnumeration;
import edu.unc.ceccr.persistence.DataSet;
import edu.unc.ceccr.persistence.Descriptors;
import edu.unc.ceccr.persistence.HibernateUtil;
import edu.unc.ceccr.persistence.Prediction;
import edu.unc.ceccr.persistence.PredictionValue;
import edu.unc.ceccr.persistence.Predictor;
import edu.unc.ceccr.persistence.Queue;
import edu.unc.ceccr.task.WorkflowTask;
import edu.unc.ceccr.utilities.DatasetFileOperations;
import edu.unc.ceccr.utilities.FileAndDirOperations;
import edu.unc.ceccr.utilities.PopulateDataObjects;
import edu.unc.ceccr.utilities.Utility;
import edu.unc.ceccr.workflows.CreateDirectoriesWorkflow;
import edu.unc.ceccr.workflows.GenerateDescriptorWorkflow;
import edu.unc.ceccr.workflows.GetJobFilesWorkflow;
import edu.unc.ceccr.workflows.KnnPredictionWorkflow;
import edu.unc.ceccr.workflows.ReadDescriptorsFileWorkflow;
import edu.unc.ceccr.workflows.WriteDescriptorsFileWorkflow;

public class QsarPredictionTask implements WorkflowTask {

	ArrayList<PredictionValue> allPredValues = new ArrayList<PredictionValue>();
	private String filePath;
	private String jobName;
	private String sdf;
	private String cutoff;
	private String userName;
	private String selectedPredictorIds;
	private DataSet predictionDataset;
	private String step = Constants.SETUP; //stores what step we're on 
	private int allPredsTotalModels = -1; //used by getProgress function
	private ArrayList<String> selectedPredictorNames; //used by getProgress function
	
	public String getProgress() {
		
		try{
			if(! step.equals(Constants.PREDICTING)){
				return step; 
			}
			else{
				//get the % done of the overall prediction
				
				if(allPredsTotalModels < 0){
					//we haven't read the needed predictor data yet
					//get the number of models in all predictors, and their names
					allPredsTotalModels = 0;
					selectedPredictorNames = new ArrayList<String>();	
					Session s = HibernateUtil.getSession();
					String[] selectedPredictorIdArray = selectedPredictorIds.split("\\s+");
					for(int i = 0; i < selectedPredictorIdArray.length; i++){
						Predictor selectedPredictor = PopulateDataObjects.getPredictorById(Long.parseLong(selectedPredictorIdArray[i]), s);
						allPredsTotalModels += selectedPredictor.getNumTestModels();
						selectedPredictorNames.add(selectedPredictor.getName());
					}
					s.close();
				}
				
				int modelsPredictedSoFar = 0;
				for(int i = 0; i < selectedPredictorNames.size(); i++){
					if(filePath != null){
						File predOutFile = new File(filePath + selectedPredictorNames.get(i) + "/" + Constants.PRED_OUTPUT_FILE + ".preds");
						if(predOutFile.exists()){
							//quickly count the number of lines in the output file for this predictor
							InputStream is = new BufferedInputStream(new FileInputStream(predOutFile));
						    byte[] c = new byte[1024];
						    int count = 0;
						    int readChars = 0;
						    while ((readChars = is.read(c)) != -1) {
						        for (int j = 0; j < readChars; ++j) {
						            if (c[j] == '\n')
						                ++count;
						        }
						    }
						    modelsPredictedSoFar += count - 4; //there are 4 header lines in the cons_pred.preds file
						}
						
					}
				}
				
				float progress = modelsPredictedSoFar / allPredsTotalModels;
				progress *= 100; //it's a percent
				return step + " (" + Math.round(progress) + "%)"; 
			}
			
		}catch(Exception ex){
			Utility.writeToDebug(ex);
			return "";
		}
	}
		
	public QsarPredictionTask(String userName, String jobName, String sdf, String cutoff,
			String selectedPredictorIds, DataSet predictionDataset) throws Exception {
		
		this.predictionDataset = predictionDataset;
		this.jobName = jobName;
		this.userName = userName;
		this.sdf = sdf;
		this.cutoff = cutoff;
		this.selectedPredictorIds = selectedPredictorIds;
		this.filePath = Constants.CECCR_USER_BASE_PATH + userName + "/"+ jobName + "/";
		
	}

	public void execute() throws Exception {

		Utility.writeToDebug("QsarPredictionTask: ExecutePredictor",userName,jobName);
		Utility.writeToMSDebug("QsarPredictionTask: Start"+userName+" "+jobName);

		Session s = HibernateUtil.getSession();
		
		ArrayList<Predictor> selectedPredictors = new ArrayList<Predictor>();
		String[] selectedPredictorIdArray = selectedPredictorIds.split("\\s+");

		allPredsTotalModels = 0;
		for(int i = 0; i < selectedPredictorIdArray.length; i++){
			Predictor selectedPredictor = PopulateDataObjects.getPredictorById(Long.parseLong(selectedPredictorIdArray[i]), s);
			
			//We're keeping a count of how many times each predictor was used.
	        //So, increment number of times used on each and save each predictor object.
			
	        selectedPredictor.setNumPredictions(selectedPredictor.getNumPredictions() + 1);
			Transaction tx = null;
			try {
				tx = s.beginTransaction();
				s.saveOrUpdate(selectedPredictor);
				tx.commit();
			} catch (RuntimeException e) {
				if (tx != null)
					tx.rollback();
				Utility.writeToDebug(e);
			}
			
			
			selectedPredictors.add(selectedPredictor);
		}

		
		//Now, make the prediction with each predictor.
		//Workflow will be:
		//0. copy dataset into jobDir. 
		//1. Create each of the descriptor types that will be needed.
		//for each predictor do {
		//	2. copy predictor into jobDir/predictorDir
		//	3. copy dataset from jobDir to jobDir/predictorDir. Scale descriptors to fit predictor.
		//	4. make predictions in jobDir/predictorDir
		//	5. get output, put it into predictionValue objects and save them
		//}
		//6. move jobDir into PREDICTIONS.
		
		
		//Here we go!
		//0. copy dataset into jobDir.
		step = Constants.SETUP;
		CreateDirectoriesWorkflow.createDirs(userName, jobName);
		
		String path = Constants.CECCR_USER_BASE_PATH + userName + "/" + jobName + "/";
		
		GetJobFilesWorkflow.getDatasetFiles(userName, predictionDataset, path);
		//done with 0. (copy dataset into jobDir.)
		
		//1. Create each of the descriptor types that will be needed.
		step = Constants.DESCRIPTORS;
		
		ArrayList<String> requiredDescriptors = new ArrayList<String>();
		for(int i = 0; i < selectedPredictors.size(); i++){
			String descType = selectedPredictors.get(i).getDescriptorGenerationDisplay();
			if(! requiredDescriptors.contains(descType)){
				requiredDescriptors.add(descType);
			}
		}

		String sdfile = predictionDataset.getSdfFile();
		
		for(int i = 0; i < requiredDescriptors.size(); i++){
			if(requiredDescriptors.get(i).equals(Constants.MOLCONNZ)){
				Utility.writeToDebug("ExecutePredictor: Generating MolconnZ Descriptors", userName, jobName);
				GenerateDescriptorWorkflow.GenerateMolconnZDescriptors(path + sdfile, path + sdfile + ".mz", Constants.PREDICTION);
			}
			else if(requiredDescriptors.get(i).equals(Constants.DRAGON)){
				Utility.writeToDebug("ExecutePredictor: Generating Dragon Descriptors", userName, jobName);
				GenerateDescriptorWorkflow.GenerateDragonDescriptors(path + sdfile, path + sdfile + ".dragon", Constants.PREDICTION);
			}
			else if(requiredDescriptors.get(i).equals(Constants.MOE2D)){
				Utility.writeToDebug("ExecutePredictor: Generating Moe2D Descriptors", userName, jobName);
				GenerateDescriptorWorkflow.GenerateMoe2DDescriptors(path + sdfile, path + sdfile + ".moe2D");
			}
			else if(requiredDescriptors.get(i).equals(Constants.MACCS)){
				Utility.writeToDebug("ExecutePredictor: Generating MACCS Descriptors", userName, jobName);
				GenerateDescriptorWorkflow.GenerateMaccsDescriptors(path + sdfile, path + sdfile + ".maccs");
			}
		}
		//done with step 1. (Create each of the descriptor types that will be needed.)
		
		//for each predictor do {
		for(int i = 0; i < selectedPredictors.size(); i++){
			Predictor selectedPredictor = selectedPredictors.get(i);
			
			//	2. copy predictor into jobDir/predictorDir
			
			String predictionDir = path + selectedPredictor.getName() + "/";
			new File(predictionDir).mkdirs();
			
			step = Constants.COPYPREDICTOR;
			GetJobFilesWorkflow.getPredictorFiles(userName, selectedPredictor, predictionDir);

			//  done with 2. (copy predictor into jobDir/predictorDir)
			
			//	3. copy dataset from jobDir to jobDir/predictorDir. Scale descriptors to fit predictor.
			FileAndDirOperations.copyDirContents(path, predictionDir, false);
			ArrayList<String> descriptorNames = new ArrayList<String>();
			ArrayList<Descriptors> descriptorValueMatrix = new ArrayList<Descriptors>();
			ArrayList<String> chemicalNames = DatasetFileOperations.getSDFCompoundList(path + sdfile);
			
			step = Constants.PROCDESCRIPTORS;
			
			if(selectedPredictor.getDescriptorGeneration().equals(DescriptorEnumeration.MOLCONNZ)){
				Utility.writeToDebug("ExecutePredictor: Processing MolconnZ Descriptors", userName, jobName);
				ReadDescriptorsFileWorkflow.readMolconnZDescriptors(predictionDir + sdfile + ".mz", descriptorNames, descriptorValueMatrix);
			}
			else if(selectedPredictor.getDescriptorGeneration().equals(DescriptorEnumeration.DRAGON)){
				Utility.writeToDebug("ExecutePredictor: Processing Dragon Descriptors", userName, jobName);
				ReadDescriptorsFileWorkflow.readDragonDescriptors(predictionDir + sdfile + ".dragon", descriptorNames, descriptorValueMatrix);
			}
			else if(selectedPredictor.getDescriptorGeneration().equals(DescriptorEnumeration.MOE2D)){
				Utility.writeToDebug("ExecutePredictor: Processing Moe2D Descriptors", userName, jobName);
				ReadDescriptorsFileWorkflow.readMoe2DDescriptors(predictionDir + sdfile + ".moe2D", descriptorNames, descriptorValueMatrix);
			}
			else if(selectedPredictor.getDescriptorGeneration().equals(DescriptorEnumeration.MACCS)){
				Utility.writeToDebug("ExecutePredictor: Processing MACCS Descriptors", userName, jobName);
				ReadDescriptorsFileWorkflow.readMaccsDescriptors(predictionDir + sdfile + ".maccs", descriptorNames, descriptorValueMatrix);
			}
			
			String descriptorString = Utility.StringArrayListToString(descriptorNames);
			WriteDescriptorsFileWorkflow.writePredictionXFile(
					chemicalNames, 
					descriptorValueMatrix, 
					descriptorString, 
					predictionDir + sdfile + ".renorm.x", 
					predictionDir + "train_0.x", 
					selectedPredictor.getScalingType());
			
			//  done with 3. (copy dataset from jobDir to jobDir/predictorDir. Scale descriptors to fit predictor.)
			
			//	4. make predictions in jobDir/predictorDir

			step = Constants.PREDICTING;
			Utility.writeToDebug("ExecutePredictor: Making predictions", userName, jobName);
			
			KnnPredictionWorkflow.RunKnnPlusPrediction(userName, jobName, predictionDir, sdfile, Float.parseFloat(cutoff) );
			//KnnPredictionWorkflow.RunKnnPrediction(userName, jobName, predictionDir, sdfile, Float.parseFloat(cutoff) );

			//  done with 4. (make predictions in jobDir/predictorDir)
			
			//	5. get output, put it into predictionValue objects and save them
			
			step = Constants.READPRED;
			
			
			ArrayList<PredictionValue> predValues = KnnPredictionWorkflow.ReadPredictionOutput(predictionDir, selectedPredictor.getPredictorId());
			//ArrayList<PredictionValue> predValues = parsePredOutput(predictionDir + Constants.PRED_OUTPUT_FILE, selectedPredictor.getPredictorId());
			Utility.writeToDebug("ExecPredictorActionTask: Complete", userName, jobName);
			
			allPredValues.addAll(predValues);
			
			//  done with 5. (get output, put it into predictionValue objects and save them)
			
		}
		//}
		
		//6. move jobDir into PREDICTIONS.

		KnnPredictionWorkflow.MoveToPredictionsDir(userName, jobName);
		
		//done with 6. (move jobDir into PREDICTIONS.)
		
	}
	
	protected static Predictor getPredictor(
			Long selectedPredictorId) throws ClassNotFoundException,
			SQLException {

		Predictor pred = null;
		Session session = HibernateUtil.getSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			pred = (Predictor) session.createCriteria(Predictor.class).add(Expression.eq("predictorId", selectedPredictorId)).uniqueResult();
			tx.commit();
		} catch (RuntimeException e) {
			if (tx != null)
				tx.rollback();
			Utility.writeToDebug(e);
		} finally {
			session.close();
		}
		return pred;
	}
	
	private static PredictionValue createPredObject(String[] extValues) {

		if (extValues == null) {
			return null;
		}
		int arraySize = extValues.length;
		
		PredictionValue predOutput = new PredictionValue();
		predOutput.setCompoundName(extValues[0]);
		try{
			predOutput.setNumModelsUsed(Integer.parseInt(extValues[1]));
			predOutput.setPredictedValue(Float.parseFloat(extValues[2]));
			if (arraySize > 3){
				predOutput.setStandardDeviation(Float.parseFloat(extValues[3]));
			}
		}
		catch(Exception ex){
			//if it couldn't get the information, then there is no prediction for this compound.
			//Don't worry about the NumberFormatException, it doesn't matter.
		}
		
		return predOutput;

	}
	
    @SuppressWarnings("unchecked")
	public static ArrayList<PredictionValue> parsePredOutput(String fileLocation, Long predictorId) throws IOException {
		Utility.writeToDebug("Reading prediction output from " + fileLocation);
		ArrayList<PredictionValue> allPredValue = new ArrayList<PredictionValue>();
		try{
			BufferedReader in = new BufferedReader(new FileReader(fileLocation));
			String inputString;
	
			//skip all the non-blank lines with junk in them
			while (!(inputString = in.readLine()).equals(""))
				;
			//now skip some blank lines
			while ((inputString = in.readLine()).equals(""))
				;
			//now we're at the data we need
			do {
				String[] predValues = inputString.split("\\s+");
				PredictionValue extValOutput = createPredObject(predValues);
				extValOutput.setPredictorId(predictorId);
				allPredValue.add(extValOutput);
			} while ((inputString = in.readLine()) != null);
		} catch(Exception ex){
			Utility.writeToDebug(ex);
		}
		if(allPredValue == null){
			Utility.writeToDebug("Warning: parsePredOutput returned null.");
		}
		return allPredValue;
	}
	
		
	public void cleanUp() throws Exception {
		Queue.getInstance().deleteTask(this);
	}

	public void setUp() throws Exception {

		Utility.writeToDebug("Setting up prediction task", userName, jobName);
		try{
			new File(Constants.CECCR_USER_BASE_PATH + userName + "/"+ jobName).mkdir();
			
			if(predictionDataset.getUserName().equals(userName)){
				FileAndDirOperations.copyFile(
						Constants.CECCR_USER_BASE_PATH + userName + "/DATASETS/"+predictionDataset.getFileName()+"/"+sdf, 
						Constants.CECCR_USER_BASE_PATH + userName + "/"+ jobName + "/"+sdf
						);
			}
			else{
				FileAndDirOperations.copyFile(
						Constants.CECCR_USER_BASE_PATH + "all-users" + "/DATASETS/"+predictionDataset.getFileName()+"/"+sdf, 
						Constants.CECCR_USER_BASE_PATH + userName + "/"+ jobName + "/"+sdf
						);
			}			
		}
		catch(Exception e){
			Utility.writeToMSDebug(e.getMessage());
			Utility.writeToDebug(e);
		}
	}

	public void save(){
		try{
		Prediction predictionJob = new Prediction();
		predictionJob.setDatabase(this.sdf);
		predictionJob.setUserName(this.userName);
		predictionJob.setSimilarityCutoff(new Float(this.cutoff));
		Utility.writeToDebug("saving selected predictor ids: " + selectedPredictorIds);
		predictionJob.setPredictorIds(this.selectedPredictorIds);
		predictionJob.setJobName(this.jobName);
		predictionJob.setStatus("saved");
		predictionJob.setDatasetId(predictionDataset.getFileId());
		predictionJob.setHasBeenViewed(Constants.NO);

		if(this.allPredValues == null){
			Utility.writeToDebug("Warning: allPredValue is null.");
		}
		else{
			Utility.writeToDebug("Saving prediction to database.");
		}
		
		for (PredictionValue predOutput : this.allPredValues){
			predOutput.setPredictionJob(predictionJob);
		}
		
		predictionJob.setPredictedValues(new ArrayList<PredictionValue>(allPredValues));

		Session session = HibernateUtil.getSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.save(predictionJob);		
			tx.commit();
		} catch (RuntimeException e) {
			if (tx != null)
				tx.rollback();
			Utility.writeToDebug(e);
		} finally {
			session.close();
		}
		
		File dir=new File(Constants.CECCR_USER_BASE_PATH+this.userName+"/"+this.jobName+"/");
		FileAndDirOperations.deleteDir(dir);
		
		}
		catch(Exception ex){
			Utility.writeToDebug(ex);
		}
	}
	
	public String getJobName() {
		return jobName;
	}
	
	public DataSet getPredictionDataset() {
		return predictionDataset;
	}

	public void setPredictionDataset(DataSet predictionDataset) {
		this.predictionDataset = predictionDataset;
	}

}
