package Servlets;

import Utils.Files.JsonOperations;
import ajaxResponses.GenericOperationResponse;
import magitEngine.Constants;
import magitEngine.MagitEngine;
import magitObjects.Repository;
import notifications.ClientsManager;
import notifications.Message;
import servletUtils.ServletUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;

@WebServlet(name = "ForkServlet", urlPatterns = "/forkRepo")
public class ForkServlet extends HttpServlet {
    private static final Object forkLock = new Object();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        try {
            String userName = request.getParameter(Constants.USERNAME);
            String userNameToFork = request.getParameter(Constants.USER_TO_FORK);
            String repoNameToFork = request.getParameter(Constants.REPO_TO_FORK);

            ClientsManager manager = ServletUtils.getClientsManager(getServletContext());
            synchronized (forkLock)
            {
                // check if the current user already has a repo with this name
                Hashtable<String, Repository> allUserRepos = manager.getAllReposOfUser(userName);
                if (allUserRepos.get(repoNameToFork) != null )
                {
                    // the active user already has a repo with with name
                    // return false to the client
                    GenericOperationResponse res = new GenericOperationResponse(false, "You already have repository with this name");
                    JsonOperations.printToOut(res, response.getWriter());
                }
                else
                {
                    MagitEngine engine = new MagitEngine();
                    engine.setUserName(userName);

                    String pathOfRepoToFork = Constants.MAGIT_PATH + File.separator + userNameToFork + File.separator + repoNameToFork;
                    String newRepoPath = Constants.MAGIT_PATH + File.separator + userName;
                    Repository newRepo = engine.clone(pathOfRepoToFork, newRepoPath, repoNameToFork, repoNameToFork, userNameToFork);

                    // added the new repo to the current user
                    manager.addRepositoryToUser(userName, newRepo);

                    // sent notification to the forked repo user
                    Message forkMessage = new Message(userName, repoNameToFork, userName + " forked your repository.", new Date());
                    manager.sendMessageToUser(userNameToFork, forkMessage);

                    // send message to client
                    GenericOperationResponse res = new GenericOperationResponse(true, null);
                    JsonOperations.printToOut(res, response.getWriter());
                }
            }
        }
        catch (Exception e)
        {
            GenericOperationResponse res = new GenericOperationResponse(false, e.getMessage());
            JsonOperations.printToOut(res, response.getWriter());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}
