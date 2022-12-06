import java.io.IOException;
import java.lang.Math;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.io.ObjectOutputStream;

public class appMessage extends Thread{

    kooToueg kT;
    public appMessage(kooToueg kT) {
        this.kT = kT;
    }

    public void updateFLS(int randNeigbhorIndex, int label) {
        synchronized(kT.firstLabelSent) {
            if(kT.firstLabelSent[randNeigbhorIndex] == -1) {
                kT.firstLabelSent[randNeigbhorIndex] = label;
            }

            kT.lastLabelSent[randNeigbhorIndex] = label;
        }
    }

    public void sendApplicationMessage() {
        ArrayList<Node> nodeNeigbhors = kT.nodeDictionary.get(kT.id).getNodeNeigbhors();
        int randValue = (int)(Math.random() * nodeNeigbhors.size());
        Node randNeigbhor = nodeNeigbhors.get(randValue);
        int randNeigbhorIndex = kT.nodeDictionary.get(kT.id).findNeighbourIndex(randNeigbhor.getNodeId());

        int label = ++kT.messageLabel[randNeigbhorIndex];
        kT.vectorClock[kT.id]++;
        Message newAppMessage = new Message(kT.id, 0, kT.vectorClock, label, -1, -1);
        updateFLS(randNeigbhorIndex, label);

        try {
            ObjectOutputStream output = kT.outputMap.get(randNeigbhor.getNodeId());
            output.writeObject(newAppMessage);
            output.flush();
        } catch (UnknownHostException e) {
            
        } catch (IOException e) {
            
        }
    }

    public void run() {
        try {
            sleep(3000);
        } catch(Exception exception) {
            
        }

        while(kT.iterator < kT.operations.size()) {
            try {
                sleep(1000);
            } catch(Exception exception) {
                
            }

            sendApplicationMessage();
        }
    }
}
