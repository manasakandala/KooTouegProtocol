import java.io.Serializable;

public class Message implements Serializable {
    int id;
    int messageType; // 0 - Application Msg, 1 - CP Msg, 2 - Recovery Msg, 3 - Flood Msg 4 - Ack 5 - Permanent CP 6 - Terminate
    int[] vectorClock;
    int labelValue;
    int iterator;
    int recIterator;

    public Message() {
        
    }

    public Message(int id, int messageType, int[] vectorClock, int labelValue, int iterator, int recIterator) {
        this.id = id;
        this.messageType = messageType;
        this.vectorClock = vectorClock;
        this.labelValue = labelValue;
        this.iterator = iterator;
        this.recIterator = recIterator;
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

    public int getIterator() {
        return iterator;
    }

    public void setIterator(int iterator) {
        this.iterator = iterator;
    }

    public int getRecIterator() {
        return recIterator;
    }

    public void setRecIterator(int recIterator) {
        this.recIterator = recIterator;
    }

}
