package servletUtils;

import magitEngine.MagitEngine;
import magitObjects.Repository;
import notifications.ClientsManager;

import javax.servlet.ServletContext;

public class ServletUtils {
    private static final String CLIENTS_MANAGER = "clienstManager";
    private static final Object clientsManagerLock = new Object();


    public static ClientsManager getClientsManager(ServletContext context)
    {
        ClientsManager CM;

        synchronized(clientsManagerLock)
        {
            CM = (ClientsManager)context.getAttribute(CLIENTS_MANAGER);
            if (CM == null)
            {
                CM = new ClientsManager();
                context.setAttribute(CLIENTS_MANAGER, CM);
            }
        }
        return (CM);
    }

    //Creating an instance of magit engine with current username and repository
    public static MagitEngine createMagitEngine(String userName, String repoName, ServletContext servletContext){

        ClientsManager CM = getClientsManager(servletContext);
        MagitEngine magitEngine = new MagitEngine();
        //Getting repository object according to the username and repository name
        Repository currentRepo = CM.getAllReposOfUser(userName).get(repoName);
        magitEngine.setUserName(userName);
        magitEngine.setCurrentRepo(currentRepo);
        return magitEngine;
    }
}
