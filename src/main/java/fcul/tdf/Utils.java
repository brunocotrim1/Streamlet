package fcul.tdf;

import fcul.tdf.enums.Type;
import fcul.tdf.objects.Block;
import fcul.tdf.objects.Message;
import fcul.tdf.objects.Node;
import fcul.tdf.objects.Transaction;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

public class Utils {

    public static boolean isLeader(int epoch, int leader, int nodeSize) {
        int hash = (epoch) % nodeSize;
        return hash == leader;
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
