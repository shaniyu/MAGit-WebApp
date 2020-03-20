package ajaxResponses;

public class RepositoryAjaxResponse {
    private String repositoryName;
    private String activeBranchName;
    private int branchesNumber;
    private String dateOfLastChange;
    private String lastCommitMessage;
    private String remoteRepoPath;
    private String remoteRepoName;
    private String remoteRepoUserName;

    public RepositoryAjaxResponse(String repositoryName, String activeBranchName, int branchesNumber, String dateOfLastChange, String lastCommitMessage, String remoteRepoPath, String remoteRepoName, String remoteRepoUserName) {
        this.repositoryName = repositoryName;
        this.activeBranchName = activeBranchName;
        this.branchesNumber = branchesNumber;
        this.dateOfLastChange = dateOfLastChange;
        this.lastCommitMessage = lastCommitMessage;
        this.remoteRepoPath = remoteRepoPath;
        this.remoteRepoName = remoteRepoName;
        this.remoteRepoUserName = remoteRepoUserName;
    }
}
