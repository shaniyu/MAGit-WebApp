package ajaxResponses;

import notifications.PRStatus;
import java.util.Date;
import java.util.HashSet;

public class PullRequestAjaxResponse {

    String userWhoCreatedPR;
    HashSet<String> commitsDelta;
    String targetBranch;
    String baseBranch;
    Date creationDate;
    PRStatus currStatus;
    String remoteRepoName;

    public PullRequestAjaxResponse(String userWhoCreatedPR, String targetBranch, String baseBranch,
                                   Date creationDate, PRStatus currStatus, HashSet<String> commitsDelta, String remoteRepoName) {
        this.userWhoCreatedPR = userWhoCreatedPR;
        this.targetBranch = targetBranch;
        this.baseBranch = baseBranch;
        this.creationDate = creationDate;
        this.currStatus = currStatus;
        this.commitsDelta = commitsDelta;
        this.remoteRepoName = remoteRepoName;
    }
}
