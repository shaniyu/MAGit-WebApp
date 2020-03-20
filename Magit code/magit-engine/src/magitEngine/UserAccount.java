package magitEngine;

import magitObjects.Branch;
import magitObjects.Repository;
import notifications.NotificationsManager;
import notifications.PRStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

public class UserAccount {
    private String username;
    private NotificationsManager userNotifications;
    private Hashtable<String, Repository> userRepositories;
    private ArrayList<PullRequest> mPullRequests;
    private Hashtable<String, HashSet<String>> commitsPushedToRemoteByBranches;

    public UserAccount(String name)
    {
        username = name;
        userNotifications = new NotificationsManager();
        // empty repositories set
        userRepositories = new Hashtable<>();
        commitsPushedToRemoteByBranches = new Hashtable<>();
        mPullRequests = new ArrayList<>();
    }

    public Hashtable<String, HashSet<String>> getCommitsPushedToRemoteByBranches() {
        return commitsPushedToRemoteByBranches;
    }

    public synchronized void addCommitsPushedByBranch(String branchName, String repoName, HashSet<String> commitChanges){
        String key = branchName + ";" + repoName;
        commitsPushedToRemoteByBranches.put(key, commitChanges);
    }

    public synchronized void removeCommitsPushedByBranch(String branchName, String repoName){
        String key = branchName + ";" + repoName;
        commitsPushedToRemoteByBranches.remove(key);
    }

    public synchronized ArrayList<PullRequest> getmPullRequests() {
        return mPullRequests;
    }

    public synchronized void addPullRequest(PullRequest pullRequestToAdd){

        if(mPullRequests == null){
            mPullRequests = new ArrayList<>();
        }
        mPullRequests.add(pullRequestToAdd);
    }

    //This method will check if there is another open PR with the same details, and returns it if exists
    //We will return null if there is a PR with the same details but it is closed or declined (because that means we can't update the PR anymore)
    public PullRequest getPullRequest(String targetBranch, String baseBranch, String repoName) {

        //check if there is a PR from username, to the owner of repoName's Remote, from target to base
        for(PullRequest pullRequest : mPullRequests){
            if(pullRequest.getBaseBranchName().equals(baseBranch)
                    && pullRequest.getTargetBranchName().equals(targetBranch)
                    && pullRequest.getRemoteRepository().getRepositoryName().equals(repoName)
                    && pullRequest.getStatus() == PRStatus.OPEN) //if the PR is closed or declined we can't keep updating it.
            {
                return pullRequest;
            }
        }

        return null;
    }


    public void addRepository(Repository repoToAdd)
    {
        try {
            userRepositories.put(repoToAdd.getRepositoryName(), repoToAdd);
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    public Hashtable<String, Repository> getUserRepositoriesHashTable() {
        return userRepositories;
    }

    public ArrayList<Branch> getAllBranchesOfRepo(String repoName)
    {
        Repository repo = userRepositories.get(repoName);
        return repo.getAllOfTheBranches();
    }

    // we can assume we have this repo
    public boolean isHeadBranch(String repoName, String branchName)
    {
        Repository repo = userRepositories.get(repoName);
        return (repo.getHeadBranch().getName().equals(branchName));
    }

    public NotificationsManager getUserNotificationsManager() {
        return userNotifications;
    }
    public String getUsername() {
        return username;
    }
}
