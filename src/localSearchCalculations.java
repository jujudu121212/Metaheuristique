import java.util.ArrayList;
import java.util.HashMap;

public class localSearchCalculations implements Runnable {

    private int stepValue = 1;
    int compressNb = 0;
    int compressRNb = 0;
    int debug = 1;
    int initVal; 
    Boolean maxRatesComputed = false;
    HashMap<Integer, Integer> maxEvacRates;

    data globalData;
    Solution globalSol;
    Integer index;

    public localSearchCalculations(data d, Solution s, Integer index) {
        this.globalData = d;
        this.globalSol = s;
        this.index = index;
    }

    public void run() {
        int aVal = localSearchIntern(globalData,  globalSol);
        synchronized(LocalSearch.values) {
            LocalSearch.values.add(aVal);
        }
        synchronized(LocalSearch.counter) {
            LocalSearch.counter++;
        }
    }

    private ArrayList<Solution> computeNeighbours(data d, Solution initSol) {
        //System.out.println("Computing Neighbours...");
        ArrayList<Solution> neighbours = new ArrayList<Solution>();
        for(int evacNode : initSol.evacNodesList.keySet()) {
            int startDate = initSol.evacNodesList.get(evacNode).beginDate;
            int rate = initSol.evacNodesList.get(evacNode).evacRate;
            //System.out.println("start date = " + startDate + " ; stepValue = " + stepValue);
            if(startDate >= stepValue) {
                Solution bestSol = new Solution(initSol);
                bestSol.evacNodesList.put(evacNode, new EvacNodeData(rate, startDate-stepValue));
                //System.out.println("Node " + evacNode + " will evacuate at time " + (startDate-stepValue) + " instead of " + (startDate));
                neighbours.add(bestSol);
            }
        }
        return neighbours;
    }

    private Solution findBest(data d, Solution sol) {
        this.stepValue = sol.objectiveValue/2;
        int bestValue = sol.objectiveValue;
        if(debug >= 2) {
            System.out.println("[THREAD" + index + "] " + "[Compression phase " + compressNb + "] Looking for best compression... (initial value = " + bestValue + ")");
        }
        Solution bestSol = sol;
        int nb = 0;
        int stepValueInt = stepValue;
        Boolean foundBest = true;
        while(foundBest || (stepValue != 1)) { // stepValue accélère la première compression (car la borne sup est très grossière) puis vaut 1 pour la suite
            foundBest = false;
            stepValue = stepValueInt;
            for(Solution s : computeNeighbours(d, bestSol)) {
                Checker ch = new Checker();
                nb++;
                int val = ch.check(d, s).endingEvacTime;
                s.objectiveValue = val;
                if((val > 0) && (val <= bestValue)) {
                    foundBest = true;
                    if(val != bestValue)  {
                        if(debug >= 2) {
                            System.out.println("[THREAD" + index + "] " + "[Compression phase " + compressNb + "] Best solution found (cost = " + val + ")");
                        }
                    }
                    bestValue = val;
                    bestSol = s;
                }
            }
            if(!foundBest) {
                stepValueInt = stepValue / 2;
            }
        }
        if(debug >= 2) {
            System.out.println("[THREAD" + index + "] " + "[Compression phase " + compressNb + "] Best solution found at cost " + bestValue + "(explored  " + nb + " solutions)");
        }
        compressNb++;
        return findBestRate(d, bestSol);
    }

    private ArrayList<Solution> computeNeighboursRate(data d, Solution initSol) {
        //System.out.println("Computing Neighbours...");
        ArrayList<Solution> neighbours = new ArrayList<Solution>();
        for(int evacNode : initSol.evacNodesList.keySet()) {
            int startDate = initSol.evacNodesList.get(evacNode).beginDate;
            int rate = initSol.evacNodesList.get(evacNode).evacRate;
            //System.out.println("start date = " + startDate + " ; stepValue = " + stepValue);
            if(maxEvacRates.get(evacNode) > rate) {
                Solution bestSol = new Solution(initSol);
                bestSol.evacNodesList.put(evacNode, new EvacNodeData(rate+1, startDate));
                //System.out.println("Node " + evacNode + " will evacuate at time " + (startDate-stepValue) + " instead of " + (startDate));
                neighbours.add(bestSol);
            }
        }
        return neighbours;
    }

