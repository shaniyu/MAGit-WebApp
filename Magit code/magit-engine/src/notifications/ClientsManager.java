package notifications;

import magitEngine.Constants;
import magitEngine.MagitEngine;
import magitEngine.UserAccount;
import magitObjects.Branch;
import magitObjects.Commit;
import magitObjects.Repository;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class ClientsManager {
    private Hashtable<String, UserAccount> usersInServer; // key - user name, value - user account


    public ClientsManager() {
        this.usersInServer = new Hashtable<>();
    }

    public synchronized void addUser(String username)
    {
        UserAccount newUserAccount = new UserAccount(username);
        usersInServer.put(username, newUserAccount);
        createUserDirectoryOnMagitPath(username);
    }

    // we can assume this user exists
    public void addRepositoryToUser(String username, Repository newRepo)
    {
        UserAccount userAccount = usersInServer.get(username);
        userAccount.addRepository(newRepo);
    }

    // this is not synchronized function but it is called with synchronized code only!
    public boolean isUserExist(String username)
    {
        return (usersInServer.get(username) != null);
    }

    // can assume this user exists
    public Hashtable<String, Repository> getAllReposOfUser(String username)
    {
        UserAccount user = usersInServer.get(username);
        return user.getUserRepositoriesHashTable();
    }

    public synchronized Set<String> getAllUserNames()
    {
        return usersInServer.keySet();
    }

    public ArrayList<Branch> getAllRepoBranches(String username, String repoName)
    {
        ArrayList<Branch> result;
        UserAccount user = usersInServer.get(username);
        result = user.getAllBranchesOfRepo(repoName);

       return result;
    }
    public boolean isHeadBranch(String username, String repoName, String branchName)
    {
        UserAccount user = usersInServer.get(username);
        return user.isHeadBranch(repoName, branchName);
    }

    public void forkRepository()
    {
        // handle fork notification
    }

    private void createUserDirectoryOnMagitPath(String username)
    {
        // creating a folder for the user in the path :  c:\magit-ex3
        String pathForUserFolder = Constants.MAGIT_PATH + File.separator + username;
        File newDirForUser = new File(pathForUserFolder);
        newDirForUser.mkdirs();
    }

    public Hashtable<String, UserAccount> getUsersInServer() {
        return usersInServer;

    }

    public boolean isThereOpenChangesInRepo(String username, String repoName) throws Exception
    {
        UserAccount user = usersInServer.get(username);
        Repository repo = user.getUserRepositoriesHashTable().get(repoName);
        if (repo != null) // sanity check
        {
            MagitEngine magitEngine = new MagitEngine();
            magitEngine.setCurrentRepo(repo);
            magitEngine.setUserName(username);
            boolean res;
            res = magitEngine.isThereOpenChangesInRepository();
            return res;
        }

        return false;
    }

    public ArrayList<Commit> getHeadCommits(String username, String repoName) throws IOException
    {
        ArrayList<Commit> res = new ArrayList<>();
        UserAccount user = usersInServer.get(username);
        Repository repo = user.getUserRepositoriesHashTable().get(repoName);
        MagitEngine engine = new MagitEngine();
        engine.setCurrentRepo(repo);
        engine.setUserName(username);
        Commit headCommit = repo.getHeadBranch().getCommit();
        if(headCommit != null) { //headCommit could be null if this is an empty repository
            HashSet<String> sha1OfAllCommitsInHistoryOfHead = engine.getAllSha1InHistoryOfCommit(headCommit);

            for (String sha1 : sha1OfAllCommitsInHistoryOfHead)
            {
                Commit commit = repo.getCommitBySHA1(sha1);
                res.add(commit);
            }
        }

        return res;
    }

    public ArrayList<String> getBranchesPointedByCommit(String username, String repoName, Commit commit)
    {
        ArrayList<String> result = new ArrayList<>();
        UserAccount user = usersInServer.get(username);
        Repository repo = user.getUserRepositoriesHashTable().get(repoName);
        ArrayList<Branch> allOfRepoBranches = getAllRepoBranches(username, repoName);
        for (Branch b: allOfRepoBranches)
        {
            if (b.getCommit() != null && !b.getCommit().getSha1().equals("null"))
            {
                if (b.getCommit().getSha1().equals(commit.getSha1()))
                {
                    result.add(b.getName());
                }
            }
        }
        return result;
    }

    public Repository getRepoOfUserByRepoName(String repoName, String username)
    {
        return getAllReposOfUser(username).get(repoName);
    }


    public List<Message> getAllUserMessages(String userName, int versionFrom)
    {
        UserAccount account = usersInServer.get(userName);
        NotificationsManager manager = account.getUserNotificationsManager();
        return manager.getNotifications(versionFrom);
    }
    public int getNotificationsVersion(String username)
    {
        UserAccount account = usersInServer.get(username);
        NotificationsManager manager = account.getUserNotificationsManager();
        return manager.getVersion();
    }

    public int getNotificationsVersionInClient(String username)
    {
        UserAccount account = usersInServer.get(username);
        NotificationsManager manager = account.getUserNotificationsManager();
        return manager.getVersionOfUser();
    }

    public void setNotificationsVersionInClient(String username, int version)
    {
        UserAccount account = usersInServer.get(username);
        NotificationsManager manager = account.getUserNotificationsManager();
       manager.setVersionOfUser(version);
    }


    public void sendMessageToUser(String usernameToSendTo, Message message)
    {
        UserAccount account = usersInServer.get(usernameToSendTo);
        NotificationsManager manager = account.getUserNotificationsManager();
        manager.addMessage(message);
    }
}
