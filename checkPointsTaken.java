public class checkPointsTaken {
    int seqNumber;
    int[] vectorClock;
    
    public checkPointsTaken(int seqNumber, int[] vectorClock) {
        this.seqNumber = seqNumber;
        this.vectorClock = vectorClock;
    }

    public int getSeqNumber() {
        return seqNumber;
    }
    public void setSeqNumber(int seqNumber) {
        this.seqNumber = seqNumber;
    }
    
    public int[] getVectorClock() {
        return vectorClock;
    }
    public void setVectorClock(int[] vectorClock) {
        this.vectorClock = vectorClock;
    }
}
