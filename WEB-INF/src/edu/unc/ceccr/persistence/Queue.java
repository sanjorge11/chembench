package edu.unc.ceccr.persistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.criterion.Expression;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;

import edu.unc.ceccr.global.Constants.DescriptorEnumeration;
import edu.unc.ceccr.global.Constants.KnnEnumeration;
import edu.unc.ceccr.global.Constants;
import edu.unc.ceccr.messages.ErrorMessages;
import edu.unc.ceccr.task.WorkflowTask;
import edu.unc.ceccr.taskObjects.ModellingTask;
import edu.unc.ceccr.taskObjects.PredictionTask;
import edu.unc.ceccr.taskObjects.QsarPredictionTask;
import edu.unc.ceccr.taskObjects.GenerateDatasetInfoActionTask;
import edu.unc.ceccr.taskObjects.GenerateSketchesTask;
import edu.unc.ceccr.taskObjects.QsarModelingTask;
import edu.unc.ceccr.taskObjects.VisualizationTask;
import edu.unc.ceccr.utilities.*;

public class Queue {
	
	@Entity()
	@Table(name = "cbench_task")
	public static class QueueTask {
		
		public enum State {	ready, started, finished, error, PermissionRequired, deleted};
		public enum Component {	modelbuilder, predictor, visualisation, sketches };
		private State state;
		private String message;
		private Date finish;
		private Date start;
		private Date submit;
		public WorkflowTask task;
		private String userName;
		public String jobName;
		public Long id = null;
		public Component component;
		private KnnEnumeration modelMethod;
		private DescriptorEnumeration modelDescriptors;
		private String ACTFile;
		private String SDFile;
		private int numCompounds;
		private int numModels;
		
		public QueueTask(WorkflowTask task, String userName)throws FileNotFoundException,IOException {
			
			this.setSubmit(new Date()); 
			this.task = task;
			this.setUserName(userName);
			
			if (task instanceof QsarModelingTask) 
			{
				QsarModelingTask t = (QsarModelingTask) task;
				this.jobName = t.getJobName();
				this.component = Component.modelbuilder;
				this.ACTFile = t.getActFileName();
				this.SDFile = t.getSdFileName();
				String temp = Utility.numCompounds(Constants.CECCR_USER_BASE_PATH+userName+"/"+t.getJobName()+"/"+t.getActFileName());
				this.numCompounds= -1;
				if(!temp.contains(ErrorMessages.ACT_CONTAINS_DUPLICATES)) this.numCompounds= new Integer(temp).intValue(); 
				this.numModels=Utility.numModels(t);
				
				//if it's a big job, require an admin's approval before running it.
				if(this.numCompounds>Constants.MAXCOMPOUNDS||this.numModels>Constants.MAXMODELS)
				{
					//if the user's an admin, just let the job run - no permission required.
					if(! Utility.isAdmin(userName)){	
						this.setState(State.PermissionRequired);
						
						try{
							instance.saveTaskRecord(this);
						}catch(ClassNotFoundException e){Utility.writeToDebug(e);}
						catch(SQLException e){Utility.writeToDebug(e);}
						
						try {
							sentRequestEmail(this.getUserName(), jobName,numCompounds,numModels);
						} catch (Exception e) {
							Utility.writeToDebug(e);
						}
					}
					else{
						this.setState(State.ready);
					}
				}
				else	{
					this.setState(State.ready);
				}
				
			}else if(task instanceof QsarPredictionTask){
				QsarPredictionTask t = (QsarPredictionTask) task;
				this.setState(State.ready);
				this.jobName = t.getJobName();
				this.component = Component.predictor;
			}
			else if(task instanceof GenerateDatasetInfoActionTask){
				GenerateDatasetInfoActionTask t = (GenerateDatasetInfoActionTask) task;	
				this.setState(State.ready);
				this.jobName = t.getJobName();
				this.component = Component.visualisation;
		}
			else if(task instanceof GenerateSketchesTask){
				GenerateSketchesTask t = (GenerateSketchesTask) task;
				this.setState(State.ready);
				this.jobName = t.getJobName();
				this.component = Component.sketches;
			}
			
			saveTask();
		}

