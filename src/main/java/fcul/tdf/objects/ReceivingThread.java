package fcul.tdf.objects;

import fcul.tdf.Streamlet;
import fcul.tdf.Utils;
import fcul.tdf.enums.Type;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class ReceivingThread extends Thread {
    private Socket clientSocket;
    private static AtomicInteger alive = new AtomicInteger(0);

    public ReceivingThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
            Message message = (Message) inputStream.readObject();
            System.out.println(message);
            processMessage(message);


        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    private void processMessage(Message m) {
        switch (m.getType()) {
            case ALIVE:
                alive.incrementAndGet();
                break;

            case PROPOSE:
                System.out.println("Received PROPOSE");
                // BlockTree.addBlock((Block) message.content);
                break;
            case ECHO:
                m = (Message) m.getContent();
                processMessage(m);
                break;
        }
    }
    public static int getAlive() {
        return alive.get();
    }

}
