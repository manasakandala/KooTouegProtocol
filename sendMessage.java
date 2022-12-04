import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class sendMessage {

    
    public sendMessage() {
        
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
}
