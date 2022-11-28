import java.io.EOFException;
import java.io.ObjectInputStream;
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

    public void run() {
        int port = kT.nodeDictionary.get(kT.id).getPort();

        ServerSocket serverSocket = null;
        Socket socket = null;
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
                    if (incomingMessage.getMessageType() == 0) { // Application Message
                        System.out.println("Message Type: " + incomingMessage.getMessageType() + " From: "+ incomingMessage.getId());
                        int neighbourIndex = kT.nodeDictionary.get(kT.id).findNeighbourIndex(senderId);

                        updateLLR(neighbourIndex, incomingMessage.getLabelValue());
                        kT.updateVectorClock(incomingMessage.getVecClk(), incomingMessage.getId());

                    } else if (incomingMessage.getMessageType() == 1) { // Checkpoint Message
                        if (kT.takePermanent == false) {
                            kT.takePermanent = true;
                            int[] incomingVector = incomingMessage.getVecClk();
                            int index = kT.nodeDictionary.get(kT.id).findNeighbourIndex(incomingMessage.id);
                            if (incomingMessage.labelValue >= kT.firstLabelSent[index]
                                    && kT.firstLabelSent[index] > -1) {
                                kT.takeCheckpoint(incomingMessage.id);
                                continue;
                            }

                            Message ackMessage = new Message(kT.id, 4, incomingVector, -1, kT.iterator);
                            kT.sendMeassage(kT.nodeDictionary.get(incomingMessage.id), ackMessage);
                        } else {
                            Message ackMessage = new Message(kT.id, 4, incomingMessage.vectorClock, -1, kT.iterator);
                            kT.sendMeassage(kT.nodeDictionary.get(incomingMessage.id), ackMessage);
                        }
                    } else if (incomingMessage.getMessageType() == 2) { // Recovery Message

                    } else if (incomingMessage.getMessageType() == 3) { // Flood Message
                        int itr = incomingMessage.getIterator();
                        if (kT.iterator < itr) {
                            kT.iterator = itr;
                            int opNodeId = Integer.parseInt(kT.operations.get(itr).get(1));
                            if (kT.id == incomingMessage.getLabelValue()) { // iterator's id == mine => start
                                System.out.println("Need to start operation");
                                for (long stop = System.currentTimeMillis() + kT.minDelay; stop > System
                                        .currentTimeMillis();)
                                    ;
                                if (kT.operations.get(incomingMessage.iterator).get(0).equals("c")) {
                                    kT.takeCheckpoint(opNodeId);
                                } else {
                                    // recovery operation start
                                }

                            } else { // flood the msg to all neighbours
                                ArrayList<Node> neighbors = kT.nodeDictionary.get(kT.id).getNodeNeigbhors();
                                for (Node n : neighbors) {
                                    if (n.getNodeId() != incomingMessage.id)
                                        kT.sendMeassage(n, incomingMessage);
                                }
                            }
                        }

                    } else if (incomingMessage.getMessageType() == 4) { // Ack Message

                        int nodeId = incomingMessage.getId();
                        kT.cohorts.remove(nodeId);
                    } else if (incomingMessage.getMessageType() == 5) { // Permanent CP
                        synchronized(kT.cPointsTaken) {   
                            kT.takePermanent = false;
                            int seqNumber = incomingMessage.getLabelValue();
                            if (kT.id == Integer.parseInt(kT.operations.get(kT.iterator).get(1))) {
                                continue;
                            }
                            if (kT.cPointsTaken.size() == 0) {
                                checkPointsTaken CP = new checkPointsTaken(seqNumber, kT.backupVectorClock);
                                kT.cPointsTaken.add(CP);
                                System.out.println("_______Permanent CheckPoint Taken 1_________");

                                Message perMessage = new Message(kT.id, 5, kT.vectorClock, seqNumber, kT.iterator);
                                kT.sendPermanentMessage(perMessage);
                            } else {
                                // int number = kT.cPointsTaken.getLast().getSeqNumber();
                                kT.saveCheckPoint(seqNumber);

                            }
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