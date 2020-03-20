package Servlets;

import Utils.Files.FilesOperations;
import magitEngine.Constants;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.File;

@WebListener
public class MagitServletContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        //creates magit folder on initialization of the tomcat servlet
        File magitFolder = new File(Constants.MAGIT_PATH);
        magitFolder.mkdirs();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        File magitFolder = new File(Constants.MAGIT_PATH);
        FilesOperations.deleteFolder(magitFolder);
    }


}
