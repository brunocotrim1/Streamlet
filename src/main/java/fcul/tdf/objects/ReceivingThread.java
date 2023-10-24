package fcul.tdf.objects;

import fcul.tdf.Streamlet;
import fcul.tdf.Utils;
import fcul.tdf.enums.Type;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static fcul.tdf.Utils.Broadcast;
import static fcul.tdf.Utils.BroadcastExceptX;

public class ReceivingThread extends Thread {
    private Socket clientSocket;
    private static AtomicInteger alive = new AtomicInteger(0);
    private ScheduledExecutorService executorService;

    public ReceivingThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
            Message message = (Message) inputStream.readObject();
            // System.out.println(message);
            processMessage(message);
/*            inputStream.close();
            clientSocket.close();*/

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    private void processMessage(Message m) {
        System.out.println("Processing message " + m);
        if (m.getType() == Type.ECHO && m.getSender() != Streamlet.nodeId) {
            processMessage((Message) m.getContent());
            return;
        }
        if ((Streamlet.messageHistory.get(m.getSender()) != null
                && Streamlet.messageHistory.get(m.getSender()).getSequence() >= m.getSequence())
                && m.sender != Streamlet.nodeId) {
            // Se ja tivermos visto a mensagem, nao fazemos nada
            System.out.println("Already seen message " + m);
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
                if (m.getSender() != Streamlet.nodeId)
                    BroadcastExceptX(Message.builder().type(Type.ECHO).content(m).build(),
                            List.of(m.getSender(), Streamlet.nodeId));
                //Fazer broadcast a todos menos a quem produzio e a nos proprios
                if (BlockTree.verifyGenesisBlock((Block) m.getContent())) {
                    Streamlet.epoch.getAndIncrement();
                    initiateEpoch((Instant) m.getAdditionalInfo());
                }
                break;
            case VOTE:

                BroadcastExceptX(Message.builder().type(Type.ECHO).content(m).build(),
                        List.of(m.getSender(), Streamlet.nodeId));
                break;
        }
    }

    public static int getAlive() {
        return alive.get();
    }

    public void epoch() {
        System.out.println("Epoch + " + Streamlet.epoch.get() + " Started");
        if (Utils.isLeader(Streamlet.epoch.get(), Streamlet.nodeId, Streamlet.nodes.size())) {
            System.out.println("I am the leader - Broadcasting genesis block");
        }
        Streamlet.epoch.getAndIncrement();
    }


    public void initiateEpoch(Instant epochStart) {
        executorService = Executors.newScheduledThreadPool(1);
        while (true) {
            Instant currentInstant = Instant.now();
            if (currentInstant.isAfter(epochStart)) {
                executorService.scheduleAtFixedRate(this::epoch, 0,
                        Streamlet.epochDelta, TimeUnit.SECONDS);
                break; // Exit the loop when the instant has arrived
            }
            try {
                Thread.sleep(10); // Sleep for 1 second
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
