package fcul.tdf;

import fcul.tdf.enums.Type;
import fcul.tdf.objects.BlockTree;
import fcul.tdf.objects.Message;
import fcul.tdf.objects.ReceivingThread;
import fcul.tdf.objects.ReconnectMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static fcul.tdf.Utils.isLeader;

public class Streamlet {
    public static List<String> nodesList;
    public static int epochDelta = 0;
    public static Map<Integer, Node> nodes = new HashMap<>();
    public static int nodeId;
    public static AtomicInteger epoch = new AtomicInteger(0);
    public static final AtomicInteger sequence = new AtomicInteger(0);
    public static Map<Integer, Message> messageHistory = new HashMap<>();
    public static BlockTree blockTree = new BlockTree();

    public static String nodeFileName;
    public static volatile boolean reconnect = false;

    public static void main(String[] args) throws IOException {
        readArgs(args);
        readProperies();

        ExecutorService executor = Executors.newFixedThreadPool(nodesList.size() + 1);
        executor.submit(() -> ListenRequests());

        for (int i = 0; i < nodesList.size(); i++) {
            String[] address = nodesList.get(i).split(":");
            Node node = new Node(i, nodesList.get(i));
            nodes.put(i, node);
            messageHistory.put(i, null);
            executor.execute(node);
        }

        if (!reconnect) {
            while (ReceivingThread.getAlive() != nodesList.size()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("All nodes connected");
            System.out.println("Starting consensus");
            if (isLeader(0, nodeId)) {
                System.out.println("I am the leader - Broadcasting genesis block");
                firstGenesisBlock();
            }
        } else {
            synchronized (blockTree) {
                System.out.println("Reconnecting to the network");
                try {
                    Socket clientSocket;
                    ObjectOutputStream outputStream;
                    clientSocket = new Socket("0.0.0.0", 8080);
                    outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                    ReconnectMessage message = ReconnectMessage.builder()
                            .node(nodeId)
                            .build();
                    outputStream.writeObject(message);
                    ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
                    ReconnectMessage reconnectMessage = (ReconnectMessage) inputStream.readObject();

                    BlockTree.lastFinalizedBlock = reconnectMessage.getLastFinalizedBlock();
                    BlockTree.unverifiedTransactions = reconnectMessage.getUnverifiedTransactions();
                    BlockTree.epochVotes = reconnectMessage.getEpochVotes();
                    BlockTree.blockTree = reconnectMessage.getBlockTree();
                    epoch = reconnectMessage.getEpoch();
                    Utils.epochLeaders = reconnectMessage.getEpochLeaders();
                    Utils.epochRandom = reconnectMessage.getEpochRandom();
                    //messageHistory = reconnectMessage.getMessageHistory();
                    ReceivingThread.initiateEpoch(reconnectMessage.getNextEpoch());
                    Utils.Broadcast(Message.builder().sender(nodeId).type(Type.RECONNECT).build());
                    reconnect = false;
                    inputStream.close();
                    outputStream.flush();
                    outputStream.close();
                    clientSocket.close();
                } catch (IOException e) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static void firstGenesisBlock() {
        synchronized (sequence) {
            Message m = Message.builder().type(Type.PROPOSE).sender(nodeId).sequence(sequence.get())
                    .content(Utils.getGenesisBlock()).additionalInfo(Instant.now().plusSeconds(5)).build();
            Utils.Broadcast(m);
            Streamlet.sequence.incrementAndGet();
            System.out.println("Broadcasted genesis block");
        }
    }


    private static void ListenRequests() {
        try {
            ExecutorService processors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            int port = 8080 + nodeId;

            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server listening on port " + port);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    processors.submit(new ReceivingThread(clientSocket));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Error creating server socket: " + e.getMessage());
            System.exit(-1);
        }
    }

    private static void readArgs(String[] args) {
        if (args.length != 2) {
            System.err.println("Please provide a node ID as a command-line argument Or reconnect 1/0");
            System.exit(-1);
        }
        try {
            nodeId = Integer.parseInt(args[0]);
            reconnect = Integer.parseInt(args[1]) == 1;
            nodeFileName = "node" + nodeId + ".json";
            try {
                // Delete the file if it exists
                Files.deleteIfExists(Paths.get(nodeFileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid node ID provided. Please provide an integer.");
            System.exit(-1);
        }
    }

    private static void readProperies() {
        String resourceName = "properties.txt"; // could also be a constant
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties props = new Properties();
        try (InputStream resourceStream = loader.getResourceAsStream(resourceName)) {
            props.load(resourceStream);
            nodesList = Arrays.asList(props.getProperty("nodes").replace("[", "").replace("]", "").split(","));
            epochDelta = Integer.parseInt(props.getProperty("epochDelta"));
        } catch (IOException e) {
            System.err.println("Error reading properties file: " + e.getMessage());
            System.exit(-1);
        }
    }

    private static int aliveNodes() {
        int count = 0;
        for (Node n : nodes.values()) {
            if (n.isConnectionAlive()) {
                count++;
            }
        }
        return count;
    }
}
