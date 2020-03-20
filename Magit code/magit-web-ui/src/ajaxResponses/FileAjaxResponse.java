package ajaxResponses;

public class FileAjaxResponse {

    private String fileName;
    private String fileFullPath;
    private String fileContent;

    public FileAjaxResponse(String fileName, String fileFullPath, String fileContent) {
        this.fileName = fileName;
        this.fileFullPath = fileFullPath;
        this.fileContent = fileContent;
    }
}
