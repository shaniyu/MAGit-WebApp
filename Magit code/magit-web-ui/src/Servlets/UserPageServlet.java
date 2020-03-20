package Servlets;

import Utils.Files.JsonOperations;
import ajaxResponses.NotificationsAjaxResponse;
import ajaxResponses.RepositoryAjaxResponse;
import javafx.fxml.Initializable;
import magitEngine.Constants;
import magitEngine.MagitEngine;
import magitObjects.Commit;
import magitObjects.Repository;
import notifications.ClientsManager;
import notifications.Message;
import servletUtils.ServletUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

@WebServlet(name = "UserPageServlet", urlPatterns = "/loadUser")
public class UserPageServlet extends HttpServlet {


    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // we want to return an object, any object and not only text, so we use json
        response.setContentType("application/json;charset=UTF-8");

        try {
            String userName = request.getParameter(Constants.USERNAME);
            String typeOfRequest = request.getParameter(Constants.REQUEST_TYPE);

            switch (typeOfRequest)
            {
                case(Constants.GET_REPOS_REQUEST):
                    // get all repositories of user
                    handleGetUserRepos(userName, response);
                    break;
                case(Constants.GET_USERS_REQUEST):
                    handleGetUsers(userName, response);
                    break;
                case(Constants.GET_USER_NOTIFICATIONS):
                    String notificationsVersion = request.getParameter(Constants.NOTIFICATIONS_VERSION);
                    handleGetUserMessages(userName, Integer.valueOf(notificationsVersion), response);
                    break;
                case(Constants.GET_USER_NOTIFICATIONS_VERSION_CLIENT):
                    handleGetUserMessagesVersionInClient(userName, response);
                    break;
                case(Constants.GET_USER_NOTIFICATIONS_VERSION):
                    handleGetUserMessagesVersion(userName, response);
                    break;
                case(Constants.SET_USER_NOTIFICATIONS_VERSION_CLIENT):
                    int version = Integer.valueOf(request.getParameter(Constants.NOTIFICATIONS_VERSION));
                    setNotificationsVersionInClient(userName, version, response);
                    break;
            }

        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private void handleGetUserRepos(String userName, HttpServletResponse response) throws IOException
    {
        PrintWriter out = response.getWriter();
        ArrayList<RepositoryAjaxResponse> userRepos = getUserRepositories(userName);
        //z{"repositoryName":"stam repo2","activeBranchName":"master","branchesNumber":1},{"repositoryName":"stam repo1","activeBranchName":"master","branchesNumber":1},{"repositoryName":"stam repo","activeBranchName":"master","branchesNumber":1}]
        JsonOperations.printToOut(userRepos, out);
    }

    private void handleGetUsers(String userName, HttpServletResponse response) throws IOException
    {
        PrintWriter out = response.getWriter();
        String[] allUsersExceptMe = getAllMagitUserNames(userName);
        JsonOperations.printToOut(allUsersExceptMe, out);
    }

    private void handleGetUserMessages(String userName, int lastUserNotificationsVersion, HttpServletResponse response) throws IOException
    {
        PrintWriter out = response.getWriter();
        ArrayList<Message> messagesToSend = getAllUserMessages(userName, lastUserNotificationsVersion);
        int version = getNotificationsVersion(userName);
        NotificationsAjaxResponse res = new NotificationsAjaxResponse(messagesToSend, version);
        JsonOperations.printToOut(res, out);
    }

    private void handleGetUserMessagesVersion(String userName, HttpServletResponse response) throws IOException
    {
        PrintWriter out = response.getWriter();
        int notificationsVersion = getNotificationsVersion(userName);
        JsonOperations.printToOut(notificationsVersion, out);
    }

    // returns the version number that the user already seen
    private void handleGetUserMessagesVersionInClient(String userName, HttpServletResponse response) throws IOException
    {
        PrintWriter out = response.getWriter();
        int notificationsVersionInClient = getNotificationsVersionInClient(userName);
        JsonOperations.printToOut(notificationsVersionInClient, out);
    }

    private int getNotificationsVersionInClient(String username)
    {
        ClientsManager CM = ServletUtils.getClientsManager(getServletContext());
        int notificationsVersionInClient = CM.getNotificationsVersionInClient(username);
        return notificationsVersionInClient;
    }

    private void setNotificationsVersionInClient(String username, int version, HttpServletResponse response) throws IOException
    {
        ClientsManager CM = ServletUtils.getClientsManager(getServletContext());
        CM.setNotificationsVersionInClient(username, version);
        PrintWriter out = response.getWriter();
        JsonOperations.printToOut("OK", out);
    }

    private int getNotificationsVersion(String username)
    {
        ClientsManager CM = ServletUtils.getClientsManager(getServletContext());
        int notificationsVersion = CM.getNotificationsVersion(username);
        return notificationsVersion;
    }

    private ArrayList<RepositoryAjaxResponse> getUserRepositories(String username){
        ClientsManager CM = ServletUtils.getClientsManager(getServletContext());
        Hashtable<String, Repository> allUserRepos = CM.getAllReposOfUser(username);

        // create array of RepositoryAjaxResponse from the hashmap
        ArrayList<RepositoryAjaxResponse> result = new ArrayList<>();
        Set<String> keys = allUserRepos.keySet();

        for(String key: keys){
            String lastCommitMessage = null;
            String lastCommitDate = null;
            String remoteRepoName = null;
            String remoteRepoPath = null;
            String remoteRepoUsername = null;

            Repository repo = allUserRepos.get(key);
            String repoName = repo.getRepositoryName();
            String headBranch = repo.getHeadBranch().getName();
            int branchesNumber = repo.getAllOfTheBranches().size();
            Commit lastCommit = repo.getLastCommit();
            if (lastCommit != null)
            {
                lastCommitDate = lastCommit.getmCreatedDate();
                lastCommitMessage = lastCommit.getmMessage();
            }
            // this repo has a RR , it was created by  fork
            if (repo.getRemoteRepoPath() != null && repo.getRemoteRepoName() != null)
            {
                remoteRepoName = repo.getRemoteRepoName();
                remoteRepoPath = repo.getRemoteRepoPath();
                remoteRepoUsername = repo.getRemoteRepoUsername();
            }

            result.add(new RepositoryAjaxResponse(repoName, headBranch, branchesNumber, lastCommitDate, lastCommitMessage, remoteRepoPath, remoteRepoName, remoteRepoUsername));
        }
        return result;
    }

    private String[] getAllMagitUserNames(String username)
    {
        ClientsManager CM = ServletUtils.getClientsManager(getServletContext());

        Set<String> allUsers  = new HashSet<>();
        allUsers.addAll(CM.getAllUserNames());
        allUsers.remove(username);
        return allUsers.toArray(new String[0]);
    }

    private ArrayList<Message> getAllUserMessages(String userName, int lastUserNotificationsVersion)
    {
        ArrayList<Message> res = new ArrayList<>();
        ClientsManager CM = ServletUtils.getClientsManager(getServletContext());
        res.addAll(CM.getAllUserMessages(userName, lastUserNotificationsVersion));
        return res;
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
