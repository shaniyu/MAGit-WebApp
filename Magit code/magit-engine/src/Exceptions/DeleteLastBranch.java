package Exceptions;

public class DeleteLastBranch extends Exception {
    String branchName;

    public DeleteLastBranch(String name){
        branchName = name;
    }
}
