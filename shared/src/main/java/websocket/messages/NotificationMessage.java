package websocket.messages;

import java.util.Objects;

public class NotificationMessage extends ServerMessage {
    private final String notification;

    public NotificationMessage(ServerMessageType type, String notification) {
        super(type);
        this.notification = notification;
    }


    public String getNotification() {
        return notification;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        NotificationMessage that = (NotificationMessage) o;
        return Objects.equals(getNotification(), that.getNotification());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getNotification());
    }
}
