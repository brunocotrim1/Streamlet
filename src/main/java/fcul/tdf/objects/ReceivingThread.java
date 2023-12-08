package fcul.tdf.objects;

import fcul.tdf.Streamlet;
import fcul.tdf.Utils;
import fcul.tdf.enums.Type;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static fcul.tdf.Streamlet.*;
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
            //System.out.println(message);
            processMessage(message);
            inputStream.close();
            clientSocket.close();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    private void processMessage(Message m) {
        if (m.getType() == Type.ECHO && m.getSender() != nodeId) {
            processMessage((Message) m.getContent());
            return;
        }
        //   System.out.println("Processing message " + m);
        if (Streamlet.messageHistory.get(m.getSender()) != null &&
                Streamlet.messageHistory.get(m.getSender()).getSequence() >= m.getSequence()) {
            return;
        }
        if (m.getType() != Type.ALIVE) {
            Streamlet.messageHistory.put(m.sender, m);
        }

        switch (m.getType()) {
            case ALIVE:
                alive.incrementAndGet();
                break;

            case PROPOSE:
                // BlockTree.addBlock((Block) message.content);
                System.out.println("Received PROPOSE " + (Block) m.getContent());
                if (((Block) m.getContent()).epoch ==8){
System.out.println();
                }
                Streamlet.messageHistory.put(m.getSender(), m);
                if (m.getSender() != nodeId)
                    BroadcastExceptX(Message.builder().type(Type.ECHO).content(m).build()
                            , List.of(m.getSender(), nodeId));
                //Fazer broadcast a todos menos a quem produzio e a nos propriosSystem.out.println(m);
                if (BlockTree.verifyGenesisBlock((Block) m.getContent())) {
                    blockTree.blockTree.put(0, new ArrayList<>(Arrays.asList(new Block[]{(Block) m.getContent()})));
                    Streamlet.epoch.getAndIncrement();
                    initiateEpoch((Instant) m.getAdditionalInfo());
                } else if (Streamlet.blockTree.addBlock((Block) m.getContent(), m.sender)) {
                    System.out.println("Sending VOTE" + (Block) m.getContent());
                    synchronized (Streamlet.sequence) {
                        Broadcast(Message.builder().type(Type.VOTE)
                                .sequence(Streamlet.sequence.get())
                                .sender(nodeId).content(m).build());
                        Streamlet.sequence.incrementAndGet();
                    }
                }
                break;
            case VOTE:
                System.out.println("Received VOTE from " + m.sender + " : " + m.getContent());
                BroadcastExceptX(Message.builder().type(Type.ECHO).content(m).build()
                        , List.of(m.getSender(), nodeId));
                Streamlet.blockTree.addVote((Message) m.getContent(), m.getSender());
                break;
            case TRANSACTION:
                List<Transaction> transactions = (List<Transaction>) m.getContent();
                blockTree.addUnverifiedTransactions(transactions);
                break;

        }
    }

    public static int getAlive() {
        return alive.get();
    }

    public void epoch() {
        try {
            synchronized (Streamlet.sequence) {
                if (Streamlet.epoch.get() == 6) {
                    System.out.println();
                }
                blockTree.checkFinalized();
                System.out.println("Epoch " + Streamlet.epoch.get() + " Started");
                if (Utils.isLeader(Streamlet.epoch.get(), nodeId)) {
                    generateRandomTransctions();
                    Block block = null;
                    try {
                        block = Streamlet.blockTree.pruposeBlock();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Message m = Message.builder().type(Type.PROPOSE).sequence(Streamlet.sequence.get())
                            .sender(nodeId).content(block).build();
                    Streamlet.sequence.incrementAndGet();
                    Broadcast(m);
                    System.out.println("Broadcasting block " + block);
                }

                Streamlet.epoch.getAndIncrement();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void initiateEpoch(Instant epochStart) {
        executorService = Executors.newScheduledThreadPool(1);
        while (true) {
            Instant currentInstant = Instant.now();
            if (currentInstant.isAfter(epochStart)) {
                executorService.scheduleAtFixedRate(this::epoch, 0, Streamlet.epochDelta, TimeUnit.SECONDS);
                break; // Exit the loop when the instant has arrived
            }
            try {
                Thread.sleep(10); // Sleep for 1 second
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void generateRandomTransctions() {
        List<Transaction> transactions = new ArrayList<>();
        Random r = ThreadLocalRandom.current();
        for (int i = 0; i < Streamlet.nodesList.size(); i++) {
            transactions.add(new Transaction(8080 + nodeId,
                    Integer.valueOf(Streamlet.nodesList.get(i).split(":")[1]),
                    r.nextInt(),
                    epoch.get()));
        }
        blockTree.addUnverifiedTransactions(transactions);

        Message m = Message.builder().type(Type.TRANSACTION).sequence(Streamlet.sequence.get())
                .sender(nodeId).content(transactions).build();
        Streamlet.sequence.incrementAndGet();
        BroadcastExceptX(m, List.of(nodeId));
    }


}
