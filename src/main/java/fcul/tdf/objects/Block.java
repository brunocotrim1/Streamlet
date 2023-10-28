package fcul.tdf.objects;

import fcul.tdf.Streamlet;
import lombok.Builder;
import lombok.Data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Data
@Builder
public class Block implements java.io.Serializable {
    private byte[] previousHash = new byte[32];
    int epoch;
    private int length;
    private ArrayList<Transaction> transactions;
    Queue<Integer> votes = new ConcurrentLinkedQueue<Integer>();


    public byte[] hashBlock() {
        StringBuilder sc = new StringBuilder();
        sc.append(this.getEpoch());
        sc.append(this.getLength());
        sc.append(new String(Base64.getEncoder().encode(this.getPreviousHash())));
        if (this.getTransactions() != null) {
            for (Transaction t : this.getTransactions()) {
                sc.append(t.toString());
            }
        }
        String toHash = sc.toString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(toHash.getBytes());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }

    }

    public boolean isNotarized() {
        return this.votes.size() > Streamlet.nodesList.size() / 2;
    }

    public String toString() {
        return "Block{" +
                "epoch=" + epoch +
                ", length=" + length +
                "," + votes+
                '}';
    }

    public boolean equals(Block block2) {
        if (block2 == null) {
            return false;
        }
        if (block2 == this) {
            return true;
        }

        if (block2.getEpoch() == this.getEpoch() && block2.getLength() == this.getLength() && Arrays.equals(block2.getPreviousHash(), this.getPreviousHash())) {
            //Also Compare transactions
            return true;
        }
        return false;
    }

    public void reset(){
        votes = new ConcurrentLinkedQueue<Integer>();
    }
}
