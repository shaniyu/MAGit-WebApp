package ajaxResponses;

public class EditWCAjaxResponse {

    private boolean successValue;
    private String errorMessage;

    public EditWCAjaxResponse(boolean successValue, String errorMessage) {
        this.successValue = successValue;
        this.errorMessage = errorMessage;
    }
}
