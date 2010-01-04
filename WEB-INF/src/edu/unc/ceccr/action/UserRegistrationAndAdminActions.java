package edu.unc.ceccr.action;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

//struts2
import com.opensymphony.xwork2.ActionSupport; 
import com.opensymphony.xwork2.ActionContext; 

import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaFactory;
import net.tanesha.recaptcha.ReCaptchaResponse;

import org.apache.struts2.interceptor.SessionAware;
import org.hibernate.Session;
import org.hibernate.Transaction;

import edu.unc.ceccr.global.Constants;
import edu.unc.ceccr.persistence.DataSet;
import edu.unc.ceccr.persistence.HibernateUtil;
import edu.unc.ceccr.persistence.PredictionValue;
import edu.unc.ceccr.persistence.Predictor;
import edu.unc.ceccr.persistence.Queue;
import edu.unc.ceccr.persistence.User;
import edu.unc.ceccr.taskObjects.QsarModelingTask;
import edu.unc.ceccr.taskObjects.QsarPredictionTask;
import edu.unc.ceccr.utilities.FileAndDirOperations;
import edu.unc.ceccr.utilities.PopulateDataObjects;
import edu.unc.ceccr.utilities.SendEmails;
import edu.unc.ceccr.utilities.Utility;
import edu.unc.ceccr.workflows.SmilesPredictionWorkflow;


public class UserRegistrationAndAdminActions extends ActionSupport{

	/* USER FUNCTIONS */
	
	public String RegisterUser() throws Exception{
		String result = SUCCESS;
				
		//form validation
		
			errorText = "";
		
			//check if CAPTCHA was passed
			ReCaptcha captcha = ReCaptchaFactory.newReCaptcha(Constants.RECAPTCHA_PUBLICKEY,Constants.RECAPTCHA_PRIVATEKEY, false);
	        ReCaptchaResponse resp = captcha.checkAnswer(request.getRemoteAddr(), request.getParameter("recaptcha_challenge_field"), request.getParameter("recaptcha_response_field"));
	
	        if (!resp.isValid()) {
	        	errorText += "The text you typed for the CAPTCHA test did not match the picture. Try again. <br />";
	        	result = ERROR;
	        }
	        //CAPTCHA passed. Validate first name.
			if(!IsValid(firstName)){
				errorText += "This is not a valid first name : <font color=red><u>"+registerForm.getFirstName()+"</u></font>";
				result = ERROR;
			}
	    	//Validate last name.
			if(!IsValid(lastName)){
				errorText += "This is not a valid last name : <font color=red><u>"+registerForm.getFirstName()+"</u></font>";
				result = ERROR;
			}
			//Check whether the username already exists 
			//(queries database)
			if(UserExists(userName)){
				errorText += "The user name <font color=red><u>"+registerForm.getUserName()+"</u></font>"+" already in use.";
				result = ERROR;
			}
			if(result.equals(ERROR)){
				return result;
			}
				
		//make user
			user = new User();

			user.setUserName(userName);
			user.setEmail(email);
			user.setFirstName(firstName);
			user.setLastName(lastName);
			
			user.setOrgName(organizationName);
			user.setOrgType(organizationType);
			user.setOrgPosition(organizationPosition);
			user.setPhone(phoneNumber);

			//optional fields
			user.setAddress(address);
			user.setState(stateOrProvince);
			user.setCity(city);
			user.setCountry(country);
			user.setZipCode(zipCode);
			user.setWorkbench(workBench); //deprecated, but some people think it's still important
			
			user.setStatus("NOTSET");
	
			String setting = Constants.ACCEPTANCE;
			String password=Utility.randomPassword();
			
			Session s = HibernateUtil.getSession();
			Transaction tx = null;
		
		//commit user to DB
			
			try {
				tx = s.beginTransaction();
				s.saveOrUpdate(user);
				tx.commit();
			} catch (RuntimeException e) {
				if (tx != null)
					tx.rollback();
				Utility.writeToDebug(e);
			} finally {s.close();}
			
		//send user an email
			
			String errormessage = "error"; //used in case emailing their password to them won't work (e.g. mail server dead)
		    try{
		    	  String setting=Constants.ACCEPTANCE;
		    	  if(setting.contains("manual"))
		    	  {
		    		 errormessage = "A C-Chembench administrator will process your user request. " +
						"If you are approved, you will be given a password to log in.";
		    		 
		    			String HtmlBody="(This message is automatically generated by the C-Chembench system.  Please do NOT reply.) "
		    			+"<br/><br/><br/>A new user, <b>" + user.getUserName() + "</b>, has requested a login for C-Chembench."
		    			+"<br/> <br/>To view this user's user, please log on to C-Chembench."
		    			+"<br/><br/> Thank you"
		    			+"<br/><br/><br/>"
		    			+ new Date();
		    		 
		    		 SendEmails.sendEmailToAdmins("New user registration", HtmlBody);
		    		 
		    	  }else{ 
		      		user.setStatus("agree");
		    		user.setPassword(Utility.encrypt(password));
		    		s = HibernateUtil.getSession();
		    		tx = null;
		    	  }
    	    }catch(Exception ex){
	      	  Utility.writeToDebug("Failed to send email for user registration: " + user.getUserName());
	      	  Utility.writeToDebug(ex);
	      	  Utility.writeToDebug("Error message: " + errormessage);
	      	  return ERROR;
	        }
			
	  		errormessage = "Thank you for you interest in CECCR's C-Chembench. <br/>Your account has been approved.<br/>"
	  			+"<br/> Your user name : <font color=red>"+ user.getUserName() + "</font>"
	  			+"<br/> Your temporary password : <font color=red>" + password + "</font>" 
	  			+"<br/> Please note that passwords are case sensitive. "
	  			+"<br/> In order to change your password,  log in to C-Chembench and click the 'edit profile' link at the upper right."
	  			+"<br/>"
	  			+"<br/>We hope that you find C-Chembench to be a useful tool. <br/>If you have any problems or suggestions for improvements, please contact us at : "+Constants.WEBSITEEMAIL
	  			+"<br/><br/>Thank you. <br/>The C-Chembench Team<br/>";
	
	  		Utility.writeToDebug("In case email failed: password for user: " + user.getUserName() + " is: " + password);
	  		
	  		try {
	  			SendEmails.sendEmail(user.getEmail(), "", "", "Chembench User Registration", errormessage);
	  		}catch(Exception ex){
	      	  Utility.writeToDebug("Failed to send email for user registration: " + user.getUserName());
	      	  Utility.writeToDebug(ex);
	      	  Utility.writeToDebug("Error message: " + errormessage);
	      	  return ERROR;
	        }
	  		
		return result;
	}
	
