package edu.unc.ceccr.chembench.global;

import edu.unc.ceccr.chembench.jobs.CentralDogma;
import edu.unc.ceccr.chembench.utilities.ParseConfigurationXML;
import org.apache.log4j.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.nio.file.Paths;
import java.util.Set;

public class ChembenchServletContextListener implements ServletContextListener {
    private static Logger logger = Logger.getLogger(ChembenchServletContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        // read $CHEMBENCH_HOME, then append config directory / filename
        String ENV_CHEMBENCH_HOME;
        try {
            ENV_CHEMBENCH_HOME = System.getenv("CHEMBENCH_HOME");
            if (ENV_CHEMBENCH_HOME == null) {
                throw new RuntimeException("Required environment variable $CHEMBENCH_HOME is not set");
            }
        } catch (SecurityException e) {
            throw new RuntimeException("Couldn't read $CHEMBENCH_HOME environment variable", e);
        }

        String configFilePath = Paths.get(ENV_CHEMBENCH_HOME, "config", "systemConfig.xml").toString();
        ParseConfigurationXML.initializeConstants(configFilePath);

        // start up the job queues
        CentralDogma.getInstance();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        Set<Thread> jobThreads = CentralDogma.getInstance().getThreads();
        for (Thread t : jobThreads) {
            t.interrupt();
        }
    }
}
