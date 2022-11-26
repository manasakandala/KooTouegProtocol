import java.util.ArrayList;

public class Node {
    private int nodeId;
    private String hostName;
    private int port;
    private ArrayList<Node> nodeNeigbhors;

    Node(int nodeId, String hostName, int port) {
        this.nodeId = nodeId;
        this.hostName = hostName;
        this.port = port;
        nodeNeigbhors = new ArrayList<Node>();
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ArrayList<Node> getNodeNeigbhors() {
        return nodeNeigbhors;
    }

    public void addNodeNeigbhor(Node node) {
        this.nodeNeigbhors.add(node);
    }

    public int findNeighbourIndex(int neighbourId) {
        int t = 0;
        for (Node i : nodeNeigbhors) {
            if (i.getNodeId() == neighbourId) {
                return t;
            }
            t += 1;
        }
        return -1;
    }
}
