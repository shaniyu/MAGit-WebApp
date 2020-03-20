package Servlets;

import Utils.Files.FilesOperations;
import Utils.Files.JsonOperations;
import ajaxResponses.GenericOperationResponse;
import ajaxResponses.PullRequestAjaxResponse;
import magitEngine.Constants;
import magitEngine.PullRequest;
import magitEngine.UserAccount;
import magitObjects.Branch;
import magitEngine.MagitEngine;
import magitObjects.Commit;
import magitObjects.Repository;
import notifications.ClientsManager;
import notifications.PLMessage;
import notifications.PRStatus;
import org.apache.commons.io.FileUtils;
import servletUtils.ServletUtils;


import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

@WebServlet(name = "PullRequestServlet", urlPatterns = "/pullrequest")
public class PullRequestServlet extends HttpServlet {

    private static final Object pullRequestLock = new Object();


    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        GenericOperationResponse res = null;
        ArrayList<PullRequestAjaxResponse> allPullRequestsAjaxResponse = null;

        String typeOfRequest = request.getParameter(Constants.REQUEST_TYPE);
        String userName = request.getParameter(Constants.USERNAME);
        String repoName = request.getParameter(Constants.REPO_NAME); // repository on which the user perform the pull request
        String branchToPush = request.getParameter(Constants.BRANCH_TO_PUSH);
        String targetBranch = request.getParameter(Constants.TARGET_BRANCH);
        String baseBranch = request.getParameter(Constants.BASE_BRANCH);
        String pullRequestMessage = request.getParameter(Constants.PR_MESSAGE);
        String remoteRepoUsername = request.getParameter(Constants.REMOTE_REPO_USERNAME);

