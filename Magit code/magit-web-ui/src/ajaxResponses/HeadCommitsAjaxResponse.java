package ajaxResponses;

import java.util.ArrayList;

public class HeadCommitsAjaxResponse {
    private String commitMessage;
    private String commitSha1;
    private String dateOfCommit;
    private String commitCreatedBy;
    ArrayList<String> branchesPointsToThisCommit;

    public HeadCommitsAjaxResponse(String commitMessage, String commitSha1, String dateOfCommit, String commitCreatedBy, ArrayList<String> branchesPointsToThisCommit) {
        this.commitMessage = commitMessage;
        this.commitSha1 = commitSha1;
        this.dateOfCommit = dateOfCommit;
        this.commitCreatedBy = commitCreatedBy;
        this.branchesPointsToThisCommit = branchesPointsToThisCommit;
    }
}
