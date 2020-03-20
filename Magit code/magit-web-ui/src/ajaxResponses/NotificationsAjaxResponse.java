package ajaxResponses;

import notifications.Message;

import java.util.ArrayList;

public class NotificationsAjaxResponse {
    private ArrayList<Message> allNewMessages;
    private int version;

    public NotificationsAjaxResponse(ArrayList<Message> allNewMessages, int version) {
        this.allNewMessages = allNewMessages;
        this.version = version;
    }
}
