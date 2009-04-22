package edu.unc.ceccr.action;

import java.sql.SQLException;
import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Expression;

import edu.unc.ceccr.global.Constants;
import edu.unc.ceccr.persistence.DataSet;
import edu.unc.ceccr.persistence.HibernateUtil;
import edu.unc.ceccr.persistence.PredictionJob;
import edu.unc.ceccr.persistence.Predictor;
import edu.unc.ceccr.persistence.Queue;
import edu.unc.ceccr.persistence.Queue.QueueTask;
import edu.unc.ceccr.persistence.Queue.QueueTask.Component;
import edu.unc.ceccr.taskObjects.QsarModelingTask;
import edu.unc.ceccr.taskObjects.QsarPredictionTask;
import edu.unc.ceccr.utilities.PopulateDataObjects;
import edu.unc.ceccr.utilities.Utility;

public class DeleteUserFile extends Action {

	ActionMapping mapping;

	private ActionForward forward;

	public ActionForward execute(ActionMapping mapping, ActionForm form,	HttpServletRequest request, HttpServletResponse response) {

		forward = mapping.findForward("success");
		
		HttpSession session = request.getSession(false);
		
		if (session == null) {
			forward = mapping.findForward("login");
		}else if (session.getAttribute("user") == null){
			forward = mapping.findForward("login");
		}else{
			try {
				String userName=request.getParameter("userName");
				
				String fileName=request.getParameter("fileName");
				
				String msg = "Cannot delete dataset! ";
				
				String jobname = checkJobNames(userName, fileName);
				String predictorname = checkPredictions(userName, fileName);
				String modellingname = checkModelling(userName, fileName);
				
				Utility.writeToMSDebug("DELETE DATASET JOBNAMES CHECK:"+jobname);
				Utility.writeToMSDebug("DELETE DATASET PREDICTORNAMES CHECK:"+predictorname);
				Utility.writeToMSDebug("DELETE DATASET ModellingNAMES CHECK:"+modellingname);
				
				if(jobname==null && predictorname==null && modellingname==null) 
					deleteDataset(userName, fileName);
				else{
					if(jobname!=null) msg += "Job "+jobname+" is using it! ";
					if(predictorname!=null) msg += "Prediction job "+predictorname+" is using it! ";
					if(modellingname!=null) msg += "Modelling job "+modellingname+" is using it! ";
					request.removeAttribute("validationMsg");
					request.setAttribute("validationMsg", msg);
					forward = mapping.findForward("failure");
				}
			} catch (Exception e) {
				forward = mapping.findForward("failure");
				Utility.writeToDebug(e);
			}
		}
		return (forward);

	}
	
	
	
	public void deleteDataset(String userName, String fileName)throws ClassNotFoundException, SQLException
	{
		DataSet dataSet=new DataSet();
				
		Session session = HibernateUtil.getSession();
		
		Transaction tx=null,tx2=null;
		
		try{
			tx=session.beginTransaction();
			dataSet=(DataSet)session.createCriteria(DataSet.class).add(Expression.eq("userName", userName)).add(Expression.eq("fileName",fileName))
			               .uniqueResult();
			tx.commit();
		}catch (RuntimeException e) {
			if (tx != null)
				tx.rollback();
			Utility.writeToDebug(e);
		}
		
		String dir = Constants.CECCR_USER_BASE_PATH+userName+"/DATASETS/"+fileName;
		Utility.writeToMSDebug("DEL::"+dir);
				
		//deleting dataset from job
		
		List<QueueTask> tasks = (List<QueueTask>) Queue.getInstance().getUserTasks(userName);
				for(Iterator<QueueTask> i=tasks.iterator();i.hasNext();){
			QueueTask temp = i.next();
			if(temp.component.equals(Component.visualisation) && temp.getJobName().equals(fileName)){
				Queue.getInstance().deleteTask(temp);
			}
			if(temp.component.equals(Component.sketches) && temp.getJobName().equals(fileName+"_sketches_generation")){
				Queue.getInstance().deleteTask(temp);
			}
		}
				
		tasks = (List<QueueTask>) Queue.getInstance().getQueuedTasks();
		for(Iterator<QueueTask> i=tasks.iterator();i.hasNext();){
			QueueTask temp = i.next();
			if(temp.getUserName().equals(userName) && temp.getJobName().equals(fileName) || temp.getJobName().equals(fileName+"_sketches_generation")){
				Queue.getInstance().deleteTask(temp);
			}
		}
			
		///
		Utility.writeToMSDebug("DeleteUserFile::"+dir);
		if(Utility.deleteDir(new File(dir)))
		{
			try{
				tx2=session.beginTransaction();
			    session.delete(dataSet);
			  // session.delete(predDatabase);
			    tx2.commit();
			}catch (RuntimeException e) {
				if (tx2 != null)
					tx2.rollback();
				Utility.writeToDebug(e);
			}
		}
		session.close();
		
	}
	
