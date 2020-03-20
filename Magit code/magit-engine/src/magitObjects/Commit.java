package magitObjects;

import DataStructures.CommitChanges;
import DataStructures.MagitCommitNode;
import puk.team.course.magit.ancestor.finder.CommitRepresentative;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

public class Commit implements CommitRepresentative {

    public static final int mainRepoSHA1 = 0;
    public static final int firstPrevCommitSHA1 = 1;
    public static final int secondPrevCommitSHA1 = 2;
    public static final int commitMessage = 3;
    public static final int lastModifiedDate = 4;
    public static final int username = 5;

    private String mSHA1;
    private String mRepoFolderSHA1;
    private String prevCommitSha1First;
    private String prevCommitSha1Second;
    private String mMessage;
    private String mCreatedDate;
    private String mCreatedBy;
    private CommitChanges commitChangesToFirstPrecedingCommit;
    private CommitChanges commitChangesToSecondPrecedingCommit;

    //This constructor is used when loading an existing repository
    //When initializing allOfTheBranches of repository we only add the SHA1 to the branch's commit
    //So we want to create a commit with SHA1 only.
    public Commit(String SHA1) {
        mSHA1 = SHA1;
        nullifyCommitChanges();
    }


    public Commit(String mSHA1, String mRepoFolderSHA1, String mPrevSHA1First, String mPrevSHA1Second, String mMessage, String mCreatedDate, String mCreatedBy) {

        this.mSHA1 = mSHA1;
        this.mRepoFolderSHA1 = mRepoFolderSHA1;
        this.prevCommitSha1First = mPrevSHA1First;
        this.prevCommitSha1Second = mPrevSHA1Second;
        this.mMessage = mMessage;
        this.mCreatedDate = mCreatedDate;
        this.mCreatedBy = mCreatedBy;
        nullifyCommitChanges();
    }

    public Commit() {
        nullifyCommitChanges();
    }


    private void nullifyCommitChanges() {
        commitChangesToFirstPrecedingCommit = null;
        commitChangesToSecondPrecedingCommit = null;
    }

    @Override
    public boolean equals(Object obj) {

        return ((obj instanceof Commit) &&
                (this.mSHA1.equals(((Commit) obj).getSha1())));

    }

    @Override
    public int hashCode() {
        return Integer.valueOf(mSHA1);
    }


    
    public String getFirstParentSha1() {
        return prevCommitSha1First;
    }

    
    public String getSecondParentSha1() {
        return prevCommitSha1Second;
    }


    public String getmCreatedDate() {
        return mCreatedDate;
    }


    public String getmMessage() {
        return mMessage;
    }

    @Override
    public String getSha1() {
        return mSHA1;
    }

    public String getmCreatedBy() {
        return mCreatedBy;
    }

    @Override
    public String getFirstPrecedingSha1() {
        return prevCommitSha1First.equals("null") ? "" : prevCommitSha1First;
    }

    @Override
    public String getSecondPrecedingSha1() {
        return prevCommitSha1Second.equals("null") ? "" : prevCommitSha1Second;
    }


    public CommitChanges getCommitChangesToFirstPrecedingCommit() {
        return commitChangesToFirstPrecedingCommit;
    }

    public CommitChanges getCommitChangesToSecondPrecedingCommit() {
        return commitChangesToSecondPrecedingCommit;
    }

    public void setCommitChangesToFirstPrecedingCommit(CommitChanges commitChangesToFirstPrecedingCommit) {
        this.commitChangesToFirstPrecedingCommit = commitChangesToFirstPrecedingCommit;
    }

    public void setCommitChangesToSecondPrecedingCommit(CommitChanges commitChangesToSecondPrecedingCommit) {
        this.commitChangesToSecondPrecedingCommit = commitChangesToSecondPrecedingCommit;
    }

    public static Comparator<Commit> commitDateComparator = new Comparator<Commit>() {
        public int compare(Commit commit1, Commit commit2) {
            try {

                // we parse the date string into a date and compare the dates
                String dateOfFirst = commit1.getmCreatedDate();
                String dateOfSecond = commit2.getmCreatedDate();
                SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");

                Date dateOfFirstDate = format.parse(dateOfFirst);
                Date dateOfSecondDate = format.parse(dateOfSecond);
                return (dateOfSecondDate.compareTo(dateOfFirstDate));
            } catch (Exception e) {
                return (commit2.getmCreatedDate().compareTo(commit1.getmCreatedDate()));
            }
        }
    };
}
