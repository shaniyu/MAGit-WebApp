package Exceptions;

public class XmlNotValidException extends Exception{

    String message;

    public XmlNotValidException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return (message);
    }


}
