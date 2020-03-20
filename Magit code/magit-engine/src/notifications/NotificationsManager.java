package notifications;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NotificationsManager {
    private ArrayList<Message> messages;
    private int versionOfUser;

    public NotificationsManager() {
        versionOfUser = 0; //  is set to zero on user account creation
        this.messages = new ArrayList<>();
        // add a welcoming message from magit boss
        this.messages.add(new Message("Magit boss", "", "Welcome to magit!\nHere are all your notifications", new Date()));
        }
    // this class manages the notifications, add message, remove et'

    public synchronized void addMessage(Message message) {
        messages.add(message);
    }

    public synchronized List<Message> getNotifications(int fromIndex){
        if (fromIndex < 0 || fromIndex > messages.size()) {
            fromIndex = 0;
        }


        return messages.subList(fromIndex, messages.size());
    }

    public int getVersion() {
        return messages.size();
    }

    // get the version of client, means the version that the client has seen already
    public int getVersionOfUser() {
        return versionOfUser;
    }

    public void setVersionOfUser(int versionOfUser) {
        this.versionOfUser = versionOfUser;
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }
}
