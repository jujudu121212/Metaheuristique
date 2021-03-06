import java.util.ArrayList;
import java.util.HashMap;

public class computeInfSup {

    private static int computeSumTimes(ArrayList<Integer> list) {
        int totalTime = 0;
        for(int time : list) {
            totalTime += time;
        }
        return totalTime;
    }

    private static int findMaxTime(ArrayList<Integer> list) {
        int max_time = Integer.MIN_VALUE;
        for(int time : list) {
            if(time > max_time) {
                max_time = time;
            }
        }
        return max_time;
    }
    
    private static ArrayList<Integer> computeEvacTimes(data inData){
        ArrayList<Integer> sol = new ArrayList<Integer>();
        for(Path_data aPath : inData.evac_paths.values()) { // iterates over all evac paths
            int travelTime = 0;
            // we compute the sum of the travel times of each edge on the path
            int currNode = aPath.origin;
            int minCapa = Integer.MAX_VALUE;
            for(int nextNode : aPath.following) {
                travelTime+=inData.getEdgeLength(currNode, nextNode);
                if(inData.getEdgeCapa(currNode, nextNode) < minCapa) {
                    minCapa = inData.getEdgeCapa(currNode, nextNode);
                }
                currNode = nextNode;
            }
            int nbPaquets = aPath.population / minCapa;
            if((aPath.population % minCapa) != 0) {
                nbPaquets++; // Pour "faire passer" le dernier paquet
            }
            sol.add(nbPaquets+travelTime);
        }
        return sol;
    }

    public static Solution computeInfSolution(data inData) {
        Long startTime = java.lang.System.currentTimeMillis();
        Solution sol = new Solution();
        sol.other = "Solution for a inf solution (might be impossible for real)";
        sol.method = "Automatically generated by computeInfSup.java methods";
        sol.instanceName = "Inf Solution auto-generated";
        sol.nature = false; // In general case
        sol.objectiveValue = computeInf(inData);
        sol.evacNodesNB = inData.evac_paths.size();
        sol.evacNodesList = new HashMap<Integer,EvacNodeData>();
        for(Path_data evacPath : inData.evac_paths.values()) {
            int evacNode = evacPath.origin;
            int rate = evacPath.max_rate; // this one is sometimes incorrect !
            int beginTime = 0; // all evacuation tasks begin at the same time (time 0)
            int currNode = evacPath.origin;
            int minCapa = Integer.MAX_VALUE;
            for(int nextNode : evacPath.following) {
                if(inData.getEdgeCapa(currNode, nextNode) < minCapa) {
                    minCapa = inData.getEdgeCapa(currNode, nextNode);
                }
                currNode = nextNode;
            }
            sol.evacNodesList.put(evacNode, new EvacNodeData(minCapa, beginTime));
        }
        Long endTime = java.lang.System.currentTimeMillis();
        sol.computeTime = ((Long)(endTime - startTime)).intValue();
        System.out.println("Inf Solution generated in " + sol.computeTime + " ms");
        return sol;
    }

    public static Solution computeSupSolution(data inData) {
        Long startTime = java.lang.System.currentTimeMillis();
        Solution sol = new Solution();
        sol.other = "Solution for a sup solution (should be always possible for real)";
        sol.method = "Automatically generated by computeInfSup.java methods";
        sol.instanceName = "Sup Solution auto-generated";
        sol.nature = true; // In general case
        //sol.objectiveValue = computeSup(inData);
        sol.evacNodesNB = inData.evac_paths.size();
        sol.evacNodesList = new HashMap<Integer,EvacNodeData>();
        int totalTime = 0;
        for(Path_data evacPath : inData.evac_paths.values()) {
            int evacNode = evacPath.origin;

            int travel_time = 0;
            // we compute the sum of the travel times of each edge on the path
            int currNode = evacPath.origin;
            int minCapa = Integer.MAX_VALUE;
            for(int nextNode : evacPath.following) {
                travel_time+=inData.getEdgeLength(currNode, nextNode);
                if(inData.getEdgeCapa(currNode, nextNode) < minCapa) {
                    minCapa = inData.getEdgeCapa(currNode, nextNode);
                }
                currNode = nextNode;
            }
            int evacTime = evacPath.population / minCapa;
            if((evacPath.population % minCapa) != 0) {
                evacTime++; // Pour "faire passer" le dernier paquet
            }
            evacTime+=travel_time; // durée de trajet des paquets jusqu'au point sûr
            
            int beginTime = totalTime; 
            totalTime+=evacTime; // next evacuation will begin while this one is totally finished
            sol.evacNodesList.put(evacNode, new EvacNodeData(minCapa, beginTime));
        }
        sol.objectiveValue = totalTime;
        Long endTime = java.lang.System.currentTimeMillis();
        sol.computeTime = ((Long)(endTime - startTime)).intValue();
        System.out.println("Sup Solution generated in " + sol.computeTime + " ms");
        return sol;
    }

    public static int computeInf(data inData) {
        ArrayList<Integer> evacTimes = computeEvacTimes(inData);
        int max_time = findMaxTime(evacTimes);
        return max_time;
    }

    public static int computeSup(data inData) {
        ArrayList<Integer> evacTimes = computeEvacTimes(inData);
        int sum_times = computeSumTimes(evacTimes);
        return sum_times;
    }

    /**
    @return a list whose first element is the inf and second element is the sup
    */
    public static ArrayList<Integer> computeInfAndSup(data inData) { // Because computeInf and computeMax perform the same calculations
        ArrayList<Integer> sol = new ArrayList<Integer>();
        ArrayList<Integer> evacTimes = computeEvacTimes(inData);
        int max_time = findMaxTime(evacTimes);
        int sum_times = computeSumTimes(evacTimes);
        sol.add(max_time);
        sol.add(sum_times);
        return sol;
    }

}
