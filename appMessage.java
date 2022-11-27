import java.io.IOException;
import java.lang.Math;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.io.ObjectOutputStream;

public class appMessage extends Thread{

    kooToueg kT;
    public appMessage(kooToueg kT) {
        this.kT = kT;
    }

    public void updateFLS(int randNeigbhorIndex, int label) {
        if(kT.firstLabelSent[randNeigbhorIndex] == -1) {
            kT.firstLabelSent[randNeigbhorIndex] = label;
        }

        kT.lastLabelSent[randNeigbhorIndex] = label;
    }

    public void sendApplicationMessage() {
        // try {
        //     sleep(5000);
        // } catch (InterruptedException e1) {
        //     e1.printStackTrace();
        // }
        ArrayList<Node> nodeNeigbhors = kT.nodeDictionary.get(kT.id).getNodeNeigbhors();
        int randNeigbhorIndex = (int)(Math.random() * nodeNeigbhors.size());
        Node randNeigbhor = nodeNeigbhors.get(randNeigbhorIndex);

        int label = ++kT.messageLabel[randNeigbhorIndex];
        Message newAppMessage = new Message(kT.id, 0, kT.vectorClock, label, -1);
        updateFLS(randNeigbhorIndex, label);

        try {
            Socket clientSocket = new Socket(randNeigbhor.getHostName(), randNeigbhor.getPort());
            ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
            output.writeObject(newAppMessage);
            output.flush();
            output.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            sleep(3000);
        } catch(Exception exception) {
            exception.printStackTrace();
        }

        while(kT.iterator < kT.operations.size()) {
            try {
                sleep(1000);
            } catch(Exception exception) {
                exception.printStackTrace();
            }

            sendApplicationMessage();
        }
    }
}
