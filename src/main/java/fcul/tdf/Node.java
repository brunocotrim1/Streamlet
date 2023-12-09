package fcul.tdf;

import fcul.tdf.Streamlet;
import fcul.tdf.enums.Type;
import fcul.tdf.objects.Message;
import lombok.Getter;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedDeque;

import static fcul.tdf.Utils.confusion_duration;
import static fcul.tdf.Utils.confusion_start;

public class Node extends Thread {
    private Socket clientSocket;
    @Getter
    private int nodeId;
    private ConcurrentLinkedDeque<Message> messageQueue = new ConcurrentLinkedDeque<>();
    private final String address;
    private ObjectOutputStream outputStream;

    public Node(int id, String address) {
        this.nodeId = id;
        this.address = address;
        messageQueue.addFirst(Message.builder().type(Type.ALIVE).sender(Streamlet.nodeId).build());
    }

    @Override
    public void run() {
        String[] address = this.address.split(":");
        while (true) {
            try {
                if (!messageQueue.isEmpty()) {

                    if (Streamlet.epoch.get() < confusion_start ||
                            Streamlet.epoch.get() >= confusion_start + confusion_duration - 1) {
                        Message message = messageQueue.poll();
                        if (networkSend(message, address)) {
                            //System.out.println("Sending message " + message + " to " + nodeId);
                        } else {
                            messageQueue.addFirst(message);
                        }
                        Thread.sleep(10);

                    }else{
                        Thread.sleep(1000);
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean networkSend(Message message, String[] address) {
        try {
            clientSocket = new Socket(address[0], Integer.parseInt(address[1]));
            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            outputStream.writeObject(message);
            outputStream.flush();
            outputStream.close();
            clientSocket.close();
            return true;
        } catch (IOException e) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
        return false;
    }

    public void sendMessage(Message message) {
        messageQueue.add(message);
    }

    public boolean isConnectionAlive() {
        System.out.println(clientSocket);
        return clientSocket != null && clientSocket.isConnected();
    }
}
