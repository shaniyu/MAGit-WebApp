package magitEngine;

public class TaskValue {

    private boolean taskSuccess;
    private Exception exception;

    public boolean isTaskSuccess() {
        return taskSuccess;
    }

    public void setTaskSuccess(boolean taskSuccess) {
        this.taskSuccess = taskSuccess;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
