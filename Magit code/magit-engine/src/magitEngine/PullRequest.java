package magitEngine;

import magitObjects.Repository;
import notifications.PRStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

public class PullRequest {
    private String usernameWhoAsked;
    private String userNameWhoGetsRequest;
    private String targetBranchName; // push this branch into the base
    private String baseBranchName; // to merge in to
    private Date dateOfRequest;
    //commitsDelta This will hold all the commits that were pushed to the remote (the delta)
    //We need this in order to determine what changed commits (=the delta that was pushed) to show for a single pull request
    HashSet<String> commitsDelta;
    private PRStatus status;
    private int id;
    private String PRMessage;
    private Repository remoteRepository; //the repository that was cloned (this repository's owner will get the PR)
    private Repository localRepository; // the repository which cloned the remoteRepository


    public PullRequest(String usernameWhoAsked, String userNameWhoGetsRequest, String targetBranchName,
                       String baseBranchName, Date dateOfRequest,
                       PRStatus status, int id, String PRMessage,
                       Repository localRepository, Repository remoteRepository, HashSet<String> commitsDelta)
    {
        this.usernameWhoAsked = usernameWhoAsked;
        this.userNameWhoGetsRequest = userNameWhoGetsRequest;
        this.targetBranchName = targetBranchName;
        this.baseBranchName = baseBranchName;
        this.dateOfRequest = dateOfRequest;
        this.status = status;
        this.id = id;
        this.PRMessage = PRMessage;
        this.localRepository = localRepository;
        this.remoteRepository = remoteRepository;
        this.commitsDelta = commitsDelta;
    }

    public PRStatus getStatus() {
        return status;
    }

    public void setStatus(PRStatus status) {
        this.status = status;
    }

    public String getUserNameWhoGetsRequest() {
        return userNameWhoGetsRequest;
    }

    public String getTargetBranchName() {
        return targetBranchName;
    }

    public String getBaseBranchName() {
        return baseBranchName;
    }

    public Repository getRemoteRepository() {
        return remoteRepository;
    }

    public Repository getLocalRepository() {
        return localRepository;
    }

    public String getUsernameWhoAsked() {
        return usernameWhoAsked;
    }

    public void setDateOfRequest(Date dateOfRequest) {
        this.dateOfRequest = dateOfRequest;
    }

    public int getId() {
        return id;
    }

    public String getPRMessage() {
        return PRMessage;
    }

    public void setPRMessage(String PRMessage) {
        this.PRMessage = PRMessage;
    }

    public HashSet<String> getCommitsDelta() {
        return commitsDelta;
    }

    public Date getDateOfRequest() {
        return dateOfRequest;
    }
}


