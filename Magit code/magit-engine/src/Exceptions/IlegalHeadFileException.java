package Exceptions;

public class IlegalHeadFileException extends Exception{
    String headFilePath;
    public IlegalHeadFileException(String headFilePath)
    {
        this.headFilePath = headFilePath;
    }
    @Override
    public String getMessage() {
        return ("The HEAD file in: " + headFilePath + " doesn't include a valid branch name.");
    }
}
