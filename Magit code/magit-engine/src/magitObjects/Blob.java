package magitObjects;

public class Blob extends GitObject {

    public Blob(String mName, String mPath, String mSHA1, FolderType type, String mLastModifiedDate, String mLastModifiedBy) {
        super(mName, mPath, mSHA1, type, mLastModifiedDate, mLastModifiedBy);
    }

}
