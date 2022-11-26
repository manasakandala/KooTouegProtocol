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
    ArrayList<ArrayList<String>> operations;
    int id;
    boolean task;
    int taskNodeId;
    String taskType;
    int iterator;
    int[] vectorClock, backupVectorClock;
    int[] messageLabel;
    int[] lastLabelRcvd, firstLabelSent, lastLabelSent, bkLLR, bkFLS;

    public kooToueg(int serverNumber) {
        noOfNeigbhorNodes = 0;
        iterator = 0;
        this.id = serverNumber;
        task = false;
        nodeDictionary = new HashMap<>();
        operations = new ArrayList<ArrayList<String>>();
    }

    public class appMessage extends Thread {

        kooToueg kT;

        public appMessage(kooToueg kT) {
            this.kT = kT;
        }

        public void updateFLS(int randNeigbhorIndex, int label) {
            if (kT.firstLabelSent[randNeigbhorIndex] == -1) {
                kT.firstLabelSent[randNeigbhorIndex] = label;
            }

            kT.lastLabelSent[randNeigbhorIndex] = label;
        }

        public void sendApplicationMessage() {
            try {
                sleep(10000);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            ArrayList<Node> nodeNeigbhors = kT.nodeDictionary.get(kT.id).getNodeNeigbhors();
            int randNeigbhorIndex = (int) (Math.random() * nodeNeigbhors.size());
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
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void run() {

            while (kT.iterator < kT.operations.size()) {
                try {
                    sleep(1000);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }

                sendApplicationMessage();
            }
        }
    }

    public class appControler extends Thread{
    
        kooToueg kT;
        public appControler(kooToueg kT) {
            this.kT = kT;
        }
    
        public void run() {
            
        }
    }
    

    public class Checkpoint extends Thread {

        kooToueg kT;

        public Checkpoint(kooToueg kT) {
            this.kT = kT;
        }

        public void floodNetwork(int id, int iterator) {
            int[] temp = new int[0];
            Message floodMessage = new Message(id,  3, temp, -1 ,iterator);
            
            ArrayList<Node> neigbours = nodeDictionary.get(id).getNodeNeigbhors();
            for(int i=0; i<neigbours.size(); i++) {
                
            }
        }

        public void run() {
            try {
                appMessage.sleep(1000);
                appControler.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
            System.out.println("_________CheckPoint Taken__________");
            
            floodNetwork(id, iterator);
        }
    }
    public void checkPointMessage(int id) {
        //TODO send msg to all neigbors totake checkpoint
        ArrayList<Node> neigbours = nodeDictionary.get(id).getNodeNeigbhors();
        for(int i=0; i<neigbours.size(); i++) {
            requestCheckPoint();
        }
    }

    public void startThreads() {
        new server(this).start();
        if (task == true && taskType == "c") {
            new Checkpoint(this).start();
            checkPointMessage(id);
        }
        new appMessage(this).start();
        new appControler(this).start();
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
            System.out.println("Error during reading file");
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