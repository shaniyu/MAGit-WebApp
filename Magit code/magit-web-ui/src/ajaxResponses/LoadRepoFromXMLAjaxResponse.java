package ajaxResponses;

public class LoadRepoFromXMLAjaxResponse {
    private boolean successValue;
    private String errorMessage;

    public LoadRepoFromXMLAjaxResponse(boolean successValue, String errorMessage) {
        this.successValue = successValue;
        this.errorMessage = errorMessage;
    }
}
