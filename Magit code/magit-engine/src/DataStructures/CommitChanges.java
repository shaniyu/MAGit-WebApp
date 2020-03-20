package DataStructures;

import com.sun.org.apache.xpath.internal.operations.Bool;

import java.util.ArrayList;
import java.util.List;

public class CommitChanges {

    private List<String> deletedFiles;
    private List<String> newFiles;
    private List<String> changedFiles;

    public CommitChanges() {
        this.deletedFiles = new ArrayList<>();
        this.newFiles = new ArrayList<>();
        this.changedFiles = new ArrayList<>();
    }

    public void addToNewFiles(String filePath){
        newFiles.add(filePath);
    }

    public void addToChangedFiles(String filePath){
        changedFiles.add(filePath);
    }

    public void addToDeletedFiles(String filePath){
        deletedFiles.add(filePath);
    }

    public List<String> getDeletedFiles() {
        return deletedFiles;
    }

    public List<String> getNewFiles() {
        return newFiles;
    }

    public List<String> getChangedFiles() {
        return changedFiles;
    }

    public Boolean isFileInTheListsUnion(String fileRelativePath)
    {
        return (deletedFiles.contains(fileRelativePath) || newFiles.contains(fileRelativePath) || changedFiles.contains(fileRelativePath));
    }
}
