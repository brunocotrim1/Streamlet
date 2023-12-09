package fcul.tdf.objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fcul.tdf.Streamlet;
import fcul.tdf.Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class BlockTree {
    public static Map<Integer, List<Block>> blockTree = new HashMap<>();
    public Map<String, List<Integer>> epochVotes = new HashMap<>();
    public static ConcurrentLinkedQueue<Transaction> unverifiedTransactions = new ConcurrentLinkedQueue<>();

    public static Block lastFinalizedBlock = null;

    public synchronized boolean addBlock(Block block, int sender) {
        try {
            boolean voted = true;
            block.reset();
            if (block.epoch == 0 && fcul.tdf.objects.BlockTree.blockTree.containsKey(0)) {
                return false;
            }
            if (block.epoch == 0 && Utils.isLeader(0, sender)) {
                List<Block> blockList = new ArrayList<>();
                blockList.add(block);
                blockTree.put(0, blockList);
                return true;
            }
            if (!Utils.isLeader(block.epoch, sender)) {
                return false;
            }
            List<Block> longestChain = longestNotarizedChain();
/*            if (!Arrays.equals(longestChain.get(longestChain.size() - 1).hashBlock(), block.getPreviousHash())) {
                return false;
            }*/
            if(longestChain.get(longestChain.size() - 1).getLength() >= block.getLength()){
                System.out.println(longestChain.get(longestChain.size() - 1));
                System.out.println(block.getLength());
                voted = false;
            }
            if (Streamlet.epoch.get() == 0 && block.epoch == 0) {
                List<Block> blockList = new ArrayList<>();
                blockList.add(block);
                blockTree.put(0, blockList);
            } else {
                //Se o tamanho do block for de uma cadeia imediatamente a seguir à maior cadeia
                if (blockTree.containsKey(block.getLength())) {
                    List<Block> blockList = blockTree.get(block.getLength());
                    blockList.add(block);
                } else {
                    List<Block> blockList = new ArrayList<>();
                    blockList.add(block);
                    blockTree.put(block.getLength(), blockList);
                }
            }
            return voted;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }




    public void checkFinalized() {
        System.out.println();
        List<Block> longestChain = longestNotarizedChain();
        if (longestChain.isEmpty()) {
            return;
        }
        if(longestChain.size() >4){
            Block lastBlock = longestChain.get(longestChain.size() - 1);
            int lastEpoch = longestChain.get(longestChain.size() - 1).getEpoch();
            int secondLastEpoch = longestChain.get(longestChain.size() - 2).getEpoch();
            int thirdLastEpoch = longestChain.get(longestChain.size() - 3).getEpoch();
            if ((lastEpoch - secondLastEpoch == 1) && (lastEpoch - thirdLastEpoch == 2)){
                System.out.println("FINALIZED CHAIN" + longestChain.subList(0, longestChain.size() - 1));
                writeBlocksIntoFile(longestChain.subList(0, longestChain.size() - 1));
                Block lasBlock = longestChain.get(longestChain.size() - 1);
                finalizeBlockTree(longestChain.subList(0, longestChain.size() - 1), lasBlock);
            }
        }else if (lastFinalizedBlock !=null && lastFinalizedBlock.epoch == longestChain.get(0).epoch-1){
            System.out.println("FINALIZED CHAIN" + longestChain.subList(0, longestChain.size() - 1));
            writeBlocksIntoFile(longestChain.subList(0, longestChain.size() - 1));
            Block lasBlock = longestChain.get(longestChain.size() - 1);
            finalizeBlockTree(longestChain.subList(0, longestChain.size() - 1), lasBlock);
        }
        System.out.println();
    }

    public static void finalizeBlockTree(List<Block> finalizedBlocks,Block lastBlock) {
        if(finalizedBlocks.isEmpty()){
            return;
        }
        Map<Integer, List<Block>> finalizedBlockTree = new HashMap<>();
        for (List<Block> blocks : blockTree.values()) {
            for (Block b : blocks) {
                if (b.getTransactions() == null) {
                    continue;
                }
                if (!finalizedBlocks.contains(b)) {
                    unverifiedTransactions.addAll(b.getTransactions());
                }else {
                    unverifiedTransactions.removeAll(b.getTransactions());
                }
            }
        }
        BlockTree.lastFinalizedBlock = finalizedBlocks.get(finalizedBlocks.size() - 1);
        List<Block> lastBlockList = new ArrayList<>();
        lastBlockList.add(lastBlock);
        finalizedBlockTree.put(lastBlock.getLength(), lastBlockList);
        blockTree = finalizedBlockTree;
    }

    public List<Block> longestNotarizedChain() {
        //System.out.println(blockTree);
        refreshVotes();
        if (lastFinalizedBlock != null) {
            return RecursiveLongestNotarizedChain(blockTree.keySet()
                    .stream().mapToInt(Integer::intValue).min().getAsInt(), blockTree, lastFinalizedBlock);
        } else {
            return RecursiveLongestNotarizedChain(0, blockTree, null);
        }
    }

    private List<Block> RecursiveLongestNotarizedChain(int index, Map<Integer, List<Block>> blockMap, Block lastBlock) {
        if (blockMap.size() == 1) {
            return blockMap.values().stream().collect(Collectors.toList()).get(0);
        }
        if (index == 0) {
            ArrayList<Block> blockList = new ArrayList<>();
            blockList.addAll(blockMap.get(0));
            blockList.addAll(RecursiveLongestNotarizedChain(index + 1, blockMap, blockMap.get(index).get(0)));
            return blockList;
        }

        if (index == blockMap.size()) {
            return Collections.emptyList(); // Return an empty list for an empty map
        }
        if(blockMap.get(index) == null){
            return Collections.emptyList();
        }

        List<List<Block>> chains = new ArrayList<>();

        for (Block block : blockMap.get(index)) {
            if (block.epoch == 0) {
                continue;
            }
            boolean equalToLast = Arrays.equals(block.getPreviousHash(), lastBlock.hashBlock());
            boolean isNotarized = isNotarized(block);
            if (Arrays.equals(block.getPreviousHash(), lastBlock.hashBlock()) && isNotarized(block)) {
                List<Block> blockList = new ArrayList<>();
                blockList.add(block);
/*                if (index == blockMap.size() - 1) {
                    return blockList;
                } else {*/
                blockList.addAll(RecursiveLongestNotarizedChain(index + 1, blockMap, block));
                chains.add(blockList);
                //}
            }
        }
        List<Block> longestChain = Collections.emptyList();
        for (List<Block> chain : chains) {
            if (chain.size() > longestChain.size()) {
                longestChain = chain;
            }
        }
        return longestChain;
    }

    private static boolean isNotarized(Block block) {
        // Implement your logic to check if a block is notarized.
        // You should return true if the block meets your criteria for notarization.
        // Define your criteria based on your specific use case.
        // Here, we assume all blocks are notarized.
        return block.epoch == 0 || block.isNotarized();
    }


    public static boolean verifyGenesisBlock(Block block) {
        return block.getEpoch() == 0 && block.getLength() == 0;
    }

    public synchronized void addVote(Message content, int sender) {
        refreshVotes();
        boolean found = false;
        Block block = (Block) content.getContent();
        for (List<Block> blocks : blockTree.values()) {
            for (Block b : blocks) {
                if (b.equals(block) && !b.votes.contains(sender)) {
                    b.votes.add(sender);
                }
            }
        }
        if (!found) {
            if (epochVotes.containsKey(block.getEpoch())) {
                epochVotes.get(block.getEpoch()).add(sender);
            } else {
                ArrayList<Integer> senders = new ArrayList<>();
                senders.add(sender);
                epochVotes.put(Base64.getEncoder().encodeToString(block.hashBlock()), senders);
            }
        }
    }

    public void refreshVotes() {
        for (List<Block> blocks : blockTree.values()) {
            for (Block block : blocks) {
                if (epochVotes.containsKey(Base64.getEncoder().encodeToString(block.hashBlock()))) {
                    List<Integer> votes = epochVotes.get(Base64.getEncoder().encodeToString(block.hashBlock()));
                    for (int vote : votes) {
                        if (!block.votes.contains(vote)) {
                            block.votes.add(vote);
                        }
                    }
                }
            }
        }
    }



    public Block pruposeBlock() {
        System.out.println(blockTree);
        refreshVotes();
        List<Block> longestChain = longestNotarizedChain();
        return Block.builder().epoch(Streamlet.epoch.get())
                .length(longestChain.get(longestChain.size() - 1).getLength() + 1)
                .transactions(getUnverifiedTransactions())
                .previousHash(longestChain.get(longestChain.size() - 1).hashBlock()).build();
    }

    public synchronized List<Transaction> getUnverifiedTransactions() {
        try {
            ArrayList<Transaction> unverifiedNotProposed = new ArrayList<>();
            for (Transaction t : unverifiedTransactions) {
                boolean found = false;
                for (List<Block> blocks : blockTree.values()) {
                    for (Block b : blocks) {
                        if (b.getTransactions() != null && b.getTransactions().contains(t)) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    unverifiedNotProposed.add(t);
                }
            }
            return unverifiedNotProposed;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public synchronized void addUnverifiedTransactions(List<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            if (!unverifiedTransactions.contains(transaction)) {
                unverifiedTransactions.add(transaction);
            }
        }
    }

    public void writeBlocksIntoFile(List<Block> blocks) {
        StringBuilder sb = new StringBuilder();
        for (Block block : blocks) {
            JsonObject jsonObject = new JsonObject();

            // Append your own variables
            jsonObject.addProperty("previousHash", bytesToHexString(block.getPreviousHash()));
            jsonObject.addProperty("epoch", block.getEpoch());
            jsonObject.addProperty("length", block.getLength());
            jsonObject.addProperty("votes", block.getVotes() == null ? "[]" : block.getVotes().toString());
            jsonObject.addProperty("transactions", block.getTransactions() == null ? "[]" :
                    block.getTransactions().toString());
            jsonObject.addProperty("hash", bytesToHexString(block.hashBlock()));

            sb.append(jsonObject.toString() + ",\n");
        }
        try (FileWriter writer = new FileWriter(Streamlet.nodeFileName, true)) {
            writer.write(sb.toString() + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }
}
