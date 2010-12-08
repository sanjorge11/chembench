package edu.unc.ceccr.action;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Expression;

import edu.unc.ceccr.global.Constants;
import edu.unc.ceccr.persistence.HibernateUtil;
import edu.unc.ceccr.persistence.User;
import edu.unc.ceccr.utilities.Utility;
import edu.unc.ceccr.utilities.PopulateDataObjects;

public class CheckSessionAction extends Action 
{
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)	throws Exception
	{
		
		ActionForward forward = new ActionForward();
		
		if(!Constants.isCustomized)
		{
			String path=getServlet().getServletContext().getRealPath("WEB-INF/systemConfig.xml");
			Utility.setAdminConfiguration(path);
		}
			return forward=mapping.findForward("login");
		}
		
        Cookie[] cookies=request.getCookies();
        
        Cookie cookie;       
        
        for(int i=0;i<cookies.length;i++)
        {
        	cookie=cookies[i];
        	
        	if(cookie.getName().equalsIgnoreCase("login")&&cookie.getValue().equalsIgnoreCase("true"))
        	{
        		forward=mapping.findForward("loggedin");
        		
            	return forward;
            	}
            }
        forward = mapping.findForward("login");
        
        return forward;
        
        }
	}

