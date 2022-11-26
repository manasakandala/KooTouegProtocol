import java.io.File;
import java.io.FileNotFoundException;
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
        nodeDictionary  = new HashMap<>();
        operations = new ArrayList<ArrayList<String>>();
    }

    public void startThreads() {  
        new server(this).start();
        new appMessage(this).start();
        new appControler(this).start();
    }

    public void readData() {
        try{
            File config = new File("config.txt");
            Scanner sc = new Scanner(config);
            String d = sc.nextLine();
            
            String[] line1 = d.split(" ");
            noOfNodes = Integer.parseInt(line1[0]);
            minDelay = Integer.parseInt(line1[1]);
            
            for (int i=0; i<noOfNodes; i++) {
                String data = sc.nextLine();
                if (data == "") {
                    data = sc.nextLine();
                }

                String[] nodeDetails = data.split(" ");
                Node node = new Node(Integer.parseInt(nodeDetails[0]), nodeDetails[1], Integer.parseInt(nodeDetails[2]));
                nodeDictionary.put(node.getNodeId(), node);
            }

            for(int i=0; i<noOfNodes; i++) {
                String data = sc.nextLine();
                if (data == "") {
                    data = sc.nextLine();
                }

                String[] nodeNeigbhorsDetails = data.split(" ");
                Node currentNode = nodeDictionary.get(i);

                for(int j=0; j<nodeNeigbhorsDetails.length; j++) {
                    int nodeID = Integer.parseInt(nodeNeigbhorsDetails[j]);
                    Node node = nodeDictionary.get(nodeID);
                    currentNode.addNodeNeigbhor(node);
                }
            }

            while(sc.hasNextLine()) {
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
            lastLabelRcvd  = new int[noOfNeigbhorNodes];
            lastLabelSent  = new int[noOfNeigbhorNodes];
            firstLabelSent = new int[noOfNeigbhorNodes];
            bkLLR  = new int[noOfNeigbhorNodes];
            bkFLS = new int[noOfNeigbhorNodes];
            vectorClock = new int[noOfNodes];
            backupVectorClock = new int[noOfNodes];

            sc.close();
        } catch(FileNotFoundException fileNotFoundException) {
            System.out.println("Error during reading file");
            fileNotFoundException.printStackTrace();
        }
    }
    public static void main(String args[]) {
        int serverNumber = Integer.parseInt(args[0]);
        kooToueg obj = new kooToueg(serverNumber);
        
        obj.readData();
        int serNum = Integer.parseInt(obj.operations.get(0).get(1));
        if( serNum == serverNumber) {
            obj.task = true;
            obj.taskNodeId = obj.id;
            obj.taskType = obj.operations.get(0).get(0);
        }
        
        obj.startThreads();
    }
}