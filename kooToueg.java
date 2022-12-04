import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class kooToueg {

    HashMap<Integer, Node> nodeDictionary, cohorts, bkpCohorts;
    int noOfNeigbhorNodes, noOfNodes, minDelay, id, taskNodeId, iterator, recItr, sentMsgCount;
    ArrayList<ArrayList<String>> operations; // operation, id
    boolean task, saveCP, takePermanent;
    String taskType;
    int[] iterationsTaken, vectorClock, backupVectorClock, backupPerVectorClock, messageLabel, lastLabelRcvd, firstLabelSent, lastLabelSent, bkLLR, bkFLS, bkLLS;
    LinkedList<checkPointsTaken> cPointsTaken;
    appMessage ap;
    sendMessage sndMsg;
    appMessage appMsg;

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
        sentMsgCount = 0;
        recItr = 0;
        sndMsg = new sendMessage();
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
            checkPointsTaken CP = new checkPointsTaken(seqNumber, backupVectorClock);
            cPointsTaken.add(CP);
            Message perMessage = new Message(id, 5, backupVectorClock, seqNumber, iterator, -1);
            sendPermanentMessage(perMessage);
        }
        takePermanent = false;
    }

    public void sendPermanentMessage(Message perMessage) {

        System.out.println("\nPermanent CheckPoint Taken\n");
        for (int i = 0; i < lastLabelSent.length; i++) {
            bkLLS[i] = lastLabelSent[i];
        }
        for (int i = 0; i < vectorClock.length; i++) {
            backupPerVectorClock[i] = backupVectorClock[i];
        }

        for (int n : backupVectorClock)
            System.out.print(n + " ");
        System.out.println();

        for (int id : bkpCohorts.keySet()) {
            sndMsg.sendMeassage(bkpCohorts.get(id), perMessage);
        }
        bkpCohorts = new HashMap<>();
    }

    public void floodNetwork(int id, int iterator) {
        Message message;
        
        if(iterator>=operations.size()) {
            message = new Message(id, 6, vectorClock, -1, iterator, -1);
            ArrayList<Node> neigbours = nodeDictionary.get(id).getNodeNeigbhors();
            for (Node n : neigbours) {
                sndMsg.sendMeassage(n, message);
            }
            closeClient();
        } else {
            message = new Message(id, 3, vectorClock,
                    Integer.parseInt(operations.get(iterator).get(1)), iterator, recItr);
            ArrayList<Node> neigbours = nodeDictionary.get(id).getNodeNeigbhors();
            for (Node n : neigbours) {
                sndMsg.sendMeassage(n, message);
            }
        }
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
                Message checkPointMessage = new Message(kT.id, 1, kT.vectorClock, label, kT.iterator, -1);

                sndMsg.sendMeassage(kT.cohorts.get(id), checkPointMessage);
            }
        }

        public void findCohorts(Node node) {
            ArrayList<Node> neibhors = kT.nodeDictionary.get(kT.id).getNodeNeigbhors();
            System.out.print("Cohorts: ");
            for (Node n : neibhors) {
                int index = node.findNeighbourIndex(n.getNodeId());
                if (parentNodeId == n.getNodeId())
                    continue;
                if (lastLabelRcvd[index] > -1) {
                    kT.cohorts.put(n.getNodeId(), n);
                    kT.bkpCohorts.put(n.getNodeId(), n);
                    System.out.print(" " + n.getNodeId());
                }
            }
            System.out.println();
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
                    if (kT.id == Integer.parseInt(kT.operations.get(kT.iterator).get(1))) {
                        Thread.sleep(minDelay);
                    }
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
                    System.out.println("\nTentative CheckPoint Taken\n");
                }

                if (cohorts.size() == 0) {
                    // if not initator of this iteration send reply to parent node
                    tentativeTaken = false;
                    saveCP = true;
                    if (this.parentNodeId != -1) {
                        if (Integer.parseInt(kT.operations.get(kT.iterator).get(1)) != kT.id) {
                            Message ackMessage = new Message(kT.id, 4, kT.vectorClock, -1, kT.iterator, -1);
                            sndMsg.sendMeassage(kT.nodeDictionary.get(this.parentNodeId), ackMessage);
                        }
                    }
                }
            }

            // Permanent CP
            if (kT.id == Integer.parseInt(kT.operations.get(kT.iterator).get(1))) {
                if (cPointsTaken.size() == 0) {
                    int number = (int) (Math.random());
                    checkPointsTaken CP = new checkPointsTaken(number, backupVectorClock);
                    cPointsTaken.add(CP);
                    takePermanent = false;
                    Message perMessage = new Message(id, 5, vectorClock, number, iterator, -1);
                    sendPermanentMessage(perMessage);
                    kT.iterator++;
                    kT.floodNetwork(kT.id, kT.iterator);
                } else {
                    int number = cPointsTaken.getLast().getSeqNumber();
                    saveCheckPoint(++number);
                    kT.iterator++;
                    kT.floodNetwork(kT.id, kT.iterator);
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
        appMsg = new appMessage(this);
        appMsg.start();
    }

    void performRecovery(Message incomingMessage, int parentNodeId) {
        Recovery r = new Recovery(this, incomingMessage, parentNodeId);
        r.start();
    }

    public class Recovery extends Thread {

        kooToueg kT;
        Message incMessage;
        int parentNodeId;

        public Recovery(kooToueg kT, Message incomingMessage, int parentNodeId) {
            this.kT = kT;
            this.incMessage = incomingMessage;
            this.parentNodeId = parentNodeId;
        }

        public void run() {
            try {
                if (kT.id == Integer.parseInt(kT.operations.get(kT.iterator).get(1))) {
                    Thread.sleep(minDelay);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < lastLabelSent.length; i++) {
                lastLabelSent[i] = bkLLS[i];
                lastLabelRcvd[i] = -1;
                firstLabelSent[i] = -1;
            }
            System.out.println("\nRecovery Started\n");
            for (int i = 0; i < vectorClock.length; i++) {
                System.out.print(" " + vectorClock[i]);
            }
            System.out.println("\nRecovery Completed\n");
            synchronized (kT.vectorClock) {
                for (int i = 0; i < vectorClock.length; i++) {
                    vectorClock[i] = backupPerVectorClock[i];
                    backupVectorClock[i] = -1;
                    System.out.print(" " + vectorClock[i]);
                }
            }
            System.out.println();

            for (Node n : kT.nodeDictionary.get(kT.id).getNodeNeigbhors()) {
                if (parentNodeId == -1 || n.getNodeId() != incMessage.getId()) {
                    
                    int ndId = n.getNodeId();
                    int idx = kT.nodeDictionary.get(kT.id).findNeighbourIndex(ndId);
                    int labl = kT.lastLabelSent[idx];
                    Message recMessage = new Message(kT.id, 2, incMessage.vectorClock, labl, incMessage.iterator,
                            kT.recItr);
                    kT.sndMsg.sendMeassage(n, recMessage);
                }
            }

            try {

                Thread.sleep(2000);
                if(kT.id == Integer.parseInt(kT.operations.get(kT.iterator).get(1))) {
                    kT.iterator++;
                    kT.floodNetwork(kT.id, kT.iterator);
                }
                
            } catch(Exception e) {
                e.printStackTrace();
            }   


        }
    }

    public void closeClient() {
        System.exit(0);
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
            bkLLS = new int[noOfNeigbhorNodes];
            vectorClock = new int[noOfNodes];
            backupVectorClock = new int[noOfNodes];
            backupPerVectorClock = new int[noOfNodes];
            iterationsTaken = new int[operations.size()];

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