package ajaxResponses;

public class CheckoutToBranchAjaxResponse {
    private boolean successValue;
    private String errorMessage;

    public CheckoutToBranchAjaxResponse(boolean successValue, String errorMessage) {
        this.successValue = successValue;
        this.errorMessage = errorMessage;
    }
}
