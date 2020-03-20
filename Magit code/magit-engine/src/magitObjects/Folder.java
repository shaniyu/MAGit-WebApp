package magitObjects;

public class Folder extends GitObject {

    public Folder(String mName, String mPath, String mSHA1, FolderType type, String mLastModifiedDate, String mLastModifiedBy) {
        super(mName, mPath, mSHA1, type, mLastModifiedDate, mLastModifiedBy);
    }
}
