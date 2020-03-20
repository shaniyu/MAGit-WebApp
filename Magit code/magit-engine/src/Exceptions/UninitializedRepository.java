package Exceptions;

public class UninitializedRepository extends Exception {
    @Override
    public String getMessage() {
        return "This is not a magit repository.";
    }
}
