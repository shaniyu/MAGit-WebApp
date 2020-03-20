package ajaxResponses;

import java.util.ArrayList;
import java.util.List;

public class CommitChangesAjaxResponse {

    private List<FileAjaxResponse> deletedFiles;
    private List<FileAjaxResponse> newFiles;
    private List<FileAjaxResponse> changedFiles;

    public CommitChangesAjaxResponse() {
        this.deletedFiles = new ArrayList<>();
        this.newFiles = new ArrayList<>();
        this.changedFiles = new ArrayList<>();
    }

    public void addToNewFiles(FileAjaxResponse filePath){
        newFiles.add(filePath);
    }

    public void addToChangedFiles(FileAjaxResponse filePath){
        changedFiles.add(filePath);
    }

    public void addToDeletedFiles(FileAjaxResponse filePath){
        deletedFiles.add(filePath);
    }

    public List<FileAjaxResponse> getDeletedFiles() {
        return deletedFiles;
    }

    public List<FileAjaxResponse> getNewFiles() {
        return newFiles;
    }

    public List<FileAjaxResponse> getChangedFiles() {
        return changedFiles;
    }
}
