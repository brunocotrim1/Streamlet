package fcul.tdf;

import fcul.tdf.enums.Type;
import fcul.tdf.objects.Message;
import fcul.tdf.objects.Node;
import fcul.tdf.objects.ReceivingThread;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static fcul.tdf.Utils.isLeader;

public class Streamlet {
    public static List<String> nodesList;
    public static int epochDelta = 0;
    public static ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();
    public static Map<Integer, Node> nodes = new HashMap<>();
    public static Map<Integer, ReceivingThread> receivers = new HashMap<>();
    public static ExecutorService executor;
    public static int nodeId;
    public static AtomicInteger epoch = new AtomicInteger(0);
    public static AtomicInteger sequence = new AtomicInteger(0);
    public static Map<Integer, Message> messageHistory = new HashMap<>();

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
        if (isLeader(epoch.get(), nodeId, nodesList.size())) {
            System.out.println("I am the leader - Broadcasting genesis block");
            Utils.Broadcast(Message.builder().type(Type.PROPOSE).build());
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
        if (args.length != 1) {
            System.err.println("Please provide a node ID as a command-line argument.");
            System.exit(-1);
        }
        try {
            nodeId = Integer.parseInt(args[0]);
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
