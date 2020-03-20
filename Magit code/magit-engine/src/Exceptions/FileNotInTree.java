package Exceptions;



public class FileNotInTree extends Exception{

    private String filePath;
    public FileNotInTree(String filePath)
    {
        this.filePath = filePath;
    }
    @Override
    public String getMessage() {
        return ("The file in path: " + filePath + " wasn't found in the gitObject tree");
    }
}
