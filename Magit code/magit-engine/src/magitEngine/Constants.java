package magitEngine;

import java.io.File;

public class Constants {
    public static final String MAGIT_PATH = "C:" + File.separator + "magit-ex03";
    public static final String USERNAME = "username";
    public static final String USER_TO_FORK = "usernameToFork";
    public static final String REPO_TO_FORK = "repoNameToFork";
    public static final String REMOTE_REPO_USERNAME = "remoteRepoUsername";

    public static final String BRANCH_NAME = "branchName";
    public static final String REPO_NAME = "repositoryName";
    public static final String REQUEST_TYPE = "requestType";
    public static final String NOTIFICATIONS_VERSION = "notificationsVersion";
    public static final String COMMIT_SHA1 = "commitSha1";
    public static final String COMMIT_MESSAGE = "commitMessage";
    public static final String FILE_PATH = "filePath";
    public static final String FILE_CONTENT = "fileContent";

    //Requests for UserPage Servlet
    public static final String GET_REPOS_REQUEST = "getRepos";
    public static final String GET_USERS_REQUEST = "getUsers";
    public static final String GET_USER_NOTIFICATIONS = "getNotifications";
    public static final String GET_USER_NOTIFICATIONS_VERSION = "getNotificationsVersion";
    public static final String GET_USER_NOTIFICATIONS_VERSION_CLIENT = "getNotificationsVersionInClient";
    public static final String SET_USER_NOTIFICATIONS_VERSION_CLIENT = "setNotificationsVersionInClient";


    //Requests of Repository Servlet
    public static final String GET_BRANCHES_INFO_REQUEST = "getBranchesInfo";
    public static final String DELETE_BRANCH_REQUEST = "deleteBranch";
    public static final String GET_WC_INFO_REQUEST = "getWCInfo";
    public static final String GET_COMMITS_INFO_REQUEST = "getCommitsInfo";
    public static final String CHECKOUT_TO_BRANCH_REQUEST = "checkoutToBranch";
    public static final String HANDLE_RTB_REQUEST = "handleRTB";
    public static final String OPEN_CHANGES_ON_CHECKOUT_REQUEST = "handleOpenChangesOnCheckout";
    public static final String ADD_NEW_BRANCH = "addNewBranch";

    //Requests for EditWC Servlet
    public static final String EDIT_FILE = "editFile";
    public static final String DELETE_FILE = "deleteFile";
    public static final String ADD_NEW_FILE = "addNewFile";
    public static final String GET_OPEN_CHANGES = "getOpenChanges";

    //Requests for pull request servlet
    public static final String PUSH = "pushBranchToRemote";
    public static final String BRANCH_TO_PUSH = "branchToPush";
    public static final String TARGET_BRANCH = "targetBranch";
    public static final String BASE_BRANCH = "baseBranch";
    public static final String CREATE_PR = "createPullRequest";
    public static final String CONFIRM_PR = "confirmPullRequest";
    public static final String DECLINE_PR = "declinePullRequest";
    public static final String PR_MESSAGE = "pullRequestMessage";
    public static final String GET_PULL_REQUESTS = "getPullRequests";
    public static final String DECLINE_MESSAGE = "declineMessage";


}