        try {
            //Getting the user's user account
            ClientsManager clientsManager = ServletUtils.getClientsManager(getServletContext());
            UserAccount userAccount = clientsManager.getUsersInServer().get(userName);
            MagitEngine magitEngine = null;

            switch (typeOfRequest)
            {
                case(Constants.GET_PULL_REQUESTS):
                    allPullRequestsAjaxResponse = handleGetAllPullRequests(userAccount);
                    break;
                case(Constants.PUSH):
                     magitEngine = ServletUtils.createMagitEngine(userName, repoName, getServletContext());
                    res = handlePushBranch(branchToPush, repoName, magitEngine, userAccount);
                    break;
                case(Constants.CREATE_PR):
                    magitEngine = ServletUtils.createMagitEngine(userName, repoName, getServletContext());
                    UserAccount remoteRepoUserAccount = clientsManager.getUsersInServer().get(remoteRepoUsername);
                    res = handleCreatePullRequest(magitEngine, targetBranch, baseBranch,
                            userAccount, remoteRepoUserAccount, pullRequestMessage);
                    break;
                case(Constants.CONFIRM_PR):
                    res = handleApprovePullRequest(targetBranch, baseBranch, repoName, userAccount);
                    break;
                case(Constants.DECLINE_PR):
                    String declineMessage = request.getParameter(Constants.DECLINE_MESSAGE);
                    res = handleDeclinePullRequest(targetBranch, baseBranch, repoName, userAccount, declineMessage);
                    break;
            }

        } catch (Exception e) {
            res = new GenericOperationResponse(false, e.getMessage());
        } finally {
            JsonOperations.printToOut(typeOfRequest.equals(Constants.GET_PULL_REQUESTS) ?
                    allPullRequestsAjaxResponse : res, response.getWriter());
        }
    }

    private GenericOperationResponse handlePushBranch(String branchToPush, String repoName, MagitEngine magitEngine, UserAccount userAccount) {

        GenericOperationResponse res = null;

        //Creating the commitsDelta Hashset in order to calculate commits pushed to remote repository
        HashSet<String> commitDelta = new HashSet<>();

        try{
            //pushing branch to remote repo will add the branch in RR (if doesn't exist) and all the commit deltas that are missing in the RR
            pushBranchToRemoteRepo(branchToPush, magitEngine, commitDelta);
            //Putting it in the Hashtable of current user's user-account, so we can get it later in order to put on pull request
            userAccount.addCommitsPushedByBranch(branchToPush, repoName, commitDelta);
            res = new GenericOperationResponse(true, "Branch " + branchToPush + " was pushed successfully");
        }
        catch (Exception e){
            res = new GenericOperationResponse(false, "Couldn't push branch " + branchToPush + "\n" + e.getMessage());
        }
        finally {
            return res;
        }
    }

    private ArrayList<PullRequestAjaxResponse> handleGetAllPullRequests(UserAccount userAccount) {

        ArrayList<PullRequestAjaxResponse> allPullRequestsAjaxResponse = new ArrayList<>();
        ArrayList<PullRequest> usersPullRequests = userAccount.getmPullRequests();

        for (PullRequest pullRequest : usersPullRequests){

            //Getting all details needed for pullRequestAjaxResponse from Pull Request
            String userWhoCreatedPR = pullRequest.getUsernameWhoAsked();
            HashSet<String> commitsDelta = new HashSet<>();
            commitsDelta.addAll(pullRequest.getCommitsDelta());
            String targetBranch = pullRequest.getTargetBranchName();
            String baseBranch = pullRequest.getBaseBranchName();
            Date currDate = pullRequest.getDateOfRequest();
            PRStatus currStatus = pullRequest.getStatus();
            String remoteRepoName = pullRequest.getRemoteRepository().getRepositoryName();

            PullRequestAjaxResponse pullRequestAjaxResponse = new PullRequestAjaxResponse(userWhoCreatedPR, targetBranch,
                    baseBranch, currDate, currStatus, commitsDelta, remoteRepoName);
            allPullRequestsAjaxResponse.add(pullRequestAjaxResponse);
        }

        return allPullRequestsAjaxResponse;
    }

    private GenericOperationResponse handleCreatePullRequest(MagitEngine magitEngine, String targetBranch, String baseBranch,
                                                             UserAccount userAccount, UserAccount remoteRepoUserAccount,
                                                             String pullRequestMessage) throws Exception{

        PullRequest pullRequestToUpdate = null;
        String repoName = magitEngine.getCurrentRepo().getRepositoryName();
        String commitDeltaKey = targetBranch + ";" + repoName;
        HashSet<String> commitDelta = userAccount.getCommitsPushedToRemoteByBranches().get(commitDeltaKey); //This will be updated by reference when pushing new commits to RR and then the PR will be created/updated with this
        //This is the target branch as it appears in the pull requests (it is RTB)
        String RTBTargetBranch = repoName + File.separator + targetBranch;
        GenericOperationResponse res;

        synchronized (pullRequestLock) {
            if(commitDelta == null){
                res = new GenericOperationResponse(false, targetBranch + " wasn't pushed to remote repository yet");
            }
            else{
                if ((pullRequestToUpdate = remoteRepoUserAccount.getPullRequest(RTBTargetBranch, baseBranch, repoName)) != null) {
                    // need to update the pull request, add new commits to it and send update notification to the destination repo user name
                    res = handleUpdatePullRequest(pullRequestToUpdate, pullRequestMessage, magitEngine, remoteRepoUserAccount, commitDelta);
                } else {

                    //Getting the local repository
                    Repository localRepo = userAccount.getUserRepositoriesHashTable().get(repoName);
                    if (!isTargetAndBaseHaveDifferentCommits(targetBranch, baseBranch, localRepo)) {
                        res = new GenericOperationResponse(false, "Can't create pull request, target and base points to the same commit");
                    } else {
                        //We already pushed the local branch to the remote repository so no need to push it here
                        //Now the PR will be created for the base branch and the new target branch which is now an RTB
                        String newRTBTargetBranch = repoName + File.separator + targetBranch;
                        //The PR id is calculated by the size of the user's message list
                        int PRid = userAccount.getmPullRequests().size();
                        Date currDate = new Date();
                        String userName = userAccount.getUsername();
                        createPullRequest(userName, repoName, newRTBTargetBranch, baseBranch, pullRequestMessage,
                                PRid, currDate, localRepo, remoteRepoUserAccount, commitDelta);

                        // also send PR creation notification to the destination repo user
                        PLMessage prMessage = new PLMessage(PRid, PRStatus.OPEN, pullRequestMessage, userName,
                                repoName, currDate, newRTBTargetBranch, baseBranch);
                        remoteRepoUserAccount.getUserNotificationsManager().addMessage(prMessage);
                        res = new GenericOperationResponse(true, "Pull request was created and sent to " + remoteRepoUserAccount.getUsername());
                    }
                }
            }
        }

        return (res);
    }

    private void pushBranchToRemoteRepo(String targetBranch, MagitEngine magitEngine, HashSet<String> commitsDelta) throws Exception{

        boolean isTargetBranchRTB = magitEngine.isBranchRTB(targetBranch);
        Repository localRepo = magitEngine.getCurrentRepo();
        String repoName = localRepo.getRepositoryName();
        String remoteBranchName = repoName + File.separator + targetBranch;
        String branchesInRemotePath = localRepo.getRemoteRepoPath() + File.separator + ".magit" + File.separator + "branches";
        // fetching only the RTB in the LR head that tracks the branch in the RR
        File targetBranchFileInLocal = new File(localRepo.getBranchesPath() + File.separator + targetBranch);

        // get the sha1 of the LR target branch and update the branch in the RR, and the RB in the LR
        String branchSha1InLocalTargetBranch = FileUtils.readFileToString(targetBranchFileInLocal, StandardCharsets.UTF_8);

        File branchInRemote = new File ( branchesInRemotePath + File.separator + targetBranch);

        File remoteBranchInLocal = new File(localRepo.getRepositoryLocation() +
                File.separator + ".magit" + File.separator +
                "branches" + File.separator + remoteBranchName);

        //Writing branch sha1 to branch in RR and to remote branch in LR
        //The only case in which the branch file in remote/ remote branch file in local doesn't exist-
        // is if we're pushing a new branch to RR (Bonus 4)
        if(!branchInRemote.exists()){
            branchInRemote.createNewFile();
        }

        FileUtils.writeStringToFile(branchInRemote, branchSha1InLocalTargetBranch, StandardCharsets.UTF_8);

        if(!remoteBranchInLocal.exists()){
            remoteBranchInLocal.createNewFile();
        }
        FileUtils.writeStringToFile(remoteBranchInLocal, branchSha1InLocalTargetBranch, StandardCharsets.UTF_8);

        //If the branch to push isn't RTB- then we want to add it to the RTB file in LR (Bonus 4)
        Branch remoteBranch = null;
        if(!isTargetBranchRTB){
            String rtbFileInLRPath = localRepo.getMagitPath() + File.separator + "RTB";
            String strForRTB = targetBranch + System.lineSeparator()
                    + remoteBranchName + System.lineSeparator() +
                    ";" + System.lineSeparator();
            // append the branch to RTB file
            FilesOperations.appendTextToFile(strForRTB, rtbFileInLRPath);
            //Adding the new remote branch to the allOfTheBranches list
            remoteBranch = new Branch(remoteBranchName, new Commit(branchSha1InLocalTargetBranch));
            remoteBranch.setIsRemote(true);
            localRepo.getAllOfTheBranches().add(remoteBranch);
        }
        else {
            //Setting the remote branch in this repository with the new RTB sha1
            remoteBranch = localRepo.getBranchByName(remoteBranchName);
            remoteBranch.setCommit(new Commit(branchSha1InLocalTargetBranch));
            localRepo.resetAndUpdateAllOfTheBranches(); // Need the remote branch to get all commit details
        }

        //Now we want to send the RR the new commits of the target branch
        magitEngine.pushNewCommitsToRemoteTargetBranch(targetBranch, commitsDelta);

        //Updating the remote repository branches
        String remoteRepoUserName = localRepo.getRemoteRepoUsername();
        Repository remoteRepository = ServletUtils.getClientsManager(getServletContext()).getRepoOfUserByRepoName(repoName, remoteRepoUserName);
        remoteRepository.resetAndUpdateAllOfTheBranches();
    }


    private GenericOperationResponse handleUpdatePullRequest(PullRequest pullRequestToUpdate, String pullRequestMessage,
                                         MagitEngine magitEngine, UserAccount remoteRepoUserAccount, HashSet<String> commitsDelta) throws Exception
    {
        GenericOperationResponse res;
        String remoteTargetBranch = pullRequestToUpdate.getTargetBranchName();
        String targetBranchName = remoteTargetBranch.substring(remoteTargetBranch.lastIndexOf(File.separator) + 1);

        String repoName = magitEngine.getCurrentRepo().getRepositoryName();
        String userName = pullRequestToUpdate.getUsernameWhoAsked();

        if( ! magitEngine.isThereCommitsToUpdate( pullRequestToUpdate))
        {
            res = new GenericOperationResponse(false, "No commits to push to this target");
        }
        else {
            //push the commit deltas to the remote repository and update the branch in remote repo, and the remote branch in local repo
            pushNewCommitsAndUpdateBranches(targetBranchName, magitEngine, commitsDelta);

            // need to update the pull request object
            Date currDate = new Date();
            pullRequestToUpdate.setDateOfRequest(currDate); //new date of update
            pullRequestToUpdate.setPRMessage(pullRequestMessage); //new PR message
            pullRequestToUpdate.getCommitsDelta().addAll(commitsDelta); //new commit deltas

            // need to send an update notification to the owner of the RR of repoName
            int PRid = pullRequestToUpdate.getId();
            String baseBranchName = pullRequestToUpdate.getBaseBranchName();
            PLMessage prMessage = new PLMessage(PRid, PRStatus.OPEN, "Updated- " + pullRequestMessage, userName, repoName, currDate, remoteTargetBranch, baseBranchName);
            remoteRepoUserAccount.getUserNotificationsManager().addMessage(prMessage);
            res = new GenericOperationResponse(true, "Pull request on this repo was updated with new commits and sent to " + remoteRepoUserAccount.getUsername());
        }
        return res;
    }

    //This method pushes the commit deltas to the remote repository and updates the branch in remote repo, and the remote branch in local repo
    private synchronized void pushNewCommitsAndUpdateBranches(String targetBranchName, MagitEngine magitEngine, HashSet<String> commitsDelta) throws Exception{

        Repository localRepo = magitEngine.getCurrentRepo();
        magitEngine.pushNewCommitsToRemoteTargetBranch(targetBranchName, commitsDelta);

        //Update the commit sha1 in the branch file of the remote repository
        String targetBranchFilePathInRemote = localRepo.getRemoteRepoPath() + File.separator + ".magit" + File.separator
                + "branches" + File.separator + targetBranchName;
        File targetBranchFileInRemote = new File(targetBranchFilePathInRemote);
        File targetBranchFileInLocal = new File(localRepo.getBranchesPath() + File.separator + targetBranchName);
        String updatedSha1 = FileUtils.readFileToString(targetBranchFileInLocal, StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(targetBranchFileInRemote, updatedSha1, StandardCharsets.UTF_8);
        //Updating the remote repository branches
        String repoName = localRepo.getRepositoryName();
        String remoteRepoUserName = localRepo.getRemoteRepoUsername();
        Repository remoteRepository = ServletUtils.getClientsManager(getServletContext()).getRepoOfUserByRepoName(repoName, remoteRepoUserName);
        remoteRepository.resetAndUpdateAllOfTheBranches();

        //Need to update the remote branch in local repo with the new commit
        String remoteBranchName = localRepo.getRemoteRepoName() + File.separator + targetBranchName;
        File remoteBranchFile = new File(localRepo.getBranchesPath() + File.separator + remoteBranchName);
        FileUtils.writeStringToFile(remoteBranchFile, updatedSha1, StandardCharsets.UTF_8);
        localRepo.resetAndUpdateAllOfTheBranches(); // Need the remote branch to get all commit details
    }


    private void createPullRequest(String userName, String repoName, String targetBranch,
                                   String baseBranch, String pullRequestMessage, int PRid,
                                   Date currDate, Repository localRepository, UserAccount remoteRepoUserAccount, HashSet<String> commitsDelta)
    {
        //Getting the remoteRepository
        Repository remoteRepository = remoteRepoUserAccount.getUserRepositoriesHashTable().get(repoName);
        String remoteRepoUserName = remoteRepoUserAccount.getUsername();
        PullRequest pullRequest = new PullRequest(userName, remoteRepoUserName, targetBranch, baseBranch,
                currDate, PRStatus.OPEN, PRid, pullRequestMessage, localRepository, remoteRepository, commitsDelta);
        remoteRepoUserAccount.addPullRequest(pullRequest);
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


    private GenericOperationResponse handleApprovePullRequest(String targetBranch, String baseBranch, String repoName, UserAccount userAccount ) throws IOException, Exception
    {
        ClientsManager clientsManager = ServletUtils.getClientsManager(getServletContext());
        PullRequest pullRequestToApprove = userAccount.getPullRequest(targetBranch, baseBranch, repoName);
        GenericOperationResponse res = null;

        if (pullRequestToApprove != null)
        {
            // pull request exist
            // 1. merge target to base
            MagitEngine engine = ServletUtils.createMagitEngine(userAccount.getUsername(), repoName, getServletContext());
            //The target branch name is as it was called in the userWhoAsked repository.
            // Now we want it to be the target branch as it is called in the remote repository (of the user who got the request)
            //So we need to cut the name after the "/"
            String targetBranchNameInRemote = targetBranch.substring(targetBranch.lastIndexOf(File.separator) + 1);
            String baseBranchNameInRemote = baseBranch.substring(baseBranch.lastIndexOf(File.separator) + 1);

            // if user accept PR on his HEAD branch, but WC is not clean, can't approve PR
            if(engine.isThereOpenChangesInRepository() && baseBranchNameInRemote.equals(engine.getCurrentRepo().getHeadBranch().getName()))
            {
                res = new GenericOperationResponse(false, "Can't approve! You have open changes on your working copy, and base branch is head branch.");
            }
            else
            {
                engine.approvePullRequest(targetBranchNameInRemote, baseBranchNameInRemote);

                // 2. update pull request status to closed
                pullRequestToApprove.setStatus(PRStatus.CLOSED);

                // 3. send notification to the sender of the pull request
                String pullRequestSender = pullRequestToApprove.getUsernameWhoAsked();
                String message = userAccount.getUsername() + " approved your pull request on " + repoName +" repository";
                PLMessage closedPRMessage = new PLMessage(pullRequestToApprove.getId(), PRStatus.CLOSED, message, userAccount.getUsername(), repoName, new Date(), targetBranch, baseBranch);
                UserAccount remoteRepoUserAccount = clientsManager.getUsersInServer().get(pullRequestSender);
                remoteRepoUserAccount.getUserNotificationsManager().addMessage(closedPRMessage);
                //removing the commitsDelta of this pull request from the UserAccount hashtable of commits pushed to repository
                //since the current userAccount is of the user that received the PR, we need to do it on the remoteRepoUserAccount which is of the user that sent the PR
                remoteRepoUserAccount.removeCommitsPushedByBranch(targetBranchNameInRemote, repoName);
                res = new GenericOperationResponse(true, "");
            }
        }
        else
        {
            res = new GenericOperationResponse(false, "For some reason, you don't have this pull request anymore.");
        }
        return res;
    }

    private GenericOperationResponse handleDeclinePullRequest(String targetBranch, String baseBranch, String repoName, UserAccount userAccount, String declineMessage)
    {
        GenericOperationResponse res = null;
        ClientsManager clientsManager = ServletUtils.getClientsManager(getServletContext());

        // target user doesn't want this PR and asks to close it
        // remove this PR from our user
        PullRequest pullRequestToDecline = userAccount.getPullRequest(targetBranch, baseBranch, repoName);

        if(pullRequestToDecline != null)
        {
            // 1. update pull request status to declined
            pullRequestToDecline.setStatus(PRStatus.DECLINED);

            // 2. sent notification to the sender of closing the PR
            MagitEngine engine = ServletUtils.createMagitEngine(userAccount.getUsername(), repoName, getServletContext());
            String pullRequestSender = pullRequestToDecline.getUsernameWhoAsked();
            String message = userAccount.getUsername() + " declined your pull request on " + repoName +" repository, and it is now closed. Reason: "+ declineMessage;
            PLMessage closedPRMessage = new PLMessage(pullRequestToDecline.getId(), PRStatus.DECLINED, message, userAccount.getUsername(), repoName, new Date(), targetBranch, baseBranch);
            UserAccount remoteRepoUserAccount = clientsManager.getUsersInServer().get(pullRequestSender);
            remoteRepoUserAccount.getUserNotificationsManager().addMessage(closedPRMessage);
            //removing the commitsDelta of this pull request from the UserAccount hashtable of commits pushed to repository
            //since the current userAccount is of the user that received the PR, we need to do it on the remoteRepoUserAccount which is of the user that sent the PR
            String targetBranchNameInRemote = targetBranch.substring(targetBranch.lastIndexOf(File.separator) + 1);
            remoteRepoUserAccount.removeCommitsPushedByBranch(targetBranchNameInRemote, repoName);
            res = new GenericOperationResponse(true, "");
        }
        else
        {
            res = new GenericOperationResponse(false, "For some reason, you don't have this pull request anymore.");
        }
        return res;
    }

    private boolean isTargetAndBaseHaveDifferentCommits(String targetBranchName, String baseBranchName, Repository localRepo)
    {
        String sha1OfTarget = localRepo.getBranchByName(targetBranchName).getCommit().getSha1();
        String sha1OfBase = localRepo.getBranchByName(baseBranchName).getCommit().getSha1();

        return (!sha1OfBase.equals(sha1OfTarget));
    }
}
