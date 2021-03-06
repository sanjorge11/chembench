package edu.unc.ceccr.chembench.utilities;

import edu.unc.ceccr.chembench.actions.DeleteAction;
import edu.unc.ceccr.chembench.persistence.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class ActiveUser implements HttpSessionListener {

    private static final Logger logger = LoggerFactory.getLogger(ActiveUser.class);

    private static int activeSessions = 0;

    public static String getActiveSessions() {
        return "" + activeSessions;
    }

    public void sessionCreated(HttpSessionEvent se) {
        activeSessions++;
    }

    public void sessionDestroyed(HttpSessionEvent se) {

        if (activeSessions > 0 && se != null) {
            activeSessions--;
            if (se.getSession() != null && se.getSession().getAttribute("userType") != null) {
                User user = (User) se.getSession().getAttribute("user");
                String type = (String) se.getSession().getAttribute("userType");
                if (user != null && user.getUserName() != null && user.getUserName().contains("guest") && type != null
                        && type.equals("guest")) {
                    try {
                        (new DeleteAction()).deleteUser(user.getUserName());
                    } catch (Exception e) {
                        logger.warn("Failed to delete guest user: " + user.getUserName(), e);
                    }
                    logger.debug("GUEST USER DELETED on SESSION TIMEOUT:" + user.getUserName());
                }
            }
        }
    }

}
