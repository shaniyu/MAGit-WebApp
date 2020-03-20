package notifications;

import magitEngine.PullRequest;

import java.util.Date;

public class PLMessage extends Message {
    private int PRid;
    private PRStatus messageStatus;
    private String targetBranch;
    private String baseBranch;

    public PLMessage(int PRid, PRStatus messageStatus, String message, String username,
                     String repoName, Date date, String targetBranch, String baseBranch)
    {
        // initialize parent
        super(username, repoName, message, date);

        this.PRid = PRid;
        this.messageStatus = messageStatus;
        this.targetBranch = targetBranch;
        this.baseBranch = baseBranch;
    }
}
