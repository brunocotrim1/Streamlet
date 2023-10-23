package fcul.tdf.objects;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@ToString
@Builder
public class Block implements java.io.Serializable {
    private byte[] previousHash = new byte[32];
    int epoch;
    private int length;
    private ArrayList<Transaction> transactions;
    private boolean notarized;
    private boolean finalized;
    private final AtomicInteger votes = new AtomicInteger(0);
}
