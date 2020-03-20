package magitObjects;

import java.util.Objects;

public class GitObject {

    private String mName; // only file name
    private String mPath; // name of the directory of the repo + file seperator + name of file (not full path!)
    private String mSHA1;
    private FolderType mFolderType;
    private String mLastModifiedBy;
    private String mLastModifiedDate;
    private Boolean wasChecked;


    public static final int fileName = 0;
    public static final int SHA1 = 1;
    public static final int type = 2;
    public static final int lastModifiedDate = 3;
    public static final int username = 4;


    public String getmName() {
        return mName;
    }

    public GitObject(String mName, String mPath, String mSHA1, FolderType type, String mLastModifiedDate, String mLastModifiedBy) {
        this.mName = mName;
        this.mSHA1 = mSHA1;
        this.mFolderType = type;
        this.mLastModifiedBy = mLastModifiedBy;
        this.mLastModifiedDate = mLastModifiedDate;
        this.wasChecked = false;
        this.mPath = mPath;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public String getmSHA1() {
        return mSHA1;
    }

    public void setmSHA1(String mSHA1) {
        this.mSHA1 = mSHA1;
    }

    public FolderType getmFolderType() {
        return mFolderType;
    }

    public void setmFolderType(FolderType mFolderType) {
        this.mFolderType = mFolderType;
    }

    public String getmLastModifiedBy() {
        return mLastModifiedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GitObject)) return false;
        GitObject gitObject = (GitObject) o;
        return Objects.equals(mName, gitObject.mName) &&
                mSHA1.equals(gitObject.mSHA1) &&
                mFolderType == gitObject.mFolderType;

    }

    @Override
    public int hashCode() {
        return Objects.hash(mName, mSHA1, mFolderType);
    }

    public void setmLastModifiedBy(String mLastModifiedBy) {
        this.mLastModifiedBy = mLastModifiedBy;
    }

    public String getmLastModifiedDate() {
        return mLastModifiedDate;
    }

    public void setmLastModifiedDate(String mLastModifiedDate) {
        this.mLastModifiedDate = mLastModifiedDate;
    }

    public Boolean getWasChecked() {
        return wasChecked;
    }

    public void setWasChecked(Boolean wasChecked) {
        this.wasChecked = wasChecked;
    }

    public String getmPath() {
        return mPath;
    }

    public void setmPath(String mPath) {
        this.mPath = mPath;
    }
}
