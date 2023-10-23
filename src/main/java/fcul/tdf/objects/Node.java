package fcul.tdf.objects;

import fcul.tdf.Streamlet;
import fcul.tdf.enums.Type;
import lombok.Getter;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedDeque;

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
                    Message message = messageQueue.poll();
                    if (networkSend(message, address)) {
                        System.out.println("Sending message " + message + " to " + nodeId);
                    } else {
                        messageQueue.addFirst(message);
                    }
                    Thread.sleep(1000);
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
            if ((message.getType() == Type.PROPOSE || message.getType() == Type.VOTE)
                    && Streamlet.nodeId == message.getSender()) {
                if (Streamlet.messageHistory.get(Streamlet.nodeId) == null
                        || Streamlet.messageHistory.get(Streamlet.nodeId).getSequence() < message.getSequence()) {
                    message.setSequence(Streamlet.sequence.get());
                    Streamlet.sequence.incrementAndGet();
                    Streamlet.messageHistory.put(Streamlet.nodeId, message);
                    //Apenas queremos incrementar a sequencia da mensagem e atualizar o historico se for a primeira vez
                    // que observamos nossas
                }
            }
            outputStream.writeObject(message);
            outputStream.flush();
/*            outputStream.close();
            clientSocket.close();*/
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
