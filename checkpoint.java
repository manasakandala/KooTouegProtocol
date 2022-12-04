import java.util.ArrayList;

public class Checkpoint extends Thread {

    boolean tentativeTaken;
    int parentNodeId;

    public Checkpoint() {
        tentativeTaken = false;
    }

    public Checkpoint(int id) {
        this.parentNodeId = id;
        tentativeTaken = false;
    }

    public void checkPointMessage() {
        for (int id : cohorts.keySet()) {
            int index = nodeDictionary.get(id).findNeighbourIndex(id);
            int label = lastLabelRcvd[index];
            Message checkPointMessage = new Message(id, 1, vectorClock, label, iterator);

            sendMeassage(cohorts.get(id), checkPointMessage);
        }
    }

    public void findCohorts(Node node) {
        ArrayList<Node> neibhors = nodeDictionary.get(id).getNodeNeigbhors();
        System.out.print("Cohorts: ");
        for (Node n : neibhors) {
            int index = node.findNeighbourIndex(n.getNodeId());
            if(parentNodeId == n.getNodeId())
                continue;
            if (lastLabelRcvd[index] > -1) {
                cohorts.put(n.getNodeId(), n);
                bkpCohorts.put(n.getNodeId(), n);
                System.out.print(" "+ n.getNodeId());
            }
        }
        System.out.println();
    }

    public void floodNetwork(int id, int iterator) {
        Message floodMessage = new Message(id, 3, vectorClock, Integer.parseInt(operations.get(iterator).get(1)), iterator);

        ArrayList<Node> neigbours = nodeDictionary.get(id).getNodeNeigbhors();
        for (Node n : neigbours) {
            sendMeassage(n, floodMessage);
        }
    }

    public void run() {
        findCohorts(nodeDictionary.get(id));

        if (iterator == 0 && id == Integer.parseInt(operations.get(0).get(1))) {
            try {
                sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        while (!saveCP) {
            try {
                appMessage.sleep(1000);
                if (id == Integer.parseInt(operations.get(0).get(1))) {
                    Checkpoint.sleep(minDelay);
                }
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
                    if (Integer.parseInt(operations.get(iterator).get(1)) != id) {
                        Message ackMessage = new Message(id, 4, vectorClock, -1, iterator);
                        sendMeassage(nodeDictionary.get(this.parentNodeId), ackMessage);
                    }
                }
            }
        }

        System.out.println("\nKt Id: "+ id+"\nKT iterator id: "+Integer.parseInt(operations.get(iterator).get(1)));
        // Permanent CP
        if (id == Integer.parseInt(operations.get(iterator).get(1))) {
            if (cPointsTaken.size() == 0) {
                int number = (int) (Math.random());
                checkPointsTaken CP = new checkPointsTaken(number, backupVectorClock);
                cPointsTaken.add(CP);
                System.out.println("_______Permanent CheckPoint Taken 2_________");
                takePermanent = false;
                Message perMessage = new Message(id, 5, vectorClock, number, iterator);
                sendPermanentMessage(perMessage);
                iterator++;
                floodNetwork(id, iterator);
            } else {
                int number = cPointsTaken.getLast().getSeqNumber();
                saveCheckPoint(++number);
                iterator++;
                floodNetwork(id, iterator);
            }
        }
        saveCP = false;
    }
}