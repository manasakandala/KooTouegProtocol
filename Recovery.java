public class Recovery extends Thread {

    kooToueg kT;

    public Recovery(kooToueg kT) {
        this.kT = kT;
    }

    public void run() {
        synchronized(kT.vectorClock) {
            try {
                if (kT.id == Integer.parseInt(kT.operations.get(0).get(1))) {
                    Recovery.sleep(kT.minDelay);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for(int i=0; i<kT.lastLabelSent.length; i++) {
                kT.lastLabelSent[i] = kT.bkLLS[i];
                kT.lastLabelRcvd[i] = -1;
                kT.firstLabelSent[i] = -1;
            }
            System.out.println("_____Recovery Started _____");
            for(int i=0; i<kT.vectorClock.length; i++) {
                System.out.print(" "+kT.vectorClock[i]);
            }
            System.out.println("\n_____Recovery Completed _____");
            for(int i=0; i<kT.vectorClock.length; i++) {
                kT.vectorClock[i] = kT.backupVectorClock[i];
                System.out.print(" "+kT.vectorClock[i]);
            }
            System.out.println();
        }
    }
}