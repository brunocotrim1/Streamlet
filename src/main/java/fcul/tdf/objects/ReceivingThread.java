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

            switch (message.getType()) {
                case ALIVE:
                    alive.incrementAndGet();
                    break;

                case PROPOSE:
                    System.out.println("Received PROPOSE");
                   // BlockTree.addBlock((Block) message.content);
                    break;
                case ECHO:

                    break;
            }
            //So fazemos echo se for a primeira mensagem ou se for uma mensagem que ainda nao tenhamos visto
            if ((Streamlet.messageHistory.get(message.getSender()) == null
                    || Streamlet.messageHistory.get(message.getSender()).getSequence() < message.getSequence())
            && message.getType() != Type.ALIVE) {

                //Nao mandar echos que contem a propria mensagem sua
                if(!(message.getType() == Type.ECHO && Streamlet.nodeId == ((Message) message.getContent()).getSender())){

                    Streamlet.messageHistory.put(message.getSender(), message);
                    Utils.Broadcast(Message.builder().type(Type.ECHO).sender(Streamlet.nodeId)
                            .content(message).build());
                }
            }


        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    public static int getAlive() {
        return alive.get();
    }

}
