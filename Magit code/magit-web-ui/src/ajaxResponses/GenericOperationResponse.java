package ajaxResponses;

public class GenericOperationResponse {
    private boolean success;
    private String errorMessage;

    public GenericOperationResponse(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }
}
