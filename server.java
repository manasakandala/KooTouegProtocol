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

                        int neighbourIndex = kT.nodeDictionary.get(kT.id).findNeighbourIndex(senderId);
                        updateLLR(neighbourIndex, incomingMessage.getLabelValue());
                        kT.updateVectorClock(incomingMessage.getVecClk(), incomingMessage.getId());

                    } else if (incomingMessage.getMessageType() == 1) { // Checkpoint Message
                    System.out.println("Message Type: " + incomingMessage.getMessageType() + " From: "+ incomingMessage.getId());
                        if (kT.takePermanent == false) {
                            kT.takePermanent = true;
                            int[] incomingVector = incomingMessage.getVecClk();
                            int index = kT.nodeDictionary.get(kT.id).findNeighbourIndex(incomingMessage.id);
                            if (incomingMessage.labelValue >= kT.firstLabelSent[index]
                                    && kT.firstLabelSent[index] > -1) {
                                kT.takeCheckpoint(incomingMessage.id);
                                continue;
                            }

                            Message ackMessage = new Message(kT.id, 4, incomingVector, -1, kT.iterator, -1);
                            kT.sndMsg.sendMeassage(kT.nodeDictionary.get(incomingMessage.id), ackMessage);
                        } else {
                            Message ackMessage = new Message(kT.id, 4, incomingMessage.vectorClock, -1, kT.iterator, -1);
                            kT.sndMsg.sendMeassage(kT.nodeDictionary.get(incomingMessage.id), ackMessage);
                        }
                    } else if (incomingMessage.getMessageType() == 2) { // Recovery Message
                    System.out.println("Message Type: " + incomingMessage.getMessageType() + " From: "+ incomingMessage.getId());
                        if (kT.recItr < incomingMessage.getRecIterator()) { 
                            kT.recItr = incomingMessage.getRecIterator();
                            //induced recovery case
                            int nodeId = incomingMessage.getId();
                            int index = kT.nodeDictionary.get(kT.id).findNeighbourIndex(nodeId);
                            int LLRfromi = kT.lastLabelRcvd[index];
                            System.out.println("LLR[incomingNode]: "+LLRfromi+"\nLabel: "+incomingMessage.labelValue);
                            if(LLRfromi > incomingMessage.labelValue) {
                                kT.performRecovery(incomingMessage, incomingMessage.getId());
                            }
                        }

                    } else if (incomingMessage.getMessageType() == 3) { // Flood Message
                    System.out.println("Message Type: " + incomingMessage.getMessageType() + " From: "+ incomingMessage.getId());
                        int itr = incomingMessage.getIterator();
                        if (kT.iterator < itr) {
                            kT.iterator = itr;
                            int opNodeId = Integer.parseInt(kT.operations.get(itr).get(1));
                            if (kT.id == incomingMessage.getLabelValue()) { // iterator's id == mine => start
                                
                                System.out.println("Need to start operation");
                                // for (long stop = System.currentTimeMillis() + kT.minDelay; stop > System.currentTimeMillis(););

                                if (kT.operations.get(incomingMessage.iterator).get(0).equals("c")) {
                                    kT.takeCheckpoint(opNodeId);
                                } else {
                                    //Recovery from msg.
                                    kT.recItr++;
                                    kT.performRecovery(incomingMessage, -1);
                                }

                            } else { // flood the msg to all neighbours
                                ArrayList<Node> neighbors = kT.nodeDictionary.get(kT.id).getNodeNeigbhors();
                                for (Node n : neighbors) {
                                    if (n.getNodeId() != incomingMessage.id)
                                        kT.sndMsg.sendMeassage(n, incomingMessage);
                                }
                            }
                        }

                    } else if (incomingMessage.getMessageType() == 4) { // Ack Message

                    System.out.println("Message Type: " + incomingMessage.getMessageType() + " From: "+ incomingMessage.getId());
                        int nodeId = incomingMessage.getId();
                        kT.cohorts.remove(nodeId);
                    } else if (incomingMessage.getMessageType() == 5) { // Permanent CP
                    System.out.println("Message Type: " + incomingMessage.getMessageType() + " From: "+ incomingMessage.getId());
                        synchronized(kT.cPointsTaken) {   
                            kT.takePermanent = false;
                            int seqNumber = incomingMessage.getLabelValue();
                            if (kT.id == Integer.parseInt(kT.operations.get(kT.iterator).get(1))) {
                                continue;
                            }
                            if (kT.cPointsTaken.size() == 0) {
                                checkPointsTaken CP = new checkPointsTaken(seqNumber, kT.backupVectorClock);
                                kT.cPointsTaken.add(CP);
                                Message perMessage = new Message(kT.id, 5, kT.vectorClock, seqNumber, kT.iterator, -1);
                                kT.sendPermanentMessage(perMessage);
                            } else {
                                // int number = kT.cPointsTaken.getLast().getSeqNumber();
                                kT.saveCheckPoint(seqNumber);

                            }
                        }
                    } else if (incomingMessage.getMessageType() == 6)  { // Terminate
                        ArrayList<Node> neighbors = kT.nodeDictionary.get(kT.id).getNodeNeigbhors();
                        for (Node n : neighbors) {
                            if (n.getNodeId() != incomingMessage.id)
                                kT.sndMsg.sendMeassage(n, incomingMessage);
                        }
                        kT.closeClient();
                    } 
                } catch (EOFException ex) {

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}