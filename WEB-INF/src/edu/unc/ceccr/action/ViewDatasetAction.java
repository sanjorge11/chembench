package edu.unc.ceccr.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import edu.unc.ceccr.global.Constants;
import edu.unc.ceccr.persistence.DataSet;
import edu.unc.ceccr.persistence.User;
import edu.unc.ceccr.utilities.PopulateDataObjects;
import edu.unc.ceccr.utilities.Utility;


public class ViewDatasetAction extends Action {

	ActionForward forward;
	

	
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) {

		forward = mapping.findForward("success");

		
		HttpSession session = request.getSession(false);
		
		try {

		
				String user = ((User) session.getAttribute("user"))
						.getUserName();
				String fileName = null;
				boolean isPublic = false;
				if(request==null && (String)session.getAttribute("fileName")=="")
					return mapping.findForward("failure");
				
				else if(request.getParameter("fileName")==null){
					Utility.writeToMSDebug("..................."+(String) session.getAttribute("fileName"));
					fileName = (String) session.getAttribute("fileName");
					isPublic = ((String)session.getAttribute("isPublic")).equals("true");
					}
					else{
						Utility.writeToMSDebug("...............**...."+request.getParameter("fileName"));
						fileName = request.getParameter("fileName");
						isPublic = ((String)request.getParameter("isPublic")).equals("true");
					}
			DataSet ds = PopulateDataObjects.getDataSetByName(fileName, isPublic?Constants.ALL_USERS_USERNAME:user);
							
			session.setAttribute("ds", ds);
			Utility.writeToMSDebug("ViewDatasetAction:::"+"descriptorMatrixServlet?user="+ds.getUserName()+"&project="+ds.getFileName()+"&name="+ds.getActFile());
			session.setAttribute("actFile", "descriptorMatrixServlet?user="+(isPublic?"all-users":ds.getUserName())+"&project="+ds.getFileName()+"&name="+ds.getActFile());
			session.setAttribute("viz_path", "descriptorMatrixServlet?user="+(isPublic?"all-users":ds.getUserName())+"&project="+ds.getFileName()+"/Visualization&name="+ds.getSdfFile().replace(".sdf", ""));
			session.setAttribute("isPublic",isPublic);
			

		} catch (Exception e) {
			Utility.writeToDebug(e);
		}

		return forward;

	}

}
