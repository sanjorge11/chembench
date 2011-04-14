package edu.unc.ceccr.utilities;

import edu.unc.ceccr.global.Constants;

//Holds functions needed for managing LSF jobs.
//Moving files back and forth to largefs, checking status of LSF-submitted jobs, etc.

public class LsfOperations{

	public static boolean patronsQueueHasRoom(){
		try{
			RunExternalProgram.runCommand("numPatronsJobs.sh", Constants.CECCR_USER_BASE_PATH);
			String numJobsStr = FileAndDirOperations.readFileIntoString(Constants.CECCR_USER_BASE_PATH + "numPatronsJobs.txt");
			numJobsStr.replace("\n", "");
			if(Integer.parseInt(numJobsStr) >= 1){
				return true;
			}
		}
		catch(Exception ex){
			Utility.writeToDebug(ex);
			return false;
		}
		return false;
	}
}