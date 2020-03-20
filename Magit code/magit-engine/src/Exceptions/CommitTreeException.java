package Exceptions;

public class CommitTreeException extends Exception {
    private String message;
    public CommitTreeException(String message)
    {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return ("There is something wrong in the commits graph.\n" + message);
    }
}
