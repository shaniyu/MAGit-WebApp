package magitObjects;

public enum FolderType {

    FOLDER,
    FILE;

    public static FolderType toFolderType(String string){

        return string.equals("file") ? FILE : FOLDER;
    }
}
