package fcul.tdf.objects;

import fcul.tdf.Streamlet;
import fcul.tdf.Utils;

import java.util.*;

public class BlockTree {
    public static Map<Integer, List<Block>> BlockTree = new HashMap<>();
    public Map<Integer, List<Integer>> epochVotes = new HashMap<>();

    public synchronized boolean addBlock(Block block, int sender) {
        try {


            block.reset();
            if (block.epoch == 0 && Utils.isLeader(0, sender, Streamlet.nodesList.size())) {
                List<Block> blockList = new ArrayList<>();
                blockList.add(block);
                BlockTree.put(0, blockList);
                return true;
            }
            if (!Utils.isLeader(block.epoch, sender, Streamlet.nodesList.size())) {
                return false;
            }
            List<Block> longestChain = longestNotarizedChain();
            if (!Arrays.equals(longestChain.get(longestChain.size() - 1).hashBlock(), block.getPreviousHash())) {
                return false;
            }
            if (Streamlet.epoch.get() == 0 && block.epoch == 0) {
                List<Block> blockList = new ArrayList<>();
                blockList.add(block);
                BlockTree.put(0, blockList);
            } else {
                //Se o tamanho do block for de uma cadeia imediatamente a seguir à maior cadeia
                if (BlockTree.containsKey(block.getLength())) {
                    List<Block> blockList = BlockTree.get(block.getLength());
                    blockList.add(block);
                } else {
                    List<Block> blockList = new ArrayList<>();
                    blockList.add(block);
                    BlockTree.put(block.getLength(), blockList);
                }

            }
            System.out.println("Added block to tree" + "size: " + BlockTree.size());
            checkFinalized(longestChain);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private synchronized void checkFinalized(List<Block> longestChain) {
        if (longestChain.size() < 4) {
            return;
        }
        Block lastBlock = longestChain.get(longestChain.size() - 1);
        int lastEpoch = longestChain.get(longestChain.size() - 1).getEpoch();
        int secondLastEpoch = longestChain.get(longestChain.size() - 2).getEpoch();
        int thirdLastEpoch = longestChain.get(longestChain.size() - 3).getEpoch();

        // Check if the last three epochs are consecutive.
        if ((lastEpoch - secondLastEpoch == 1) && (secondLastEpoch - thirdLastEpoch == 1)) {
            HashMap<Integer, List<Block>> newBlockTree = new HashMap<>();
            for (int i = 0; i < longestChain.size(); i++) {
                List<Block> blockList = new ArrayList<>();
                blockList.add(longestChain.get(i));
                newBlockTree.put(i, blockList);
            }
            BlockTree = newBlockTree;
        }
    }


    public synchronized List<Block> longestNotarizedChain() {
        return RecursiveLongestNotarizedChain(0, BlockTree, null);
    }

    private synchronized List<Block> RecursiveLongestNotarizedChain(int index, Map<Integer, List<Block>> blockMap, Block lastBlock) {
        refreshVotes();
/*        if (lastBlock == null && index != 0) {
            lastBlock = blockMap.get(index).get(0);
        }*/
        if (blockMap.size() == 1) {
            return blockMap.get(0);
        }
        if(index == 0){
            return RecursiveLongestNotarizedChain(index + 1, blockMap, blockMap.get(index).get(0));
        }

        if (index == blockMap.size()) {
            return Collections.emptyList(); // Return an empty list for an empty map
        }

        List<List<Block>> chains = new ArrayList<>();

        for (Block block : blockMap.get(index)) {
            if (block.epoch == 0) {
                continue;
            }
            boolean equalToLast = Arrays.equals(block.getPreviousHash(), lastBlock.hashBlock());
            boolean isNotarized = isNotarized(block);
            if (Arrays.equals(block.getPreviousHash(), lastBlock.hashBlock()) && isNotarized(block)) {

                if (index == blockMap.size() - 1) {
                    List<Block> blockList = new ArrayList<>();
                    blockList.add(block);
                    return blockList;
                } else {
                    chains.add(RecursiveLongestNotarizedChain(index + 1, blockMap, block));
                }
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
        for (List<Block> blocks : BlockTree.values()) {
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
                epochVotes.put(block.getEpoch(), senders);
            }
        }
    }

    public synchronized void refreshVotes() {
        for (int epoch : epochVotes.keySet()) {
            for (List<Block> blocks : BlockTree.values()) {
                for (Block b : blocks) {
                    for (int i : epochVotes.get(epoch)) {
                        if (!b.votes.contains(i)) {
                            b.votes.add(i);
                        }
                    }
                   // epochVotes.remove(epoch);
                }
            }
        }
    }

    public void printFinalizedChain() {
        List<Block> longestChain = longestNotarizedChain();
        for (Block b : longestChain) {
            System.out.print(b.toString() + "--->");
        }
        System.out.println();
    }

    public synchronized Block pruposeBlock() {
        System.out.println(fcul.tdf.objects.BlockTree.BlockTree);
        List<Block> longestChain = longestNotarizedChain();
        return Block.builder().epoch(Streamlet.epoch.get()).length(longestChain.size())
                .previousHash(longestChain.get(longestChain.size() - 1).hashBlock()).build();
    }
}