		public void saveTask(){
			//saves the task into the database (in chembench_task table)
			try{
			instance.saveTaskRecord(this);}
			catch(SQLException e){
				Utility.writeToDebug(e);
			}
			catch(ClassNotFoundException e){
				Utility.writeToDebug(e);
			}
		}
		
		@Column(name = "num_comp")
		public int getNumCompounds()
		{
			return numCompounds;
		}
		public void setNumCompounds(int num)
		{
			this.numCompounds=num;
		}
		@Column(name = "num_models")
		public int getNumModels()
		{
			return numModels;
		}
		public void setNumModels(int num)
		{
			this.numModels=num;
		}

		@Column(name="state")
		@Enumerated(value = EnumType.STRING)
		public State getState() {
			return state;
		}

		public void setState(State state) {
			if(this.state == QueueTask.State.finished){
				return;
			}
			
			if(this.userName != null && this.jobName != null){
				Utility.writeToDebug("State changed from " + this.state + " to " + state.toString(), userName, jobName);
				this.state = state;
				saveTask();
			}
			else{
				this.state = state;
			}
		}
		
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "task_id")
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
		

		public void setSubmit(Date submit) {
			this.submit = submit;
		}

		public Date getSubmit() {
			return submit;
		}

		public void setStart(Date start) {
			this.start = start;
		}

		public Date getStart() {
			return start;
		}

		public void setFinish(Date finish) {
			this.finish = finish;
		}

