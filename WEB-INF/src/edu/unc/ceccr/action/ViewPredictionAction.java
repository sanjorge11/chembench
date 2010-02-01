package edu.unc.ceccr.action;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
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
import org.hibernate.Transaction;

import edu.unc.ceccr.action.ViewDataset.Compound;
import edu.unc.ceccr.global.Constants;
import edu.unc.ceccr.persistence.DataSet;
import edu.unc.ceccr.persistence.ExternalValidation;
import edu.unc.ceccr.persistence.HibernateUtil;
import edu.unc.ceccr.persistence.Model;
import edu.unc.ceccr.persistence.Prediction;
import edu.unc.ceccr.persistence.PredictionValue;
import edu.unc.ceccr.persistence.Predictor;
import edu.unc.ceccr.persistence.Queue;
import edu.unc.ceccr.persistence.User;
import edu.unc.ceccr.persistence.Queue.QueueTask;
import edu.unc.ceccr.task.Task;
import edu.unc.ceccr.taskObjects.QsarModelingTask;
import edu.unc.ceccr.utilities.DatasetFileOperations;
import edu.unc.ceccr.utilities.PopulateDataObjects;
import edu.unc.ceccr.utilities.Utility;
import edu.unc.ceccr.persistence.Queue.QueueTask.jobTypes;

public class ViewPredictionAction extends ActionSupport {
	
	private User user;
	private String predictionId;
	private Prediction prediction;
	private List<Predictor> predictors; //put these in order by predictorId
	private DataSet dataset; //dataset used in prediction
	ArrayList<CompoundPredictions> compoundPredictionValues = new ArrayList<CompoundPredictions>();
	private String currentPageNumber;
	private String orderBy;
	private String datasetId; 
	
	public class CompoundPredictions{
		String compound;
		ArrayList<PredictionValue> predictionValues;

		public String getCompound() {
			return compound;
		}
		public void setCompound(String compound) {
			this.compound = compound;
		}
		public ArrayList<PredictionValue> getPredictionValues() {
			return predictionValues;
		}
		public void setPredictionValues(ArrayList<PredictionValue> predictionValues) {
			this.predictionValues = predictionValues;
		}
	}
	