	public String ChangePassword() throws Exception{
		String result = SUCCESS;
		
		//check that the user is logged in
		ActionContext context = ActionContext.getContext();
		user = getLoggedInUser(context);
		if(user == null){
			return LOGIN;
		}
		
		// Change user object to have new password
		
		
		// Commit changes
		
	}
	
	public String EditUserInformation() throws Exception{
		String result = SUCCESS;
		
		//check that the user is logged in
		ActionContext context = ActionContext.getContext();
		user = getLoggedInUser(context);
		if(user == null){
			return LOGIN;
		}
		
		// Change user object according to edited fields
		
		
		// Commit changes
		
		
		return result;
	}
	
	/* USER FUNCTIONS */
	
	/* ADMIN-ONLY FUNCTIONS */
	
	public String ChangeModelingLimits() throws Exception{
		String result = SUCCESS;
		
		//check that the user is logged in and is an admin
		ActionContext context = ActionContext.getContext();
		user = getLoggedInUser(context);
		if(user == null){
			return LOGIN;
		}
		if(! Utility.isAdmin(user.getUserName())){
			return LOGIN;
		}
		
		
		
		return result;
	}
	
	public String UpdateSoftwareExpiration() throws Exception{
		String result = SUCCESS;
		
		//check that the user is logged in and is an admin
		ActionContext context = ActionContext.getContext();
		user = getLoggedInUser(context);
		if(user == null){
			return LOGIN;
		}
		if(! Utility.isAdmin(user.getUserName())){
			return LOGIN;
		}
		
		
		
		return result;
	}
	
	public String DenyJob() throws Exception{
		String result = SUCCESS;
		
		//check that the user is logged in and is an admin
		ActionContext context = ActionContext.getContext();
		user = getLoggedInUser(context);
		if(user == null){
			return LOGIN;
		}
		if(! Utility.isAdmin(user.getUserName())){
			return LOGIN;
		}
		
		
		
		return result;
	}
	
	public String PermitJob() throws Exception{
		String result = SUCCESS;
		
		//check that the user is logged in and is an admin
		ActionContext context = ActionContext.getContext();
		user = getLoggedInUser(context);
		if(user == null){
			return LOGIN;
		}
		if(! Utility.isAdmin(user.getUserName())){
			return LOGIN;
		}
		
		
		
		return result;
	}
	
	/* END ADMIN-ONLY FUNCTIONS */
	
	/* HELPER FUNCTIONS */
	
	private User getLoggedInUser(ActionContext context){
		if(context == null){
			Utility.writeToStrutsDebug("No ActionContext available");
			return null;
		}
		else{
			user = (User) context.getSession().get("user");
			return user;
		}
	}
	
	/* END HELPER FUNCTIONS */
		
	
	/* DATA OBJECTS, GETTERS, AND SETTERS */
	
	private User user;

	/* Variables used for user registration and updates */
	private String errorText;
	
	private String userName;
	private String address;
	private String city;
	private String country;
	private String email;
	private String firstName;
	private String lastName;
	private String organizationName;
	private String organizationType;
	private String organizationPosition;
	private String phoneNumber;
	private String stateOrProvince;
	private String zipCode;
	private String workBench; //deprecated, but some people think it's still important
	/* End Variables used for user registration and updates */
	
	
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	
	/* Variables used for user registration and updates */
	
	public String getErrorText() {
		return errorText;
	}
	public void setErrorText(String errorText) {
		this.errorText = errorText;
	}

	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}

	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}

	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getOrganizationName() {
		return organizationName;
	}
	public void setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
	}

	public String getOrganizationType() {
		return organizationType;
	}
	public void setOrganizationType(String organizationType) {
		this.organizationType = organizationType;
	}

	public String getOrganizationPosition() {
		return organizationPosition;
	}
	public void setOrganizationPosition(String organizationPosition) {
		this.organizationPosition = organizationPosition;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getStateOrProvince() {
		return stateOrProvince;
	}
	public void setStateOrProvince(String stateOrProvince) {
		this.stateOrProvince = stateOrProvince;
	}

	public String getZipCode() {
		return zipCode;
	}
	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public String getWorkBench() {
		return workBench;
	}
	public void setWorkBench(String workBench) {
		this.workBench = workBench;
	}
	
	/* End Variables used for user registration and updates */
	
	/* END DATA OBJECTS, GETTERS, AND SETTERS */
}	