		public Date getFinish() {
			return finish;
		}
        @Column(name="username")
		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}

		@Column(name="jobname")
		public String getJobName() {
			return jobName;
		}

		public void setJobName(String jobName) {
			this.jobName = jobName;
		}

		@Column(name="message")
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
			if(this.getState() != null){
				this.saveTask();
			}
		}
		
		public QueueTask() {	super(); }

	
		@Enumerated(EnumType.STRING)
		@Column(name = "job_type")
		public Component getComponent() {
			return component;
		}

		public void setComponent(Component component) {
			this.component = component;
		}

		@Column(name = "ACTFileName")
		public String getACTFile() {
			return ACTFile;
		}

		public void setACTFile(String file) {
			ACTFile = file;
		}

		@Enumerated(EnumType.STRING)
		@Column(name = "model_descriptors")
		public DescriptorEnumeration getModelDescriptors() {
			return modelDescriptors;
		}

		public void setModelDescriptors(DescriptorEnumeration modelDescriptors) {
			this.modelDescriptors = modelDescriptors;
		}

		@Enumerated(EnumType.STRING)
		@Column(name = "model_method")
		public KnnEnumeration getModelMethod() {
			return modelMethod;
		}

		public void setModelMethod(KnnEnumeration modelMethod) {
			this.modelMethod = modelMethod;
		}

	
		@Column(name = "SDFileName")
		public String getSDFile() {
			return SDFile;
		}

		public void setSDFile(String file) {
			SDFile = file;
		}
		
		public void sentRequestEmail(String userName,String  jobName,int numCompounds,int numModels) throws Exception
		{
			Properties props=System.getProperties();
			props.put(Constants.MAILHOST,Constants.MAILSERVER);
			javax.mail.Session session=javax.mail.Session.getInstance(props,null);
			
			Message m=new MimeMessage(session);
			m.setFrom(new InternetAddress(Constants.WEBSITEEMAIL));
			
			Iterator it=Constants.ADMINEMAIL_LIST.iterator();
			while(it.hasNext())
			{
				m.addRecipient(Message.RecipientType.TO,new InternetAddress((String)it.next()));
			}
			m.setSubject("Permission Request");
			
			String HtmlBody="(This message is automatically generated by the C-Chembench system.  Please do NOT reply.) "
			+"<br/><br/><br/>The user<b> "+userName+" </b>has been submitted the job <b>"+jobName+"</b>."
			+"This job has "+numCompounds+" compounds and "+numModels+"models.<br/> According to the system rule, this job has been suspended."
			+"<br/> <br/>To give the permission,  please log on to CECCR : "+Constants.WEBADDRESS
			+"<br/><br/> Thank you,"
			+"<br/><br/><br/>"
			+ new Date();
			
			m.setContent(HtmlBody, "text/html");
			Transport.send(m);
		}
		
		public void cleanFiles()
		{
			Utility.writeToMSDebug("Cleaning files from queue::"+Constants.CECCR_USER_BASE_PATH +this.getUserName()+"/"+this.getJobName());
			String BASE=Constants.CECCR_USER_BASE_PATH ;
			File file=new File(BASE+this.getUserName()+"/"+this.getJobName());
		    Utility.deleteDir(file);
		}

	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////

	public  static ConcurrentLinkedQueue<QueueTask> queue = new ConcurrentLinkedQueue<QueueTask>();
	public ConcurrentLinkedQueue<QueueTask> finished = new ConcurrentLinkedQueue<QueueTask>();
	public static Queue instance = new Queue();
	
	public Collection<QueueTask> getTasks() { return queue; }
	
	public static Queue getInstance() {	return instance;	}

	public QueueTask runningTask;
	
	public Set<String> f = new HashSet<String>();

	private Queue() {
		super();
		try {
			loadTaskRecords();
		} catch (Exception e) {
			Utility.writeToDebug(e);
		}
		startRunning();
	}
	
	public void startRunning()
	{
	    exec=new Qthread();
		exec.start();
	}
	public  void interruptRunning()throws InterruptedException
	{
		exec.done();
	}
	public volatile Qthread exec;
	
	public class Qthread extends Thread
	{
		private boolean threadDone = false;
		
		public void done() {
	        threadDone = true;
	    }
		
		public void run() 
		{
			while (!threadDone){
					
				while (queue.isEmpty()) {
					try{
						//Utility.writeToDebug("empty queue.");
						sleep(500);
					} catch (InterruptedException e) {
						Utility.writeToDebug(e);
					}
				}
				
				try{
					//Utility.writeToDebug("empty queue.");
					sleep(500);
				} catch (InterruptedException e) {
					Utility.writeToDebug(e);
				}
				//Utility.writeToDebug("non-empty queue.");
				
				QueueTask.State state=QueueTask.State.ready;

				QueueTask t = queue.poll();
				//Utility.writeToDebug("Task found at top of queue.", t.userName, t.jobName);
				
				try{ state=loadState(t);
				}catch(ClassNotFoundException e){
					Utility.writeToDebug(e);
				}catch(SQLException e){
					Utility.writeToDebug(e);
				}
									
				if(state==QueueTask.State.deleted)
			    {
			    	deleteTask(t);
			    }
			    else if(state.equals(QueueTask.State.PermissionRequired)){
					if(t.task != null){
						queue.add(t);
					}
					else{
						queue.remove(t);
					}
					
					try{sleep(100);}		
					catch (InterruptedException e) 	{	Utility.writeToDebug(e);	}
				
				}else{
					t.setStart(new Date());
					t.setState(QueueTask.State.started);
					resetFlagSet();
					runningTask = t;
					try {
						saveTaskRecord(runningTask);
						Utility.writeToDebug("Starting task.", t.userName, t.jobName);
	
						if(t.task != null){
							t.task.execute();
						}
						else{
							//t.setMessage("server reset");
							throw new Exception("Job does not contain a workflowTask. Cannot execute.");
						}
	
						Utility.writeToDebug("Task finished.", t.userName, t.jobName);
						t.setFinish(new Date());
						t.task.save();
						t.setState(QueueTask.State.finished);					
						resetFlagSet();

						runningTask = null;
						finished.add(t);
						
						for(QueueTask ta : queue){
							if(ta.jobName.equals(t.jobName) && ta.userName.equals(t.userName)){
								queue.remove(ta);
							}
						}						
						resetFlagSet();
					} catch (Exception e) {
						Utility.writeToDebug(e);
						t.setState(QueueTask.State.error);
					}
				}
			}
		}
	}
	
	public void addJob(WorkflowTask job, String userName, String jobName) throws FileNotFoundException,IOException,SQLException,ClassNotFoundException{
		queue.add(new QueueTask(job, userName));
		Utility.writeToDebug("Adding Task ", userName, jobName);
		resetFlagSet();
	}

	protected QueueTask.State loadState(QueueTask task)throws HibernateException,	ClassNotFoundException, SQLException 
	{
		QueueTask t=null;
		Session s = HibernateUtil.getSession();
		Transaction tx = null;
		try {
			tx = s.beginTransaction();
			t=(QueueTask)s.createCriteria(QueueTask.class).add(Expression.eq("jobName", task.jobName))
			    .add(Expression.eq("userName",task.userName)).uniqueResult();
			tx.commit();
		} catch (RuntimeException e) {
			if (tx != null)
				tx.rollback();
			Utility.writeToDebug(e);
		} finally {
			s.close();
		}
		
		return t.getState();
	}

	protected void saveTaskRecord(QueueTask t) throws HibernateException,	ClassNotFoundException, SQLException {
		Session s = HibernateUtil.getSession();
		Transaction tx = null;
		try {
			tx = s.beginTransaction();
			s.saveOrUpdate(t);
			tx.commit();
			if(t.getComponent().equals(QueueTask.Component.modelbuilder)){
				Utility.writeToMSDebug("MoDELBUILDER QUEUE");
				ModellingTask mt = new ModellingTask(t.id, ((QsarModelingTask)t.task).getDatasetID());
				tx = s.beginTransaction();
				s.saveOrUpdate(mt);
				tx.commit();
			}
			if(t.getComponent().equals(QueueTask.Component.predictor)){
				Utility.writeToMSDebug("PREDICTOR QUEUE");
				PredictionTask pt = new PredictionTask(t.id, ((QsarPredictionTask)t.task).getPredictionDataset().getFileId());
				tx = s.beginTransaction();
				s.saveOrUpdate(pt);
				tx.commit();
			}
			if(t.getComponent().equals(QueueTask.Component.visualisation)){
				Utility.writeToMSDebug("VISUALIZATION QUEUE");
				VisualizationTask vt = new VisualizationTask(t.id, ((QsarModelingTask)t.task).getDatasetID());
				tx = s.beginTransaction();
				s.saveOrUpdate(vt);
				tx.commit();
			}
		} catch (RuntimeException e) {
			if (tx != null)
				tx.rollback();
			Utility.writeToDebug(e);
		} finally {
			s.close();
		}
	}
	
	protected void resetFlagSet() {
		f.clear();
	}
	
	public boolean testFlag(String name) {
		return f.contains(name);
	}
	
	public void setFlag(String name) {
		f.add(name);
	}

	protected void deleteTaskRecord(QueueTask t) throws HibernateException,
			ClassNotFoundException, SQLException {
		Utility.writeToDebug("DeleteTaskRecord: " + t.id);
		Session s = HibernateUtil.getSession();
		Transaction tx = null;
		try {
			tx = s.beginTransaction();
			s.delete(t);
			tx.commit();
		} catch (RuntimeException e) {
			if (tx != null)
				tx.rollback();
			Utility.writeToDebug(e);
		} finally {
			s.close();
		}
	}

	@SuppressWarnings("unchecked")
	public void loadTaskRecords() throws HibernateException,
			ClassNotFoundException, SQLException {
		Session s = HibernateUtil.getSession();
		Transaction tx = null;
		List<QueueTask> ls = new LinkedList<QueueTask>();
		try {
			tx = s.beginTransaction();
			ls.addAll(s.createCriteria(QueueTask.class).addOrder(
					Order.asc("submit")).list());
			tx.commit();
		} catch (RuntimeException e) {
			if (tx != null)
				tx.rollback();
			Utility.writeToDebug(e);
		} finally {
			s.close();
		}

		Utility.writeToDebug("Loading tasks from database.");
		for (Iterator<QueueTask> i = ls.iterator( ); i.hasNext( ); ) {

			QueueTask t = i.next( );
			
			if(t.state==QueueTask.State.finished || t.state==QueueTask.State.error)
			{
				//Utility.writeToDebug("Adding job " + t.jobName + " to finished.");
				finished.add(t);
			}
			else{
				//Utility.writeToDebug("Adding job " + t.jobName + " to queue.");
				queue.add(t);
			}
		}
	}

	
	@SuppressWarnings("unchecked")
	public List<QueueTask> getQueuedTasks()throws SQLException,ClassNotFoundException {
		List<QueueTask> ls = new LinkedList<QueueTask>();
		
		if(runningTask!=null){ls.add(runningTask);}
		
		Session s = HibernateUtil.getSession();
		Transaction tx = null;
		try {
			tx = s.beginTransaction();
			ls.addAll(s.createCriteria(QueueTask.class).addOrder(Order.asc("submit")).add(Expression.eq("state",QueueTask.State.PermissionRequired)).list());
					    
			tx.commit();
		} catch (RuntimeException e) {
			if (tx != null)
				tx.rollback();
			Utility.writeToDebug(e);
		} finally {
			s.close();
		}
		
		return ls;
	}
	
	@SuppressWarnings("unchecked")
	public List<QueueTask> totalTasksInQ() throws HibernateException,ClassNotFoundException, SQLException {
		List<QueueTask> ls = new LinkedList<QueueTask>();
		Session s = HibernateUtil.getSession();
		Transaction tx = null;
		try {
			tx = s.beginTransaction();
			ls.addAll(s.createCriteria(QueueTask.class).addOrder(Order.asc("submit"))
					.add(Expression.ne("state",QueueTask.State.finished)).add(Expression.ne("state",QueueTask.State.error)).list());
			tx.commit();
		} catch (RuntimeException e) {
			if (tx != null)
				tx.rollback();
			Utility.writeToDebug(e);
		} finally {
			s.close();
		}
		
		return ls;
	}
	
	public List<QueueTask> getUserTasks(String userName) {
		List<QueueTask> ls = new LinkedList<QueueTask>();
		finished.clear();
		try{
			loadTaskRecords();
		}catch(Exception e){
			Utility.writeToDebug(e);
		}
		for (QueueTask t : finished) {
			if (t.getUserName().equals(userName)) {
				if(t.getState().equals(QueueTask.State.finished) || t.getState().equals(QueueTask.State.error))
				{ls.add(t);}
			}
		}
		for (QueueTask t : queue) {
			if (t.getUserName().equals(userName)) {
				if(t.getState().equals(QueueTask.State.finished) || t.getState().equals(QueueTask.State.error))
				{ls.add(t);}
			}
		}
		return ls;

	}

	public void deleteTask(WorkflowTask task) {
		QueueTask del = null;
		Utility.writeToDebug("Looking for task in 'finished' queue.");
		for (QueueTask t : finished) {
			if (t.task == task)
				del = t;
		}
		if (del == null) {
			Utility.writeToDebug("Not found in 'finished' queue. Looking in queue.");
			//what if the job was not in "finished"? We still need to remove it!
			for (QueueTask t : queue) {
				if (t.task == task){
						del = t;
				}
			}
		}
		if (del != null) {
			this.finished.remove(del);
			try {
				deleteTaskRecord(del);
			} catch (HibernateException e) {
				Utility.writeToDebug(e);
			} catch (ClassNotFoundException e) {
				Utility.writeToDebug(e);
			} catch (SQLException e) {
				Utility.writeToDebug(e);
			}
		}
	
		resetFlagSet();
	}
	
	public void deleteTask(QueueTask task) {
		if (task != null) {
			this.finished.remove(task);
			Utility.writeToDebug("DeleteTask: " + task.id);
			try {
				if(task.getState() == QueueTask.State.PermissionRequired){
					task.setState(QueueTask.State.deleted);
				}
				else{
					deleteTaskRecord(task);
				}
			} catch (HibernateException e) {
				Utility.writeToDebug(e);
			} catch (ClassNotFoundException e) {
				Utility.writeToDebug(e);
			} catch (SQLException e) {
				Utility.writeToDebug(e);
			} catch (Exception e) {
				Utility.writeToDebug(e);
			}
		}
		resetFlagSet();
	}
}
