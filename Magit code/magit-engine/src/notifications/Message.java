package notifications;


import java.text.SimpleDateFormat;
import java.util.Date;

public class Message {
    private String usernameFrom;
    private String repositoryName;
    private String messageDate;
    protected String messageContent;

    public Message(String usernameFrom, String repositoryName, String messageContent, Date messageDate) {

        this.usernameFrom = usernameFrom;
        this.repositoryName = repositoryName;
        this.messageContent = messageContent;
        // use simple date format to show nicer date string in the notifications area
        String pattern = "dd/MM/yyyy HH:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String dateInFormat = simpleDateFormat.format(messageDate);

        this.messageDate = dateInFormat;
    }


}
