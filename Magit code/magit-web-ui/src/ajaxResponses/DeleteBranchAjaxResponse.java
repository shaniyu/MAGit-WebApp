package ajaxResponses;

public class DeleteBranchAjaxResponse {
    private boolean successValue;
    private String errorMessage;

    public DeleteBranchAjaxResponse(boolean successValue, String errorMessage) {
        this.successValue = successValue;
        this.errorMessage = errorMessage;
    }
}
