package Exceptions;

public class BranchAlreadyInUseException extends Exception {
    private String branchName;

    public BranchAlreadyInUseException(String branchName)
    {
        this.branchName = branchName;
    }

    @Override
    public String getMessage() {
        return "The branch " + this.branchName + " already exists.\nCan't create it.";
    }
}
