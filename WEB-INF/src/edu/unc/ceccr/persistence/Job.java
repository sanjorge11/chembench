package edu.unc.ceccr.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import edu.unc.ceccr.persistence.Queue.QueueTask.State;
import edu.unc.ceccr.persistence.Queue.QueueTask.jobTypes;
import edu.unc.ceccr.taskObjects.WorkflowTask;

@Entity()
@Table(name = "cbench_job")
public class Job {
	
	public Long id = null;
	private String userName;
	private String jobName; 
	
	private String status; //basic status. {queued, pending, started, finished, permissionRequired, deleted}
	private String message; //a more detailed status message
	private String jobList; //which jobList it's in. {NONE, INCOMING, LSF, LOCAL}
	
	private String jobType; //can be one of {MODELING, PREDICTION, DATASET};
	private String lookupId; //a primary key for the datasetJob, modelingJob, or predictionJob tables (as determined by jobType)
	private int numCompounds;
	private int numModels;
	
	private Date timeCreated;
	private Date timeStarted;
	private Date timeStartedByLsf; //jobs may remain pending in LSF for a long time before actually started.
	private Date timeFinished;
	
	private String timeFinishedEstimate;
	
	public WorkflowTask workflowTask; //contains one modelingTask, predictionTask, or datasetTask.
	
	@Column(name = "id")
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "userName")
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}

	@Column(name = "jobName")
	public String getJobName() {
		return jobName;
	}
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	@Column(name = "status")
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "message")
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	@Column(name = "jobType")
	public String getJobType() {
		return jobType;
	}
	public void setJobType(String jobType) {
		this.jobType = jobType;
	}
	
	@Column(name = "jobList")
	public String getJobList() {
		return jobList;
	}
	public void setJobList(String jobList) {
		this.jobList = jobList;
	}

	@Column(name = "lookupId")
	public String getLookupId() {
		return lookupId;
	}
	public void setLookupId(String lookupId) {
		this.lookupId = lookupId;
	}

	@Column(name = "numCompounds")
	public int getNumCompounds() {
		return numCompounds;
	}
	public void setNumCompounds(int numCompounds) {
		this.numCompounds = numCompounds;
	}

	@Column(name = "numModels")
	public int getNumModels() {
		return numModels;
	}
	public void setNumModels(int numModels) {
		this.numModels = numModels;
	}

	@Column(name = "timeCreated")
	public Date getTimeCreated() {
		return timeCreated;
	}
	public void setTimeCreated(Date timeCreated) {
		this.timeCreated = timeCreated;
	}

	@Column(name = "timeStarted")
	public Date getTimeStarted() {
		return timeStarted;
	}
	public void setTimeStarted(Date timeStarted) {
		this.timeStarted = timeStarted;
	}

	@Column(name = "timeStartedByLsf")
	public Date getTimeStartedByLsf() {
		return timeStartedByLsf;
	}
	public void setTimeStartedByLsf(Date timeStartedByLsf) {
		this.timeStartedByLsf = timeStartedByLsf;
	}

	@Column(name = "timeFinished")
	public Date getTimeFinished() {
		return timeFinished;
	}
	public void setTimeFinished(Date timeFinished) {
		this.timeFinished = timeFinished;
	}

	public String getTimeFinishedEstimate() {
		return timeFinishedEstimate;
	}
	public void setTimeFinishedEstimate(String timeFinishedEstimate) {
		this.timeFinishedEstimate = timeFinishedEstimate;
	}
	
	public WorkflowTask getWorkflowTask() {
		return workflowTask;
	}
	public void setWorkflowTask(WorkflowTask workflowTask) {
		this.workflowTask = workflowTask;
	}
	
}