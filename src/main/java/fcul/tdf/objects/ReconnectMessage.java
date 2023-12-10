package fcul.tdf.objects;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Builder
@ToString
public class ReconnectMessage implements java.io.Serializable {
    private int node;
    private Random epochRandom;
    private Map<Integer, Integer> epochLeaders;
    private Map<Integer, Message> messageHistory;
    private AtomicInteger epoch;
    private Instant nextEpoch;
    private Map<String, List<Integer>> epochVotes = new HashMap<>();
    private Map<Integer, List<Block>> blockTree = new HashMap<>();
    private ConcurrentLinkedQueue<Transaction> unverifiedTransactions = new ConcurrentLinkedQueue<>();
    private Block lastFinalizedBlock = null;
}

