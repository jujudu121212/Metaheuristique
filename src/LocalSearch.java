import java.util.ArrayList;
import java.lang.Object;

public class LocalSearch {

    private ArrayList<Solution> computeNeighbours(data d, Solution initSol) {
        ArrayList<Solution> neighbours = new ArrayList<Solution>();
        for(int evacNode : initSol.evacNodesList.keySet()) {
            int startDate = initSol.evacNodesList.get(evacNode).beginDate;
            int rate = initSol.evacNodesList.get(evacNode).evacRate;
            if(startDate > 0) {
                Solution bestSol = new Solution(initSol);
                bestSol.evacNodesList.put(evacNode, new EvacNodeData(rate, startDate-1));
                System.out.println("Node " + evacNode + " will evacuate at time " + startDate + " instead of " + (startDate +1));
                neighbours.add(bestSol);
            }
        }
        return neighbours;
    }

    private Solution findBest(data d, Solution sol) {
        System.out.println("Beginning local search");
        int bestValue = sol.objectiveValue;
        Solution bestSol = sol;
        int nb = 0;
        Boolean foundBest = true;
        while(foundBest) {
            foundBest = false;
            for(Solution s : computeNeighbours(d, bestSol)) {
                Checker ch = new Checker();
                nb++;
                int val = ch.check(d, s).endingEvacTime;
                if((val > 0) && (val <= bestValue)) {
                    foundBest = true;
                    System.out.println("Best solution found !");
                    bestValue = val;
                    bestSol = s;
                }
            }
        }
        System.out.println("Best solution found at cost " + bestValue + "(explored  " + nb + " solutions)");
        return bestSol;
    }

    private ArrayList<Solution> modifyRates(data d, Solution sol) {
        // find where the limitant edge is
        ArrayList<Solution> r = new ArrayList<Solution>();
        CheckerReturn solAnalysis = (new Checker()).check(d, sol);
        for(int aNode : solAnalysis.problematicNodes) {
            Solution bestSol = new Solution(sol);
            System.out.println("pb at node " + aNode + " (exceedent flow = " + solAnalysis.exceedentFlow + ")");
            bestSol.evacNodesList.put(aNode, new EvacNodeData(sol.evacNodesList.get(aNode).evacRate - solAnalysis.exceedentFlow, sol.evacNodesList.get(aNode).beginDate));
            System.out.println("Node " + aNode + " will now evacuate at rate " + bestSol.evacNodesList.get(aNode).evacRate + " instead of " + sol.evacNodesList.get(aNode).evacRate);
            r.add(bestSol);
        }
        return r;
    }

    public void localSearch(data d, Solution s) {
        Solution baseSol = findBest(d,s);
        baseSol.objectiveValue = (new Checker()).check(d, baseSol).endingEvacTime;
        int bestValue = baseSol.objectiveValue;
        Solution bestSol = baseSol;
        int nb = 0;
        Boolean foundBest = true;
        System.out.println("\n\n\n Will try to improve solution at cost " + bestValue + ")");
        while(foundBest) {
            foundBest = false;
            for(Solution s1 : computeNeighbours(d, bestSol)) {
                ArrayList<Solution> solList = modifyRates(d, s1);
                for(Solution sol : solList) {
                    sol.objectiveValue = (new Checker()).check(d,sol).endingEvacTime;
                    Solution compactedNewSol = findBest(d, sol);
                    int val = (new Checker()).check(d, compactedNewSol).endingEvacTime;
                    if((val > 0) && (val <= bestValue)) {
                        foundBest = true;
                        bestSol = compactedNewSol;
                        bestValue = val;
                        nb++;
                        }
                    }
            }
        }
        System.out.println("Explored " + nb + " rate diminutions.");
        System.out.println("Best solution found at cost " + bestValue + "(explored  " + nb + " solutions)");
        (new Checker()).check(d, bestSol);
    }

}