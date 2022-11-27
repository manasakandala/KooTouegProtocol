import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class server extends Thread {

    kooToueg kT;

    public server(kooToueg kT) {
        this.kT = kT;
    }

    public void updateLLR(int id, int labelValue) {
        kT.lastLabelRcvd[id] = labelValue;
    }

    public void updateVectorClock(int[] vectorClock) {
        for (int i = 0; i < kT.noOfNodes; i++) {
            if (i == kT.id) {
                if (vectorClock[i] > kT.vectorClock[i]) {
                    kT.vectorClock[i] = vectorClock[i] + 1;
                } else {
                    kT.vectorClock[i]++;
                }
            } else {
                kT.vectorClock[i] = vectorClock[i];
            }
        }
    }

    public void run() {
        String host = kT.nodeDictionary.get(kT.id).getHostName();
        int port = kT.nodeDictionary.get(kT.id).getPort();

        ObjectInputStream inputStream = null;
        ObjectOutputStream outputStream = null;
        ServerSocket serverSocket = null;
        Socket socket = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("server started");
            while (true) {
                socket = serverSocket.accept();
                new Thread(new MessageProcessingThread(socket)).start();
            }

        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    class MessageProcessingThread implements Runnable {
        ObjectInputStream inputStream;
        Socket socket;

        public MessageProcessingThread(Socket socket) {
            this.socket = socket; 
            try {
                inputStream = new ObjectInputStream(socket.getInputStream());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Message incomingMessage = (Message) inputStream.readObject();
                    int senderId = incomingMessage.getId();
                    System.out.println("Message Type: "+incomingMessage.getMessageType()+" From: "+incomingMessage.getId());
                    if (incomingMessage.getMessageType() == 0) { // Application Message

                        int neighbourIndex = kT.nodeDictionary.get(kT.id).findNeighbourIndex(senderId);
                        updateLLR(neighbourIndex, incomingMessage.getLabelValue());
                        updateVectorClock(incomingMessage.getVecClk());

                    } else if (incomingMessage.getMessageType() == 1) { // Checkpoint Message

                        int[] incomingVector = incomingMessage.getVecClk();
                        int index = kT.nodeDictionary.get(kT.id).findNeighbourIndex(incomingMessage.id);
                        if(incomingMessage.labelValue >= kT.firstLabelSent[index] && kT.firstLabelSent[index] > -1) {
                            kT.takeCheckpoint(incomingMessage.id);  
                            continue;
                        } 
                        
                        Message ackMessage = new Message(kT.id, 4, incomingVector, -1, kT.iterator);
                        kT.sendMeassage(kT.nodeDictionary.get(incomingMessage.id), ackMessage);

                    } else if (incomingMessage.getMessageType() == 2) { // Recovery Message



                    } else if (incomingMessage.getMessageType() == 3) { // Flood Message
                        int itr = incomingMessage.getIterator();
                        int opNodeId = Integer.parseInt(kT.operations.get(itr).get(1));
                        if(kT.id == opNodeId) { //iterator's id == mine => start checkpoint/recovery
                            if(incomingMessage.iterator > kT.iterator) {
                                sleep(kT.minDelay);
                                kT.takeCheckpoint(incomingMessage.id);
                            }
                        } else { //flood the msg to all neighbours
                            ArrayList<Node> neighbors = kT.nodeDictionary.get(kT.id).getNodeNeigbhors();
                            for(Node n: neighbors) {
                                if(n.getNodeId() != incomingMessage.id)
                                    kT.sendMeassage(n, incomingMessage);
                            }
                        }

                    } else if(incomingMessage.getMessageType() == 4) { //Ack Message

                        int nodeId = incomingMessage.getId();
                        kT.cohorts.remove(nodeId);

                    } else if(incomingMessage.getMessageType() == 5) { //Permanent CP
                        // kT.saveCheckPoint(incomingMessage.labelValue);
                        if(kT.cPointsTaken.size() == 0) {
                            int number = incomingMessage.getLabelValue();
                            checkPointsTaken CP = new checkPointsTaken( number, kT.vectorClock);
                            kT.cPointsTaken.add(CP);
                            
                            Message perMessage = new Message(kT.id, 5, kT.vectorClock, number, kT.iterator);
                            kT.sendPermanentMessage(perMessage);
                        } else {
                            int number = kT.cPointsTaken.getLast().getSeqNumber();
                            kT.saveCheckPoint(number);
                        }
                    }
                } catch (EOFException ex) {

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        }
    }
}