package edu.unc.ceccr.action.ViewPredictor;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

import edu.unc.ceccr.calculations.ConfusionMatrix;
import edu.unc.ceccr.calculations.RSquaredAndCCR;
import edu.unc.ceccr.global.Constants;
import edu.unc.ceccr.persistence.DataSet;
import edu.unc.ceccr.persistence.ExternalValidation;
import edu.unc.ceccr.persistence.HibernateUtil;
import edu.unc.ceccr.persistence.KnnModel;
import edu.unc.ceccr.persistence.KnnParameters;
import edu.unc.ceccr.persistence.KnnPlusModel;
import edu.unc.ceccr.persistence.KnnPlusParameters;
import edu.unc.ceccr.persistence.Prediction;
import edu.unc.ceccr.persistence.Predictor;
import edu.unc.ceccr.persistence.RandomForestGrove;
import edu.unc.ceccr.persistence.RandomForestParameters;
import edu.unc.ceccr.persistence.RandomForestTree;
import edu.unc.ceccr.persistence.SvmModel;
import edu.unc.ceccr.persistence.SvmParameters;
import edu.unc.ceccr.persistence.User;
import edu.unc.ceccr.utilities.PopulateDataObjects;
import edu.unc.ceccr.utilities.Utility;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;

public class ExternalValidationPage extends ViewPredictorAction {
	private ArrayList<ExternalValidation> externalValValues;
	private String hasGoodModels = Constants.YES;
	private ArrayList<String> residuals;
	
	//used in creation of confusion matrix (category modeling only)
	ConfusionMatrix confusionMatrix;
	String rSquared = "";
	String rSquaredAverageAndStddev = "";
	String ccrAverageAndStddev = "";
	
	public String load() throws Exception {
		getBasicParameters();
		String result = SUCCESS;
		//check that the user is logged in
		
		//get external validation compounds of predictor
		ArrayList<Predictor> childPredictors = PopulateDataObjects.getChildPredictors(selectedPredictor, session);
		if(childPredictors.size() != 0){
			//get external set for each
			externalValValues = new ArrayList<ExternalValidation>();
			SummaryStatistics childAccuracies = new SummaryStatistics(); //contains the ccr or r^2 of each child
			for(Predictor cp: childPredictors){
				ArrayList<ExternalValidation> childExtVals = (ArrayList<ExternalValidation>) PopulateDataObjects.getExternalValidationValues(cp, session);
				
				//calculate r^2 / ccr for this child
				if(childExtVals != null){
					if(selectedPredictor.getActivityType().equals(Constants.CATEGORY)){
						Double childCcr = (RSquaredAndCCR.calculateConfusionMatrix(childExtVals)).getCcr();
						childAccuracies.addValue(childCcr);
					}
					else if(selectedPredictor.getActivityType().equals(Constants.CONTINUOUS)){
						Double childRSquared = (RSquaredAndCCR.calculateConfusionMatrix(childExtVals)).getCcr();
						childAccuracies.addValue(childRSquared);
					}
					externalValValues.addAll(childExtVals);
				}
			}
			Double mean = childAccuracies.getMean();
			Double stddev = childAccuracies.getStandardDeviation();
			
			if(selectedPredictor.getActivityType().equals(Constants.CONTINUOUS)){
				rSquaredAverageAndStddev = Utility.roundSignificantFigures(""+mean, Constants.REPORTED_SIGNIFICANT_FIGURES);
				rSquaredAverageAndStddev += " � ";
				rSquaredAverageAndStddev += Utility.roundSignificantFigures(""+stddev, Constants.REPORTED_SIGNIFICANT_FIGURES);
				Utility.writeToDebug("ccr avg and stddev: " + rSquaredAverageAndStddev);
			}
			else if(selectedPredictor.getActivityType().equals(Constants.CATEGORY)){
				ccrAverageAndStddev = Utility.roundSignificantFigures(""+mean, Constants.REPORTED_SIGNIFICANT_FIGURES);
				ccrAverageAndStddev += " � ";
				ccrAverageAndStddev += Utility.roundSignificantFigures(""+stddev, Constants.REPORTED_SIGNIFICANT_FIGURES);
				Utility.writeToDebug("ccr avg and stddev: " + ccrAverageAndStddev);
			}
		}
		else{
			externalValValues= (ArrayList<ExternalValidation>) PopulateDataObjects.getExternalValidationValues(selectedPredictor, session);
		}
		
		if(externalValValues == null || externalValValues.isEmpty()){
			Utility.writeToDebug("ext validation set empty!");
			externalValValues = new ArrayList<ExternalValidation>();
			return result;
		}
		
		dataset = PopulateDataObjects.getDataSetById(selectedPredictor.getDatasetId(), session);
		
		//calculate residuals and fix significant figures on output data
		ArrayList<Double> residualsAsDouble = RSquaredAndCCR.calculateResiduals(externalValValues);

		hasGoodModels = Constants.NO;
		residuals = new ArrayList<String>();
		for(Double residual: residualsAsDouble){
			if(residual.isNaN()){
				residuals.add("");
			}
			else{
				//if at least one residual exists, there must have been a good model
				hasGoodModels = Constants.YES;
				residuals.add(Utility.roundSignificantFigures(""+residual, Constants.REPORTED_SIGNIFICANT_FIGURES));
			}
		}
		if(hasGoodModels.equals(Constants.NO)){
			return result;
		}
		
		if(selectedPredictor.getActivityType().equals(Constants.CATEGORY)){
			//if category model, create confusion matrix.
			//round off the predicted values to nearest integer.
			confusionMatrix = RSquaredAndCCR.calculateConfusionMatrix(externalValValues);
		}
		else if(selectedPredictor.getActivityType().equals(Constants.CONTINUOUS) && externalValValues.size() > 1){
			//if continuous, calculate overall r^2 and... r0^2? or something? 
			//just r^2 for now, more later.
			Double rSquaredDouble = RSquaredAndCCR.calculateRSquared(externalValValues, residualsAsDouble);
			rSquared = Utility.roundSignificantFigures("" + rSquaredDouble, Constants.REPORTED_SIGNIFICANT_FIGURES);
		}
		return result;
	}
	
	//getters and setters

	public List<ExternalValidation> getExternalValValues() {
		return externalValValues;
	}
	public void setExternalValValues(ArrayList<ExternalValidation> externalValValues) {
		this.externalValValues = externalValValues;
	}

	public String getHasGoodModels() {
		return hasGoodModels;
	}
	public void setHasGoodModels(String hasGoodModels) {
		this.hasGoodModels = hasGoodModels;
	}

	public ArrayList<String> getResiduals() {
		return residuals;
	}
	public void setResiduals(ArrayList<String> residuals) {
		this.residuals = residuals;
	}
	
	public String getrSquared() {
		return rSquared;
	}
	public void setrSquared(String rSquared) {
		this.rSquared = rSquared;
	}

	public String getrSquaredAverageAndStddev() {
		return rSquaredAverageAndStddev;
	}
	public void setrSquaredAverageAndStddev(String rSquaredAverageAndStddev) {
		this.rSquaredAverageAndStddev = rSquaredAverageAndStddev;
	}

	public ConfusionMatrix getConfusionMatrix() {
		return confusionMatrix;
	}
	public void setConfusionMatrix(ConfusionMatrix confusionMatrix) {
		this.confusionMatrix = confusionMatrix;
	}

	public String getCcrAverageAndStddev() {
		return ccrAverageAndStddev;
	}
	public void setCcrAverageAndStddev(String ccrAverageAndStddev) {
		this.ccrAverageAndStddev = ccrAverageAndStddev;
	}
	
	//end getters and setters
}