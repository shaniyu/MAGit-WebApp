package magitEngine;

import javafx.scene.Scene;

public class XmlStrings {
    public static final String DUPLICATE_BLOB_ID = "There are multiple blobs with id ";
    public static final String DUPLICATE_FOLDER_ID = "There are multiple folders with id ";
    public static final String FOLDER_HOLDS_ID_THAT_DOESNT_EXIST1 = "Folder with id ";
    public static final String FOLDER_HOLDS_ID_THAT_DOESNT_EXIST2 = " holds an item id that doesn't exist ";
    public static final String DUPLICATE_COMMIT_ID = "There are multiple commits with id ";
    public static final String COMMIT_POINTS_TO_NON_ROOT_FOLDER1 = "Commit with id ";
    public static final String COMMIT_POINTS_TO_NON_ROOT_FOLDER2 = " points to non root folder";
    public static final String COMMIT_POINTS_TO_FOLDER_THAT_DOESNT_EXIST1 =  "Commit with id ";
    public static final String COMMIT_POINTS_TO_FOLDER_THAT_DOESNT_EXIST2 = " point to folder that doesn't exist";
    public static final String HEAD_POINTS_TO_BRANCH_THAT_DOESNT_EXIST = "Head points to branch that doesn't exist";
    public static final String BRANCH_POINTS_TO_COMMIT_THAT_DOESNT_EXIST1 = "Branch ";
    public static final String BRANCH_POINTS_TO_COMMIT_THAT_DOESNT_EXIST2 = " points to commit that doesn't exist";
    public static final String HEAD_EMPTY_COMMIT ="head branch doesn't point to any commit\nhead branch can point to empty commit only in an empty repository";
    public static final String FOLDER_HOLDS_ITSELF1 = "Folder with id ";
    public static final String FOLDER_HOLDS_ITSELF2 = " holds itself";
    public static final String TRACKING_BRANCH_NOT_POINT_TO_EXISTING_REMOTE_BRANCH = " doesn't point to an existing remote branch";
    public static final String REMOTE_REPOSITORY_ISNT_REPOSITORY1 = "The remote repository in location ";
    public static final String REMOTE_REPOSITORY_ISNT_REPOSITORY2 = " isn't a magit repository";
    public static final String REMOTE_REPOSITORY_DOESNT_EXIST = " doesn't exist";

}