	@SuppressWarnings("unchecked")
	private String checkJobNames(String userName, String fileName)throws ClassNotFoundException, SQLException{
		List<String> jobnames = PopulateDataObjects.populateTaskNames(userName, true);
		
		for(int i=0;i<jobnames.size();i++){
			if(jobnames.get(i).equals(fileName)){
				return fileName; 
			}
			else if(jobnames.get(i).equals(fileName+"_sketches_generation")){
				return fileName+"_sketches_generation"; 
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private String checkModelling(String userName, String fileName) throws ClassNotFoundException, SQLException{
		List<QueueTask> queuedtasks  = Queue.getInstance().getQueuedTasks();
		DataSet dataset = PopulateDataObjects.getDataSetByName(fileName,userName);
		if(queuedtasks!=null && dataset!=null){
			for(int i=0;i<queuedtasks.size();i++ ){
				if(queuedtasks.get(i)!=null && queuedtasks.get(i).getComponent().equals(Component.modelbuilder) && queuedtasks.get(i).getUserName().equals(userName)){
					QsarModelingTask job = 	(QsarModelingTask)queuedtasks.get(i).task;
					if(job!=null){
						if(job.getDatasetID()!=null && job.getDatasetID().equals(dataset.getFileId())){
							return job.getJobName();
						}
					}
				}
			}		
		}
		List<QueueTask> tasks  = PopulateDataObjects.populateTasks(userName, false);
		if(tasks!=null && dataset!=null){
			for(int i=0;i<tasks.size();i++ ){
				Utility.writeToMSDebug("TASKS::"+tasks.get(i).task);
				if(tasks.get(i)!=null && 
						tasks.get(i).task!=null && 
						(tasks.get(i).task instanceof QsarModelingTask)){
					QsarModelingTask job = 	(QsarModelingTask)tasks.get(i).task;
					Utility.writeToMSDebug("MODELLING:::"+job.getJobName()+"---"+job.getDatasetID()+"----"+dataset.getFileId());
					if(job.getDatasetID()!=null && job.getDatasetID().equals(dataset.getFileId())){
						return job.getJobName();
					}
				}
			}		
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private String checkPredictions(String userName, String fileName)throws ClassNotFoundException, SQLException{
		List<QueueTask> queuedtasks  = Queue.getInstance().getQueuedTasks();
		List<PredictionJob> predictions  = PopulateDataObjects.populatePredictions(userName, false);
		DataSet dataset = PopulateDataObjects.getDataSetByName(fileName,userName);
		if(queuedtasks!=null && dataset!=null){
			for(int i=0;i<queuedtasks.size();i++ ){
				if(queuedtasks.get(i)!=null && queuedtasks.get(i).getComponent().equals(Component.predictor) && queuedtasks.get(i).getUserName().equals(userName)){
					QsarPredictionTask job = (QsarPredictionTask)queuedtasks.get(i).task;
					if(job!=null){
						Utility.writeToMSDebug("Prediction job::"+job.getPredictionDataset().getFileId());
						if(job.getPredictionDataset()!=null && job.getPredictionDataset().getFileId().equals(dataset.getFileId())){
							return job.getJobName();
						}
					}
				}
			}
		}
		if(predictions!=null && dataset!=null){
			for(int i=0;i<predictions.size();i++ ){
				if(predictions.get(i)!=null && predictions.get(i).getDatasetId()!=null &&predictions.get(i).getDatasetId().equals(dataset.getFileId())){
					return predictions.get(i).getJobName();
				}
			}
		}
		return null;
	}
	
}
