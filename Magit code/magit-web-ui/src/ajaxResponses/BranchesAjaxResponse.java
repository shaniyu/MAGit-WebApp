package ajaxResponses;

public class BranchesAjaxResponse {
    private String branchName;
    private String branchSha1;
    private String commitMessage;
    private boolean isHead;
    private boolean isRemote;
    private boolean isRTB;

    public BranchesAjaxResponse(String branchName, String branchSha1, String commitMessage, boolean isHead, boolean isRemote, boolean isRTB) {
        this.branchName = branchName;
        this.branchSha1 = branchSha1;
        this.commitMessage = commitMessage;
        this.isHead = isHead;
        this.isRemote = isRemote;
        this.isRTB = isRTB;
    }
}
