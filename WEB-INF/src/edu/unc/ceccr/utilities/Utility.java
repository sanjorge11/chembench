package edu.unc.ceccr.utilities;

import java.security.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.math.*;
import java.util.Random;
import java.io.*;

import edu.unc.ceccr.global.Constants;
import edu.unc.ceccr.persistence.AdminSettings;
import edu.unc.ceccr.persistence.DataSet;
import edu.unc.ceccr.persistence.HibernateUtil;
import edu.unc.ceccr.persistence.JobStats;
import edu.unc.ceccr.persistence.SoftwareExpiration;
import edu.unc.ceccr.persistence.User;
import edu.unc.ceccr.taskObjects.QsarModelingTask;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Expression;

//The Utility class is for cross-cutting concerns (logging, authentication / user stuff).

public class Utility {
	private static Integer debug_counter = 0;

	public Utility() {
	};

	public static byte[] encrypt(String str) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(str.getBytes());
		byte[] encryptedStr = md.digest();
		//convert each byte to a readable ascii character for easy database access. 
		//Values need to be inside the range [40..126].
		for(int i = 0; i < encryptedStr.length; i++){
			//writeToDebug("before: " + (Math.abs(new Integer(encryptedStr[i]))) + " after: " + (Math.abs(new Integer(encryptedStr[i]) % 87) + 40) + " afterchar: " + (char)(Math.abs(new Integer(encryptedStr[i]) % 87) + 40)  );
			encryptedStr[i] = (byte) (Math.abs(new Integer(encryptedStr[i]) % 87) + 40);
		}
		//writeToDebug("Encoding password: " + str  + " to " + new String(encryptedStr));
		return encryptedStr;
	}

	public static boolean compareEncryption(byte[] byte1, byte[] byte2) {
		if (new BigInteger(1, byte1).toString(16).equalsIgnoreCase(
				new BigInteger(1, byte2).toString(16))) {
			return true;
		} else {
			return false;
		}
	}

	public static String randomPassword() throws Exception {
		String source = Constants.SOURCE;
		String password = "";
		Random generator = new Random();
		char[] sourceStr = source.toCharArray();
		for (int i = 0; i < 8; i++) {
			password = password + sourceStr[generator.nextInt(62)];
		}
		return password;
	}

	public static String getDate(){
	    Date dateNow = new Date ();
	    
        SimpleDateFormat df = new SimpleDateFormat("dd-HH:mm:ss");
        StringBuilder s = new StringBuilder( df.format( dateNow ) );
 
		return s.toString();
	}
	
	public static void writeToDebug(String s, String userName, String jobName) {
		//Debug output write function. Used throughout Java code.
		try {
			// Append to current-job file. For ease of use, really.
			FileWriter fstream = new FileWriter(
					Constants.CECCR_BASE_PATH + "/workflow-users/javadebug.log", true);
			BufferedWriter out = new BufferedWriter(fstream);

			out.write(debug_counter.toString() + " " + userName + " " + jobName
					+ " " + s + " [" + getDate() + "]" + "\n");
			out.close();
		} catch (Exception e) {
			//ohnoes!
		}

		try {
			// Append to file
			FileWriter fstream = new FileWriter(
					Constants.CECCR_BASE_PATH + "/workflow-users/debug/" + userName
							+ "-" + jobName + ".log", true);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(debug_counter.toString() + " " + s + " [" + getDate() + "]" + "\n");
			out.close();
		} catch (Exception e) {
			//whatever
		}
		debug_counter++;
	}
	
	public static void writeToDebug(String s) {
		//Debug output write function. Used throughout Java code.
		try {
			// Append to current-job file. For ease of use, really.
			FileWriter fstream = new FileWriter(
					Constants.CECCR_BASE_PATH + "/workflow-users/javadebug.log", true);
			BufferedWriter out = new BufferedWriter(fstream);

			out.write(debug_counter.toString() + " " + s + " [" + getDate() + "]" + "\n");
			out.close();
		} catch (Exception e) {
		}
		debug_counter++;
	}

	public static void writeToUsageLog(String s, String username) {
		//Usage output write function. Used throughout Java code.
		try {
			FileWriter fstream = new FileWriter(
					Constants.CECCR_BASE_PATH + "/workflow-users/usage.log", true);
			BufferedWriter out = new BufferedWriter(fstream);

			out.write(username + ": " + s + " [" + getDate() + "]" + "\n");
			out.close();
		} catch (Exception e) {
			//oh well
		}
	}

	
	public static void writeToDebug(Exception ex) {
		try {
			// Create file
			FileWriter fstream = new FileWriter(
					Constants.CECCR_BASE_PATH+"/workflow-users/javadebug.log", true);
			String s;
			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);
			ex.printStackTrace(printWriter);
			s = result.toString();
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(s +  " [" + getDate() + "]");
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
		}
		/*
		NOTE: If you're getting (Unknown source) instead of line numbers in your output
		make sure, in your build.xml, on the javac line, you have set
		debug="true" 
		Your life will get so much easier. javac -g will do this too.
		*/
	}
	
	public static void writeToStrutsDebug(String s){
		//Debug output write function. Used in Struts code.
		try {
			// Append to current-job file. For ease of use, really.
			FileWriter fstream = new FileWriter(
					Constants.CECCR_BASE_PATH + "/workflow-users/strutsdebug.log", true);
			BufferedWriter out = new BufferedWriter(fstream);

			out.write(debug_counter.toString() + " " + s + " [" + getDate() + "]" + "\n");
			out.close();
		} catch (Exception e) {
		}
		debug_counter++;
	}
	
	public static void writeToStrutsDebug(Exception ex) {
		try {
			// Create file
			FileWriter fstream = new FileWriter(
					Constants.CECCR_BASE_PATH+"/workflow-users/strutsdebug.log", true);
			String s;
			final Writer result = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(result);
			ex.printStackTrace(printWriter);
			s = result.toString();
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(s +  " [" + getDate() + "]");
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
		}
	}
	
	public static void close(Closeable c) {
		if (c != null) {
		  try {
		    c.close();
		  } catch (IOException e) {
			  // ignored
	      }
	    }
	 }
	
	public static void writeProgramLogfile(String workingDir, String programName, InputStream stdout, InputStream errout){
		//Take the inputStreams from an external program execution, write them to a logfile
		    
		try{
		  File file=new File(workingDir + "/Logs/");
		  if(!file.exists()){
		  	file.mkdirs();
		  }

		  FileOutputStream logFileOS = new FileOutputStream(new File(workingDir + "/Logs/" + programName + ".log"));
		  FileOutputStream errFileOS = new FileOutputStream(new File(workingDir + "/Logs/" + programName + ".err"));
		  StreamGobbler outputGobbler = new StreamGobbler(stdout, "stdout", logFileOS);  
		  StreamGobbler errorGobbler = new StreamGobbler(errout, "errout", errFileOS);   
		  outputGobbler.start();
		  errorGobbler.start();
		  //logFileOS.close();
		  //errFileOS.close();
		}
		catch(Exception ex){
	      writeToDebug(ex);
		}
	}

	public static boolean isAdmin(String userName){
		boolean user_is_admin = false;
		Iterator it=Constants.ADMIN_LIST.iterator();
		while(it.hasNext())
		{
			if(userName.equals((String)it.next())){
				user_is_admin = true;
			}
		}
		
		return user_is_admin;
	}
	
	public static boolean canDownloadDescriptors(String userName){
		boolean user_can_download = false;
		Iterator it=Constants.DESCRIPTOR_DOWNLOAD_USERS_LIST.iterator();
		while(it.hasNext())
		{
			if(userName.equals((String)it.next())){
				user_can_download = true;
			}
		}
		
		return user_can_download;
	}

	public int getCounter() throws FileNotFoundException, IOException {
		return readCounter();
	}

	public synchronized void increaseCounter() throws IOException {
		int counter = readCounter();
		writeCounter(counter + 1);
	}

	public int readCounter() throws FileNotFoundException, IOException {
		int counter = 0;
		File counterFile = new File(Constants.CECCR_USER_BASE_PATH
				+ "counter.txt");
		if (counterFile.exists()) {
			FileInputStream fin = new FileInputStream(counterFile);
			DataInputStream in = new DataInputStream(fin);
			counter = in.readInt();
			return counter;
		} else {
			return counter;
		}
	}
	
	public void writeCounter(int counter) throws IOException {
		File counterFile = new File(Constants.CECCR_USER_BASE_PATH
				+ "counter.txt");
		if (!counterFile.exists()) {
			counterFile.createNewFile();
		}
		DataOutputStream out = new DataOutputStream(new FileOutputStream(
				counterFile));
		out.writeChars(Integer.toString(counter));
		out.close();
	}

	public String getJobStats() throws FileNotFoundException, IOException {
		//get info from database
		
		int numUsers = 0;
		int computeHours = 0;
		int numJobs = 0;
		
		try {
			Session s = HibernateUtil.getSession();
			List<JobStats> jobStatList = PopulateDataObjects.getJobStats(s);
			List<User> users = PopulateDataObjects.getUsers(s);
			
			numJobs = jobStatList.size();
			numUsers = users.size();
			
			for(JobStats js: jobStatList){
				if(js.getTimeFinished() != null){
					long timeDiff = js.getTimeFinished().getTime() - js.getTimeCreated().getTime();	
					int timeDiffInHours = Math.round(timeDiff / 1000 / 60 / 60);
					computeHours += timeDiffInHours;
				}
			}
			
		} catch (Exception e) {
			//don't sweat it - it's just a counter, not worth killing the page for if it fails
		}
		
		String jobstats = "";
		if(numJobs > 0){
			jobstats = "Chembench has " + numUsers + " registered users. Since April 2010, " + numJobs + " jobs have been submitted, totaling " + computeHours + " hours of compute time.";
		}
		
		return jobstats;
	}

	public static Long checkExpiration(int year, int month, int day) {
		Calendar cal1 = Calendar.getInstance();
		cal1.set(year, month - 1, day);
		Calendar cal2 = Calendar.getInstance();
		Date end = cal1.getTime();
		Date today = cal2.getTime();

		return (end.getTime() - today.getTime()) / (1000 * 60 * 60 * 24);
	}

	public static void setAdminConfiguration(String path) throws IOException {
		ParseConfigurationXML.initializeConstants(path);
		
		try{
			BufferedReader dis = new BufferedReader(new FileReader(new File(Constants.BUILD_DATE_FILE_PATH))); 
			Constants.BUILD_DATE = dis.readLine().replace("#", ""); 
		}catch(Exception ex){
			writeToDebug(ex);
		}
		
		//get max models, max compounds, and user acceptance mode from database
		//if data doesn't exist in DB yet, set to some arbitrary defaults
		//these numbers will be overwritten if there's anything in the DB.
		Constants.ACCEPTANCE = "automatic";
		Constants.MAXCOMPOUNDS = 100;
		Constants.MAXMODELS = 10000;
		
		List<AdminSettings> ls = new LinkedList<AdminSettings>();
		try{
			Session s = HibernateUtil.getSession();
			Transaction tx = s.beginTransaction();
			ls.addAll(s.createCriteria(AdminSettings.class).add(Expression.eq("type", "userAcceptanceMode")).list());
			ls.addAll(s.createCriteria(AdminSettings.class).add(Expression.eq("type", "maxCompounds")).list());
			ls.addAll(s.createCriteria(AdminSettings.class).add(Expression.eq("type", "maxModels")).list());
			tx.commit();
		}catch(Exception ex){
			Utility.writeToDebug(ex);
		}
		Iterator<AdminSettings> iter = ls.iterator();
		AdminSettings as = null;
		if(iter.hasNext()){
			as = iter.next();
		}
		while(as != null){
			if(as.getType().equalsIgnoreCase("userAcceptanceMode")){
				Constants.ACCEPTANCE = as.getValue();
			}
			else if(as.getType().equalsIgnoreCase("maxCompounds")){
				Constants.MAXCOMPOUNDS = Integer.parseInt(as.getValue());
			}
			else if(as.getType().equalsIgnoreCase("maxModels")){
				Constants.MAXMODELS = Integer.parseInt(as.getValue());
			}
			
			if(iter.hasNext()){
				as = iter.next();			
			}
			else{
				as = null;
			}
		}
		
	}

	public static int getSignificantFigures(String number, boolean removeTrailingZeros){

		int sigfigs = 0;
		
		//Remove negative sign.
		if(number.charAt(0) == '-'){
			number = number.substring(1);
		}
		
		//remove any leading zeros from the number
		while(number.charAt(0) == '0'){
			number = number.substring(1);
		}
		//remove any trailing zeros from the number
		if(removeTrailingZeros){
			while(number.charAt(number.length() - 1) == '0'){
				number = number.substring(0, number.length() - 1);
			}
		}
		//find decimal place in number
		int decPointPos = number.lastIndexOf(".");
		System.out.println("dec point found at: " + decPointPos);

		for(int i = 0; i < decPointPos; i++){
			number = number.substring(1);
			sigfigs++;
		}
		if(number.charAt(0) == '.'){
			number = number.substring(1);
		}
		while(number.length() > 0){
			number = number.substring(1);
			sigfigs++;
		}
		return sigfigs;
	}

	public static String StringArrayListToString(ArrayList<String> stringArrayList){
		String ret = "";
		int size = stringArrayList.size();
		for(int i = 0; i < size; i++){
			String s = stringArrayList.get(i);
			ret += s;
			if(i < size - 1){
				ret += " ";
			}
		}
		return ret;
	}
	
	public static String roundSignificantFigures(String number, int numFigs){
		//outputs a numerical string 
		//e.g., 12345 to 2 significant figures is 12000, not 1.2*10^4
		//although the latter is more correct, the former is more intuitive.
		boolean debug = false;
		
		if(debug)
			Utility.writeToDebug("Rounding " + number + " to " + numFigs + " significant figures.");
		
		if(number.length() < numFigs){
			return number;
		}
		
		//check if number is negative. Remove and remember.
		boolean isNegative = false;
		if(number.charAt(0) == '-'){
			isNegative = true;
			number = number.substring(1);
		}
		//remove any leading zeros from the number
		while(number.charAt(0) == '0' && number.length() > 0){
			number = number.substring(1);
		}

		if(debug)
			System.out.println("number is " + number);

		int order = (int) Math.floor(Math.log10(Double.parseDouble(number)));

		if(debug)
			System.out.println("Number is order " + order);
		//find decimal place in number
		int decPointPos = number.lastIndexOf(".");
		//we want to remove the decimal point, to make things easier
		if(decPointPos == 0){
			number = number.replaceFirst("\\.", "0");
		}
		else{
			number = number.replaceFirst("\\.", "");
		}
		
		if(numFigs > number.length()){
			numFigs = number.length();
		}
		
		//next we want to round off the insignificant digits
		String significant = number.substring(0, numFigs);
		String insignificant = number.substring(numFigs);
		String forRounding = significant + "." + insignificant;
		int roundedSignificant = (int) Math.round(Double.parseDouble(forRounding));

		if(debug)
			System.out.println("chopped number down to " + roundedSignificant);
		String roundedSignificantStr = "" + roundedSignificant;
		String outputStr = "";
		//restore number to its original order
		int currentOrder = (int) Math.floor(Math.log10(roundedSignificant));
		if(debug)
			System.out.println("order was " + order + " and is now " + currentOrder);

		if(currentOrder > order){
			//we need to make this a decimal.
			//number was sth like 1.20 and now it's 12
			if(order >= 0){
				for(int i = 0; i <= order; i++){
					outputStr += roundedSignificantStr.charAt(0);
					roundedSignificantStr = roundedSignificantStr.substring(1);
				}
				outputStr += ".";
				while(! roundedSignificantStr.equals("")){
					outputStr += roundedSignificantStr.charAt(0);
					roundedSignificantStr = roundedSignificantStr.substring(1);
				}
			}
			else{
				outputStr = "0.";
				for(int i = 0; i < ((int) Math.abs(order) - 1); i++){
					outputStr += "0";
				}
				while(! roundedSignificantStr.equals("")){
					outputStr += roundedSignificantStr.charAt(0);
					roundedSignificantStr = roundedSignificantStr.substring(1);
				}
			}
		}
		else{
			//number was sth like 123456 and now it's 12
			for(int i = 0; i < roundedSignificantStr.length(); i++){
				outputStr += roundedSignificantStr.charAt(i);
			}
			while(outputStr.length() <= order){
				outputStr += "0";
			}
		}

		if(debug)
			System.out.println("restored number to " + outputStr);
		
		if(isNegative){
			outputStr = "-" + outputStr;
		}
		
		return outputStr;

	}
	
	public static int naturalSortCompare(Object o1, Object o2) {
        String firstString = o1.toString();
        String secondString = o2.toString();
 
        if (secondString == null || firstString == null) {
            return 0;
        }
 
        int lengthFirstStr = firstString.length();
        int lengthSecondStr = secondString.length();
 
        int index1 = 0;
        int index2 = 0;
 
        while (index1 < lengthFirstStr && index2 < lengthSecondStr) {
            char ch1 = firstString.charAt(index1);
            char ch2 = secondString.charAt(index2);
 
            char[] space1 = new char[lengthFirstStr];
            char[] space2 = new char[lengthSecondStr];
 
            int loc1 = 0;
            int loc2 = 0;
 
            do {
                space1[loc1++] = ch1;
                index1++;
 
                if (index1 < lengthFirstStr) {
                    ch1 = firstString.charAt(index1);
                } else {
                    break;
                }
            } while (Character.isDigit(ch1) == Character.isDigit(space1[0]));
 
            do {
                space2[loc2++] = ch2;
                index2++;
 
                if (index2 < lengthSecondStr) {
                    ch2 = secondString.charAt(index2);
                } else {
                    break;
                }
            } while (Character.isDigit(ch2) == Character.isDigit(space2[0]));
 
            String str1 = new String(space1);
            String str2 = new String(space2);
 
            int result;
 
            if (Character.isDigit(space1[0]) && Character.isDigit(space2[0])) {
                Integer firstNumberToCompare = new Integer(Integer
                        .parseInt(str1.trim()));
                Integer secondNumberToCompare = new Integer(Integer
                        .parseInt(str2.trim()));
                result = firstNumberToCompare.compareTo(secondNumberToCompare);
            } else {
                result = str1.compareTo(str2);
            }
 
            if (result != 0) {
                return result;
            }
        }
        return lengthFirstStr - lengthSecondStr;
    }
 
		
}