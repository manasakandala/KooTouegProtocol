import java.io.Serializable;

public class Message implements Serializable {
    int id;
    int messageType;
    int[] vectorClock;
    int labelValue;

    public Message() {
        
    }

    public Message(int id, int messageType, int[] vectorClock, int labelValue) {
        this.id = id;
        this.messageType = messageType;
        this.vectorClock = vectorClock;
        this.labelValue = labelValue;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public void setVecClk(int[] vectorClock) {
        this.vectorClock = vectorClock;
    }

    public void setLabelValue(int labelValue) {
        this.labelValue = labelValue;
    }

    public int getId() {
        return id;
    }

    public int getMessageType() {
        return messageType;
    }

    public int[] getVecClk() {
        return vectorClock;
    }

    public int getLabelValue() {
        return labelValue;
    }

}
