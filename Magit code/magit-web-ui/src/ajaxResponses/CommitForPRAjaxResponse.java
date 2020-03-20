package ajaxResponses;

import DataStructures.CommitChanges;

public class CommitForPRAjaxResponse {

    String commitSha1;
    String commitMessage;
    String firstPrecedingCommitSha1;
    String secondPrecedingCommitSha1;
    CommitChangesAjaxResponse changesToFirstPrecedingCommit;
    CommitChangesAjaxResponse changesToSecondPrecedingCommit;

    public CommitForPRAjaxResponse(String commitSha1, String commitMessage, String firstPrecedingCommitSha1, String secondPrecedingCommitSha1, CommitChangesAjaxResponse changesToFirstPrecesignCommit, CommitChangesAjaxResponse changesToSecondPrecesignCommit) {
        this.commitSha1 = commitSha1;
        this.commitMessage = commitMessage;
        this.firstPrecedingCommitSha1 = firstPrecedingCommitSha1;
        this.secondPrecedingCommitSha1 = secondPrecedingCommitSha1;
        this.changesToFirstPrecedingCommit = changesToFirstPrecesignCommit;
        this.changesToSecondPrecedingCommit = changesToSecondPrecesignCommit;
    }
}
