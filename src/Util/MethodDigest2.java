package Util;

import soot.*;
import soot.Body;
import soot.jimple.IfStmt;
import soot.jimple.internal.JLookupSwitchStmt;
import soot.jimple.internal.JTableSwitchStmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class MethodDigest2 {
    public List<List<Unit>> realUnitsPaths = new LinkedList<>();

    public MethodDigest2(Body body) {
        //filter
        int log_stat_counter = 0;
        for (Unit unit : body.getUnits()) {
            if (unit.toString().toLowerCase().contains("logg"))
            {
                log_stat_counter+=1;
            }
        }
        //false
        if (false)
        {
            this.realUnitsPaths = new LinkedList<>();
        }

        else {

            //System.out.println("counter!");

            enumMethodPath(body);
            /*
            List<List<Unit>> remove_candidates = new LinkedList<>();
            for (List<Unit> realUnitsPath : this.realUnitsPaths)
            {
                int realUnitsPath_flag = 0;
                for (Unit unit : realUnitsPath)
                {
                    if(unit.toString().toLowerCase().contains("logg"))
                    {
                        realUnitsPath_flag+=1;
                        //System.out.println(unit.toString());
                    }
                }
                if(realUnitsPath_flag==0)
                {
                    //this.realUnitsPaths.remove(realUnitsPath);
                    remove_candidates.add(realUnitsPath);
                }
            }
            for(List<Unit> remove_candidate : remove_candidates)
            {
                this.realUnitsPaths.remove(remove_candidate);
            }

             */

        }
    }

    public void enumMethodPath(Body body) {//Traversing paths and filtering by predicate
        BriefBlockGraph bg = new BriefBlockGraph(body);
        modifyBlockGraph(bg, body);
        int num = bg.getBlocks().size();
        boolean[][] flags = new boolean[num][num];
        List<Block> heads = bg.getHeads();
        Block head = heads.get(0);
        List<Unit> unitPath = new ArrayList<>();
        traverseBlock(unitPath, head, flags);
    }

    public void traverseBlock(List<Unit> unitPath,
                              Block head, boolean[][] flags) {
        if (realUnitsPaths.size() >= 30000) {// Limit the number of paths
            return;
        }
        unitPath.addAll(getBlockUnits(head));//Add the information in the head to the list
        List<Block> nextBlocks = head.getSuccs();
        if (nextBlocks.isEmpty()) {
            this.realUnitsPaths.add(unitPath);
            //logger.info('xxx'+newsize+'yyy')
            return;
        }
        if (checkUnitIsPredicate(head.getTail())) {//If the block does not contain a predicate, you can save space by not saving the snapshot
            for (Block block : nextBlocks) {
                if (checkCycle(head, block, flags) == 1)
                    continue;
                List<Unit> newUnitPath = new ArrayList<>(unitPath);
                traverseBlock(newUnitPath, block, flags);
                flags[head.getIndexInMethod()][block.getIndexInMethod()] = false;
            }
        } else {
            Block block = nextBlocks.get(0);
            if (checkCycle(head, block, flags) == 1)
                return;
            traverseBlock(unitPath ,block, flags );
            flags[head.getIndexInMethod()][block.getIndexInMethod()] = false;
        }
    }

    public List<Unit> getBlockUnits(Block block) {
        List<Unit> paths = new ArrayList<>();
        for (Unit unit : block) {
            paths.add(unit);
        }
        return paths;
    }

    public int checkCycle(Block head, Block nextBlock, boolean[][] flags) {//Determine if the loop needs to be terminated
        int headIndex = head.getIndexInMethod();
        int nextIndex = nextBlock.getIndexInMethod();
        if (flags[headIndex][nextIndex]) {
            return 1;
        }
        flags[headIndex][nextIndex] = true;
        return 0;
    }

    /*
    public List<Unit> markUnitinLoop(Body body)
    {
        List<Unit> unitPaths = new ArrayList<>();


        return unitPaths;

    }
*/
    public boolean checkUnitIsPredicate(Unit unit) {
        return unit instanceof IfStmt || unit instanceof JLookupSwitchStmt || unit instanceof JTableSwitchStmt;
    }

    private void modifyBlockGraph(BriefBlockGraph bg, Body body) {//Handling try...catch statements
        List<Block> blocks = bg.getBlocks();
        Chain<Trap> traps = body.getTraps();
        for (Trap trap : traps) {
//            Unit begin = trap.getBeginUnit();
            Unit end = trap.getEndUnit();
            Unit handler = trap.getHandlerUnit();
            //the first integer is the index of the endBlock and the second integer is the index of the handlerBlock
            int[] blockIndex = getBlockIndexOfUnit(blocks, end, handler);
            Block endBlock = blocks.get(blockIndex[0]);
            Block handlerBlock = blocks.get(blockIndex[1]);
            List<Block> preds = new ArrayList<>(handlerBlock.getPreds());
            if (!preds.contains(endBlock))
                preds.add(endBlock);
            List<Block> succs = new ArrayList<>(endBlock.getSuccs());
            if (!succs.contains(handlerBlock))
                succs.add(handlerBlock);
            endBlock.setSuccs(succs);
            handlerBlock.setPreds(preds);
        }
    }

    private int[] getBlockIndexOfUnit(List<Block> blocks, Unit end, Unit handler) {//Get the index of the block where the unit is located
        int[] index = new int[2];
        for (Block block : blocks) {
            for (Unit value : block) {
                if (value.equals(end)) {
                    index[0] = block.getIndexInMethod();
                }
                if (value.equals(handler)) {
                    index[1] = block.getIndexInMethod();
                }
            }
        }
        return index;
    }


    public static String getParamForm(SootMethod method) {
        StringBuilder sb = new StringBuilder();
        Type ret = method.getReturnType();
        if (ret instanceof RefType)
            sb.append(ret).append(",");
        else if (ret instanceof ArrayType)
            sb.append(((ArrayType) ret).baseType).append(","); // ignore array
        else sb.append(ret).append(",");

        for (Type t : method.getParameterTypes()) {
            if (t instanceof RefType)
                sb.append(t).append(",");
            else if (t instanceof ArrayType)
                sb.append(((ArrayType) t).baseType).append(","); // ignore array
            else sb.append(t).append(",");
        }
        return sb.toString();
    }
}


