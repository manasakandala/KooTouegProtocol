import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.*;

public class kooToueg {

    HashMap<Integer, Node> nodeDictionary;
    int noOfNeigbhorNodes;
    int noOfNodes;
    int minDelay;
    ArrayList<ArrayList<String>> operations; // operation, id
    int id;
    boolean task;
    int taskNodeId;
    String taskType;
    int iterator;
    int[] vectorClock, backupVectorClock;
    int[] messageLabel;
    int[] lastLabelRcvd, firstLabelSent, lastLabelSent, bkLLR, bkFLS;
    LinkedList<checkPointsTaken> cPointsTaken;
    HashMap<Integer, Node> cohorts, bkpCohorts;
    boolean saveCP;
    appMessage ap;
    boolean takePermanent;
    int sentMsgCount;

    public kooToueg(int serverNumber) {
        noOfNeigbhorNodes = 0;
        iterator = 0;
        this.id = serverNumber;
        task = false;
        nodeDictionary = new HashMap<>();
        operations = new ArrayList<ArrayList<String>>();
        cPointsTaken = new LinkedList<>();
        saveCP = false;
        bkpCohorts = new HashMap<>();
        cohorts = new HashMap<>();
        takePermanent = false;
        sentMsgCount=0;
    }

    public void sendMeassage(Node neighbor, Message floodMessage) {
        try {
            Socket clientSocket = new Socket(neighbor.getHostName(), neighbor.getPort());
            ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            outputStream.writeObject(floodMessage);
            outputStream.flush();
            outputStream.close();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateVectorClock(int[] incomingVectorClock, int senderId) {
        for (int i = 0; i < noOfNodes; i++) {
            if (incomingVectorClock[i] > vectorClock[i]) {
                vectorClock[i] = incomingVectorClock[i];
            }          
        }
        vectorClock[this.id]++;
    }

    public void saveCheckPoint(int seqNumber) {
        if (seqNumber > cPointsTaken.getLast().getSeqNumber()) {
            // System.out.println("save checkpoint");
            checkPointsTaken CP = new checkPointsTaken(seqNumber, backupVectorClock);
            cPointsTaken.add(CP);
            System.out.println("_______Permanent CheckPoint Taken 3_________");

            Message perMessage = new Message(id, 5, backupVectorClock, seqNumber, iterator);
            sendPermanentMessage(perMessage);
        }
        takePermanent = false;
    }

    public void sendPermanentMessage(Message perMessage) {
        System.out.println("\nSeq No: " + perMessage.labelValue + "\nVectorClock :");
        for (int n : backupVectorClock)
            System.out.print(n + " ");
        System.out.println();

        for (int id : bkpCohorts.keySet()) {
            sendMeassage(bkpCohorts.get(id), perMessage);
        }
        bkpCohorts = new HashMap<>();
    }

    public class Checkpoint extends Thread {

        kooToueg kT;
        boolean tentativeTaken;
        int parentNodeId;

        public Checkpoint(kooToueg kT) {
            this.kT = kT;
            tentativeTaken = false;
        }

        public Checkpoint(kooToueg kT, int id) {
            this.kT = kT;
            this.parentNodeId = id;
            tentativeTaken = false;
        }

        public void checkPointMessage() {
            for (int id : kT.cohorts.keySet()) {
                int index = kT.nodeDictionary.get(kT.id).findNeighbourIndex(id);
                int label = kT.lastLabelRcvd[index];
                Message checkPointMessage = new Message(kT.id, 1, kT.vectorClock, label, kT.iterator);

                kT.sendMeassage(kT.cohorts.get(id), checkPointMessage);
            }
        }

        public void findCohorts(Node node) {
            ArrayList<Node> neibhors = kT.nodeDictionary.get(kT.id).getNodeNeigbhors();
            System.out.print("Cohorts: ");
            for (Node n : neibhors) {
                int index = node.findNeighbourIndex(n.getNodeId());
                if(parentNodeId == n.getNodeId())
                    continue;
                if (lastLabelRcvd[index] > -1) {
                    kT.cohorts.put(n.getNodeId(), n);
                    kT.bkpCohorts.put(n.getNodeId(), n);
                    System.out.print(" "+ n.getNodeId());
                }
            }
            System.out.println();
        }

        public void floodNetwork(int id, int iterator) {
            Message floodMessage = new Message(id, 3, kT.vectorClock, Integer.parseInt(kT.operations.get(kT.iterator).get(1)), kT.iterator);

            ArrayList<Node> neigbours = nodeDictionary.get(id).getNodeNeigbhors();
            for (Node n : neigbours) {
                kT.sendMeassage(n, floodMessage);
            }
        }

        public void run() {
            findCohorts(kT.nodeDictionary.get(kT.id));

            if (kT.iterator == 0 && kT.id == Integer.parseInt(kT.operations.get(0).get(1))) {
                try {
                    sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            while (!saveCP) {
                try {
                    appMessage.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (tentativeTaken == false) {
                    tentativeTaken = true;
                    checkPointMessage();

                    // Tentative CP
                    // Backup for vector Clock
                    for (int i = 0; i < vectorClock.length; i++) {
                        backupVectorClock[i] = vectorClock[i];
                    }

                    // Backup for llr, fls
                    for (int i = 0; i < lastLabelRcvd.length; i++) {
                        bkLLR[i] = lastLabelRcvd[i];
                        lastLabelRcvd[i] = -1;

                        bkFLS[i] = firstLabelSent[i];
                        firstLabelSent[i] = -1;
                    }
                    System.out.println("_________Tentative CheckPoint Taken__________");
                }

                if (cohorts.size() == 0) {
                    // if not initator of this iteration send reply to parent node
                    tentativeTaken = false;
                    saveCP = true;
                    if (this.parentNodeId != -1) {
                        if (Integer.parseInt(kT.operations.get(kT.iterator).get(1)) != kT.id) {
                            Message ackMessage = new Message(kT.id, 4, kT.vectorClock, -1, kT.iterator);
                            sendMeassage(kT.nodeDictionary.get(this.parentNodeId), ackMessage);
                        }
                    }
                }
            }

            System.out.println("\nKt Id: "+ kT.id+"\nKT iterator id: "+Integer.parseInt(kT.operations.get(kT.iterator).get(1)));
            // Permanent CP
            if (kT.id == Integer.parseInt(kT.operations.get(kT.iterator).get(1))) {
                if (cPointsTaken.size() == 0) {
                    int number = (int) (Math.random());
                    checkPointsTaken CP = new checkPointsTaken(number, backupVectorClock);
                    cPointsTaken.add(CP);
                    System.out.println("_______Permanent CheckPoint Taken 2_________");
                    takePermanent = false;
                    Message perMessage = new Message(id, 5, vectorClock, number, iterator);
                    sendPermanentMessage(perMessage);
                    kT.iterator++;
                    floodNetwork(kT.id, kT.iterator);
                } else {
                    int number = cPointsTaken.getLast().getSeqNumber();
                    saveCheckPoint(++number);
                    kT.iterator++;
                    floodNetwork(kT.id, kT.iterator);
                }
            }
            saveCP = false;
        }
    }

    public void takeCheckpoint(int id) {
        Checkpoint cp = new Checkpoint(this, id);
        cp.start();
    }
    public void startThreads() {
        new server(this).start();
        if (task == true && taskType.equals("c")) {
            takeCheckpoint(-1);
        }
        new appMessage(this).start();
        // new appControler(this).start();
    }

    public void readData() {
        try {
            File config = new File("config.txt");
            Scanner sc = new Scanner(config);
            String d = sc.nextLine();

            String[] line1 = d.split(" ");
            noOfNodes = Integer.parseInt(line1[0]);
            minDelay = Integer.parseInt(line1[1]);

            for (int i = 0; i < noOfNodes; i++) {
                String data = sc.nextLine();
                if (data == "") {
                    data = sc.nextLine();
                }

                String[] nodeDetails = data.split(" ");
                Node node = new Node(Integer.parseInt(nodeDetails[0]), nodeDetails[1],
                        Integer.parseInt(nodeDetails[2]));
                nodeDictionary.put(node.getNodeId(), node);
            }

            for (int i = 0; i < noOfNodes; i++) {
                String data = sc.nextLine();
                if (data == "") {
                    data = sc.nextLine();
                }

                String[] nodeNeigbhorsDetails = data.split(" ");
                Node currentNode = nodeDictionary.get(i);

                for (int j = 0; j < nodeNeigbhorsDetails.length; j++) {
                    int nodeID = Integer.parseInt(nodeNeigbhorsDetails[j]);
                    Node node = nodeDictionary.get(nodeID);
                    currentNode.addNodeNeigbhor(node);
                }
            }

            while (sc.hasNextLine()) {
                String data = sc.nextLine();
                if (data == "") {
                    data = sc.nextLine();
                }

                String[] op = data.split(" ");
                ArrayList<String> ops = new ArrayList<>();
                ops.add(op[1]);
                ops.add(op[3]);

                operations.add(ops);
            }

            noOfNeigbhorNodes = nodeDictionary.get(id).getNodeNeigbhors().size();
            messageLabel = new int[noOfNeigbhorNodes];
            lastLabelRcvd = new int[noOfNeigbhorNodes];
            lastLabelSent = new int[noOfNeigbhorNodes];
            firstLabelSent = new int[noOfNeigbhorNodes];
            bkLLR = new int[noOfNeigbhorNodes];
            bkFLS = new int[noOfNeigbhorNodes];
            vectorClock = new int[noOfNodes];
            backupVectorClock = new int[noOfNodes];

            sc.close();
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        }
    }

    public static void main(String args[]) {
        int serverNumber = Integer.parseInt(args[0]);
        kooToueg obj = new kooToueg(serverNumber);

        obj.readData();
        int serNum = Integer.parseInt(obj.operations.get(0).get(1));
        if (serNum == serverNumber) {
            obj.task = true;
            obj.taskNodeId = obj.id;
            obj.taskType = obj.operations.get(0).get(0);
        }

        obj.startThreads();
    }
}