    private Solution findBestRate(data d, Solution sol) {
        if(!maxRatesComputed) { // if first time then we compute max rates once
            maxEvacRates = new HashMap<Integer, Integer>();
            for(Path_data evacPath : d.evac_paths.values()) {
                int currNode = evacPath.origin;
                int minCapa = Integer.MAX_VALUE;
                for(int nextNode : evacPath.following) {                   
                    if(d.getEdgeCapa(currNode, nextNode) < minCapa) {
                        minCapa = d.getEdgeCapa(currNode, nextNode);
                    }
                    currNode = nextNode;
                }              
                maxEvacRates.put(evacPath.origin, minCapa);
            }
            maxRatesComputed = true;
        } // Now maxEvacRates(x) is the max rate possible for evac path of node x
        int bestValue = sol.objectiveValue;
        if(debug >= 2) {
            System.out.println("[THREAD" + index + "] " + "[Rate augment phase " + compressRNb + "] ... (initial value = " + bestValue + ")");
        }
        Solution bestSol = sol;
        int nb = 0;    
        Boolean foundBest = true;
        while(foundBest) { // stepValue accélère la première compression (car la borne sup est très grossière) puis vaut 1 pour la suite
            foundBest = false;

            for(Solution s : computeNeighboursRate(d, bestSol)) {
                Checker ch = new Checker();
                nb++;
                int val = ch.check(d, s).endingEvacTime;
                s.objectiveValue = val;
                if((val > 0) && (val <= bestValue)) {
                    foundBest = true;
                    if(val != bestValue)  {
                        if(debug >= 2) {
                            System.out.println("[THREAD" + index + "] " + "[Rate augment phase " + compressRNb + "] Best solution found (cost = " + val + ")");
                        }
                    }
                    bestValue = val;
                    bestSol = s;
                }
            }
    
        }
        if(debug >= 2) {
            System.out.println("[THREAD" + index + "] " + "[Rate augment phase " + compressRNb + "] Best solution found at cost " + bestValue + "(explored  " + nb + " solutions)");
        }
        compressRNb++;
        return bestSol;
    }

    private ArrayList<Solution> modifyRates(data d, Solution sol) {
        if(debug >= 2) {
            System.out.println("[THREAD" + index + "] " + "Trying to reduce rates to allow parallelization...");
        }
        // find where the limitant edge is
        ArrayList<Solution> r = new ArrayList<Solution>();
        sol.objectiveValue = initVal*4;
        CheckerReturn solAnalysis = (new Checker(true)).check(d, sol);
        for(int aNode : solAnalysis.problematicNodes) { // Unfair reductions loop
            Solution bestSol = new Solution(sol);
            //System.out.println("pb at node " + aNode + " (exceedent flow = " + solAnalysis.exceedentFlow + ")");
            if(sol.evacNodesList.get(aNode).evacRate - solAnalysis.exceedentFlow > 0) {
                bestSol.evacNodesList.put(aNode, new EvacNodeData(sol.evacNodesList.get(aNode).evacRate - solAnalysis.exceedentFlow, sol.evacNodesList.get(aNode).beginDate));
                //System.out.println("Node " + aNode + " will now evacuate at rate " + bestSol.evacNodesList.get(aNode).evacRate + " instead of " + sol.evacNodesList.get(aNode).evacRate);
                r.add(bestSol);
            }
        }
        int total = solAnalysis.exceedentFlow;
        int average = (total/(solAnalysis.problematicNodes.size()));
        int rest = total%(solAnalysis.problematicNodes.size());
        // We also compute a "fair" solution
        Boolean first = true;
        Solution bestSolAv = new Solution(sol);
        for(int aNode : solAnalysis.problematicNodes) {
            if(first) { // choix arbitraire : si nbre non multiple on donne plus au premier noeud à évacuer
                first = false;
                bestSolAv.evacNodesList.put(aNode, new EvacNodeData(sol.evacNodesList.get(aNode).evacRate - average - rest, sol.evacNodesList.get(aNode).beginDate));
            }
            else {
                bestSolAv.evacNodesList.put(aNode, new EvacNodeData(sol.evacNodesList.get(aNode).evacRate - average, sol.evacNodesList.get(aNode).beginDate));
            }
        }
        r.add(bestSolAv);
        return r;
    }


