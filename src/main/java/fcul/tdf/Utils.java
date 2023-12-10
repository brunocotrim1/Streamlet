package fcul.tdf;

import fcul.tdf.objects.Block;
import fcul.tdf.objects.Message;
import fcul.tdf.objects.ReconnectMessage;
import fcul.tdf.objects.Transaction;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Utils {

    public static Random epochRandom = new Random(1234);
    public static Map<Integer, Integer> epochLeaders = new HashMap<>();
    public static int confusion_start =2;
    public static int confusion_duration = 3;

/*    public static boolean isLeader(int epoch, int leader) {
        if(epochLeaders.containsKey(epoch)) {
            return epochLeaders.get(epoch) == leader;
        }
        int leaderId = epochRandom.nextInt(Streamlet.nodesList.size());
        epochLeaders.put(epoch, leaderId);
        return leaderId == leader;
    }*/


    public static boolean isLeader(int epoch, int leader) {
        if(epoch < confusion_start || epoch >= confusion_start + confusion_duration-1){
            if(epochLeaders.containsKey(epoch)) {
                return epochLeaders.get(epoch) == leader;
            }
            int leaderId = epochRandom.nextInt(Streamlet.nodesList.size());
            epochLeaders.put(epoch, leaderId);
            return leaderId == leader;
        }else{
            epochLeaders.put(epoch, epoch % Streamlet.nodesList.size());
            return (epoch % Streamlet.nodesList.size()) == leader;
        }
    }


    public static void Broadcast(Message message) {
       // if (message.getType() != Type.ECHO)
          //  System.out.println("Broadcasting + " + message.getType() + " " + message.getSender() + " " + message.getSequence());
        for (Node n : Streamlet.nodes.values()) {
            //  System.out.println("BroadCast message" + message + " to " + n.getNodeId());
            n.sendMessage(message);
        }
    }

    public static void BroadcastExceptX(Message message, List<Integer> exceptions) {
       // if (message.getType() != Type.ECHO)
          //  System.out.println("Broadcasting + " + message.getType() + " " + message.getSender() + " " + message.getSequence());

        for (Node n : Streamlet.nodes.values()) {
            if (!exceptions.contains(n.getNodeId())) {
                //System.out.println("BroadCast message" + message + " to " + n.getNodeId());
                n.sendMessage(message);
            }
        }
    }



    public static Block getGenesisBlock() {
        return Block.builder()
                .previousHash(new byte[32])
                .epoch(0)
                .length(0)
                .build();
    }

    public static byte[] hashBlock(Block block) {
        StringBuilder sc = new StringBuilder();
        sc.append(block.getEpoch());
        sc.append(block.getLength());
        sc.append(new String(Base64.getEncoder().encode(block.getPreviousHash())));
        for (Transaction t : block.getTransactions()) {
            sc.append(t.toString());
        }
        String toHash = sc.toString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(toHash.getBytes());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }

    }

}
