package fcul.tdf.objects;

import fcul.tdf.Streamlet;
import fcul.tdf.Utils;
import fcul.tdf.enums.Type;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static fcul.tdf.Utils.BroadcastExceptX;

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
        if (m.getType() == Type.ECHO) {
            processMessage(m);
            return;
        }
        if (Streamlet.messageHistory.get(m.getSender()) != null
                && Streamlet.messageHistory.get(m.getSender()).getSequence() >= m.getSequence()) {
            // Se ja tivermos visto a mensagem, nao fazemos nada
            return;
        }

        switch (m.getType()) {
            case ALIVE:
                alive.incrementAndGet();
                break;

            case PROPOSE:
                System.out.println("Received PROPOSE");
                // BlockTree.addBlock((Block) message.content);
                Streamlet.messageHistory.put(m.getSender(), m);
                BroadcastExceptX(Message.builder().type(Type.ECHO).content(m).build(),
                        List.of(m.getSender(), Streamlet.nodeId));
                //Fazer broadcast a todos menos a quem produzio e a nos proprios
            case VOTE:

                BroadcastExceptX(Message.builder().type(Type.ECHO).content(m).build(),
                        List.of(m.getSender(), Streamlet.nodeId));
                break;
        }
    }

    public static int getAlive() {
        return alive.get();
    }

}