    private Integer localSearchIntern(data d, Solution s) {
        this.initVal = s.objectiveValue;
        int test = (new Checker()).check(d, s).endingEvacTime;
        Solution baseSol = findBest(d,s);
        baseSol.objectiveValue = (new Checker()).check(d, baseSol).endingEvacTime;
        int bestValue = baseSol.objectiveValue;
        Solution bestSol = baseSol;
        int nb = 0;
        Boolean foundBest = true;
        if(debug >= 2) {
            System.out.println("[THREAD" + index + "] " + "[MAIN] Will try to improve solution at cost " + bestValue + " (former cost was " + test + "))");
        }
        ArrayList<Solution> explSolsList = new ArrayList<Solution>();
        int max_iterations = 5;
        int i = max_iterations;
        while(foundBest || i > 0) {
            ArrayList<Solution> explSolsListBis = new ArrayList<Solution>();
            foundBest = false;
            if(i == max_iterations) {               
                for(Solution s1 : computeNeighbours(d, bestSol)) { // boucle partant de la solution initiale dans tous les sens
                    ArrayList<Solution> solList = modifyRates(d, s1);
                    for(Solution sol : solList) {
                        sol.objectiveValue = initVal*4; // Pour ne pas que le checker fail à cause de ça (car on a sûrement allongé la durée de l'évac en diminuant le débit)
                        sol.objectiveValue = (new Checker()).check(d,sol).endingEvacTime;
                        if(sol.objectiveValue != -1) {
                            Solution compactedNewSol = findBest(d, sol);
                            //Solution compactedNewSol = (d, compactedSol);
                            int val = (new Checker()).check(d, compactedNewSol).endingEvacTime;
                            //explSolsList.add(compactedNewSol);
                            if((val > 0) && (val <= bestValue)) {
                                foundBest = true;
                                bestSol = compactedNewSol;
                                bestValue = val;
                                nb++;
                                i = max_iterations+1;
                            }
                            if(val > 0) {
                                explSolsListBis.add(compactedNewSol);
                            }
                        }
                        else {
                            //explSolsList.add(sol);
                        } 
                    }     
                                            
                }   
            } else {
                if(debug >= 2) {
                    System.out.println("[THREAD" + index + "] " + "Solution was not improved ; trying to improve generated solutions (loop " + i + " improving " + explSolsList.size() + " former sols)");
                }
                    //System.out.println(i + " : " + explSolsList.size());
                for(Solution s1 : explSolsList) { // boucle sur les solutions du coup précédent
                    stepValue = 1;
                    ArrayList<Solution> neighbours = computeNeighbours(d,s1);
                    //System.out.println("-->" + neighbours.size());
                    ArrayList<Solution> solList = new ArrayList<Solution>();
                    for(Solution k : neighbours) {
                        for(Solution l : modifyRates(d, k)) {
                            solList.add(l);
                        }
                    }
                    //System.out.println(":" + solList.size());
                    for(Solution sol : solList) {
                        sol.objectiveValue = initVal*4; // Pour ne pas que le checker fail à cause de ça (car on a sûrement allongé la durée de l'évac en diminuant le débit)
                        sol.objectiveValue = (new Checker()).check(d,sol).endingEvacTime;
                        if(sol.objectiveValue != -1) {
                            Solution compactedNewSol = findBest(d, sol);
                            int val = (new Checker()).check(d, compactedNewSol).endingEvacTime;
                            //explSolsList.add(compactedNewSol);
                            if((val > 0) && (val <= bestValue)) {
                                foundBest = true;
                                bestSol = compactedNewSol;
                                bestValue = val;
                                nb++;
                                i = max_iterations+1;
                            }
                            if(val > 0) {
                                explSolsListBis.add(compactedNewSol);
                            }
                        }
                        else {
                            //explSolsList.add(sol);
                        } 
                    }     
                                            
                } 
            }
            explSolsList = explSolsListBis;
            //System.out.println(explSolsListBis.size());
            i--;
        }
        (new Checker()).check(d, bestSol);
        if(debug >= 1) {
            System.out.println("[THREAD" + index + "] " + "Explored " + nb + " rate diminutions.");
            System.out.println("[THREAD" + index + "] " + "Best solution found at cost " + bestValue + "(explored  " + nb + " solutions)");     
        }  
        return bestValue;
    }

}