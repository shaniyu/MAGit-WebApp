package Exceptions;

public class CommitIsNullException extends Exception {

    String  branchName;
    public CommitIsNullException(String  branchName)
    {
        this.branchName = branchName;
    }
    @Override
    public String getMessage() {
        return ("The commit in " + branchName + " is empty or null");
    }
}
