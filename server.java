import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

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
            System.out.println(socket.toString());
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
                        updateVectorClock(incomingMessage.getVecClk());

                        System.out.println("Iteration :"+kT.iterator);
                        int[] incomingVector = incomingMessage.getVecClk();
                        System.out.println("reciver Id: " + kT.id);
                        System.out.println("sender Id: " + incomingMessage.getId());
                        System.out.println("Vector Clock: ");
                        for (int i = 0; i < incomingVector.length; i++) {
                            System.out.print(incomingVector[i] + " ");
                        }
                        System.out.println("");

                    } else if (incomingMessage.getMessageType() == 1) { // Checkpoint Message

                        System.out.println("Iteration :"+kT.iterator);
                        int[] incomingVector = incomingMessage.getVecClk();
                        System.out.println("reciver Id: " + kT.id);
                        System.out.println("sender Id: " + incomingMessage.getId());
                        System.out.println("Vector Clock: ");
                        for (int i = 0; i < incomingVector.length; i++) {
                            System.out.print(incomingVector[i] + " ");
                        }

                                          

                    } else if (incomingMessage.getMessageType() == 2) { // Recovery Message



                    } else if (incomingMessage.getMessageType() == 3) { // Flood Message

                        if(true) { //iterator's id == mine => start checkpoint/recovery

                        } else { //flood the msg to all neighbours
                            
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