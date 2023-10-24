package fcul.tdf.objects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockTree {
    public static Map<Integer, List<Block>> BlockTree = new HashMap<>();

    public static void addBlock(Block block){
        if(BlockTree.containsKey(block.getEpoch())){
            BlockTree.get(block.getEpoch()).add(block);
        }else{
            BlockTree.put(block.getEpoch(), List.of(block));
        }
    }

    public static boolean verifyGenesisBlock(Block block){
        return block.getEpoch() == 0 && block.getLength() == 0;
    }
}