	public String loadPredictionsSection() throws Exception {
		String result = SUCCESS;
		//check that the user is logged in
		ActionContext context = ActionContext.getContext();

		Session session = HibernateUtil.getSession();
		
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
			if(context.getParameters().get("orderBy") != null){
				 orderBy = ((String[]) context.getParameters().get("orderBy"))[0];
			}
			if(context.getParameters().get("currentPageNumber") != null){
				currentPageNumber = ((String[]) context.getParameters().get("currentPageNumber"))[0]; 	
			}
			if(context.getParameters().get("predictionId") != null){
				predictionId = ((String[]) context.getParameters().get("predictionId"))[0]; 	
			}
				
			//get prediction
			Utility.writeToStrutsDebug("prediction id: " + predictionId);
			prediction = PopulateDataObjects.getPredictionById(Long.parseLong(predictionId), session);
			if(predictionId == null){
				Utility.writeToStrutsDebug("Invalid prediction ID supplied.");
			}
			
			//define which compounds will appear on page
			int pagenum = Integer.parseInt(currentPageNumber) - 1;
			int limit = Integer.parseInt(user.getViewPredictionCompoundsPerPage()); //compounds per page to display
			int offset = pagenum * limit; //which compoundid to start on
         	
			//get prediction values
			compoundPredictionValues = new ArrayList<CompoundPredictions>();
			String datasetUser = dataset.getUserName();
			if(datasetUser.equals("_all")){
				datasetUser = "all-users";
			}
			
			String datasetDir = Constants.CECCR_USER_BASE_PATH + datasetUser + "/";
			datasetDir += "DATASETS/" + dataset.getFileName() + "/";
			Utility.writeToDebug("opening file: " + datasetDir + dataset.getSdfFile());
			ArrayList<String> compoundIDs = DatasetFileOperations.getSDFCompoundList(datasetDir + dataset.getSdfFile());
			
			for(String cid: compoundIDs){
				Compound c = new Compound();
				c.setCompoundId(cid);
				datasetCompounds.add(c);
			}
			
			//get activity values (if applicable)
			if(! dataset.getDatasetType().equals(Constants.PREDICTION)){
				HashMap<String, String> actIdsAndValues = DatasetFileOperations.getActFileIdsAndValues(datasetDir + dataset.getActFile());
				
				for(Compound c: datasetCompounds){
					c.setActivityValue(actIdsAndValues.get(c.getCompoundId()));
				}
			}

			//sort the compound array
			if(orderBy == null || orderBy.equals("") || orderBy.equals("compoundId")){
				//sort by compoundId
				Collections.sort(datasetCompounds, new Comparator<Compound>() {
				    public int compare(Compound o1, Compound o2) {
			    		return o1.getCompoundId().compareTo(o2.getCompoundId());
				    }});
			}
			else if(orderBy == "activityValue" && ! dataset.getDatasetType().equals(Constants.PREDICTION)){
				Collections.sort(datasetCompounds, new Comparator<Compound>() {
				    public int compare(Compound o1, Compound o2) {
				    	float f1 = Float.parseFloat(o1.getActivityValue());
				    	float f2 = Float.parseFloat(o2.getActivityValue());
				    	return (f2 > f1? 1:-1);
				    }});
			}

			//pick out the ones to be displayed on the page based on offset and limit
			int compoundNum = 0;
			for(int i = 0; i < datasetCompounds.size(); i++){
				if(compoundNum < offset || compoundNum >= (offset + limit)){
					//don't display this compound
					datasetCompounds.remove(i);
					i--;
				}				
				else{
					//leave it in the array
				}
				compoundNum++;
			}

			pageNums = new ArrayList<String>(); //displays the page numbers at the top
			int j = 1;
			for(int i = 0; i < compoundIDs.size(); i += limit){
				String page = Integer.toString(j);
				pageNums.add(page);
				j++;
			}
		}
		return result;
	}

	public String loadWarningsSection() throws Exception {
		String result = SUCCESS;
		//check that the user is logged in
		ActionContext context = ActionContext.getContext();

		Session session = HibernateUtil.getSession();
		
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
			
			if(context.getParameters().get("predictionId") != null){
				predictionId = ((String[]) context.getParameters().get("predictionId"))[0]; 	
			}
			//get prediction
			Utility.writeToStrutsDebug("[ext_compounds] dataset id: " + predictionId);
			prediction = PopulateDataObjects.getPredictionById(Long.parseLong(predictionId), session);
			if(predictionId == null){
				Utility.writeToStrutsDebug("Invalid prediction ID supplied.");
			}
		}
		return result;
	}
	
	
	private void populateCompoundPredictionValues(String orderBy, String pageNumber, String compoundsPerPage, Session session) throws Exception{
		
		//get compounds
		String predictionDir = Constants.CECCR_USER_BASE_PATH + user.getUserName() + "/PREDICTIONS/" + prediction.getJobName() + "/";
		ArrayList<String> compounds = DatasetFileOperations.getSDFCompoundList(predictionDir + dataset.getSdfFile());
		
		Utility.writeToDebug("numCompounds: " + compounds.size());
		for(int i = 0; i < compounds.size(); i++){
			CompoundPredictions cp = new CompoundPredictions();
			cp.compound = compounds.get(i);
			//get prediction values
			cp.predictionValues = (ArrayList<PredictionValue>) PopulateDataObjects.getPredictionValuesByPredictionIdAndCompoundId(Long.parseLong(predictionId), cp.compound, session);

			//round them to a reasonable number of significant figures
			for(PredictionValue pv : cp.predictionValues){
				int sigfigs = Constants.REPORTED_SIGNIFICANT_FIGURES;
				if(pv.getPredictedValue() != null){
					String predictedValue = DecimalFormat.getInstance().format(pv.getPredictedValue()).replaceAll(",", "");
					pv.setPredictedValue(Float.parseFloat(Utility.roundSignificantFigures(predictedValue, sigfigs)));
				}
				if(pv.getStandardDeviation() != null){
					String stddev = DecimalFormat.getInstance().format(pv.getStandardDeviation()).replaceAll(",", "");
					pv.setStandardDeviation(Float.parseFloat(Utility.roundSignificantFigures(stddev, sigfigs)));
				}
			}
			compoundPredictionValues.add(cp);
		}
		
		//sort the compound predictions array
		if(orderBy.equals("")){
			//sort by compoundId
		}
	
		//pick out the ones to be displayed on the page based on orderBy, pageNumber, and compoundsPerPage
		
	}
	
	public String loadPage() throws Exception {

		String result = SUCCESS;
		
		//check that the user is logged in
		ActionContext context = ActionContext.getContext();

		Session session = HibernateUtil.getSession();
		
		if(context == null){
			Utility.writeToStrutsDebug("No ActionContext available");
		}
		else{
			user = (User) context.getSession().get("user");
			predictionId = ((String[]) context.getParameters().get("id"))[0];
			
			if(user == null){
				Utility.writeToStrutsDebug("No user is logged in.");
				result = LOGIN;
				return result;
			}

			if(context.getParameters().get("orderBy") != null){
				 orderBy = ((String[]) context.getParameters().get("orderBy"))[0];
			}
			String pagenumstr = null;
			if(context.getParameters().get("pagenum") != null){
				pagenumstr = ((String[]) context.getParameters().get("pagenum"))[0]; //how many to skip (pagination)
			}
			
			if(context.getParameters().get("predictionId") != null){
				predictionId = ((String[]) context.getParameters().get("predictionId"))[0]; 	
			}
			
			if(predictionId == null){
				Utility.writeToStrutsDebug("No prediction ID supplied.");
			}
			else{

				currentPageNumber = "1";
				if(pagenumstr != null){
					currentPageNumber = pagenumstr;
				}
				
				Utility.writeToStrutsDebug("prediction id: " + predictionId);
				prediction = PopulateDataObjects.getPredictionById(Long.parseLong(predictionId), session);
				prediction.setDatasetDisplay(PopulateDataObjects.getDataSetById(prediction.getDatasetId(), session).getFileName());
				if(predictionId == null){
					Utility.writeToStrutsDebug("Invalid prediction ID supplied.");
				}
				
				//get predictors for this prediction
				predictors = new ArrayList<Predictor>();
				String[] predictorIds = prediction.getPredictorIds().split("\\s+");
				for(int i = 0; i < predictorIds.length; i++){
					predictors.add(PopulateDataObjects.getPredictorById(Long.parseLong(predictorIds[i]), session));
				}
				
				//get dataset
				dataset = PopulateDataObjects.getDataSetById(prediction.getDatasetId(), session);
				
				//the prediction has now been viewed. Update DB accordingly.
				if(! prediction.getHasBeenViewed().equals(Constants.YES)){
					prediction.setHasBeenViewed(Constants.YES);
					Transaction tx = null;
					try {
						tx = session.beginTransaction();
						session.saveOrUpdate(prediction);
						tx.commit();
					} catch (RuntimeException e) {
						if (tx != null)
							tx.rollback();
						Utility.writeToDebug(e);
					}
				}
			}
		}

		session.close();
		
		//log the results
		if(result.equals(SUCCESS)){
			Utility.writeToStrutsDebug("Forwarding user " + user.getUserName() + " to viewPrediction page.");
		}
		else{
			Utility.writeToStrutsDebug("Cannot load page.");
		}
		
		return result;
	}

	
	public String getPredictionId() {
		return predictionId;
	}
	public void setPredictionId(String predictionId) {
		this.predictionId = predictionId;
	}

	public Prediction getPrediction() {
		return prediction;
	}
	public void setPrediction(Prediction prediction) {
		this.prediction = prediction;
	}

	public List<Predictor> getPredictors() {
		return predictors;
	}
	public void setPredictors(List<Predictor> predictors) {
		this.predictors = predictors;
	}
		
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}

	public DataSet getDataset() {
		return dataset;
	}
	public void setDataset(DataSet dataset) {
		this.dataset = dataset;
	}
	
	public ArrayList<CompoundPredictions> getCompoundPredictionValues() {
		return compoundPredictionValues;
	}
	public void setCompoundPredictionValues(
			ArrayList<CompoundPredictions> compoundPredictionValues) {
		this.compoundPredictionValues = compoundPredictionValues;
	}
}