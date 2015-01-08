package de.learnlib.ralib.equivalence;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author falk
 */
public class IORandomWalk {

    //TODO: port equivalence test
    
//    private final Random rand;
//    private IORASimulatorOracle hyp;
//    private final IORASimulatorOracle target;
//    private final ParameterizedSymbol[] inputs;
//    private final boolean uniform;
//    private final double resetProbability;
//    private final long maxRuns;
//    private final boolean resetRuns;
//    private long runs;
//    
//    private final int maxDepth;
//    
//    private final Constants constants;
//
//    public IORandomWalk(Random rand, 
//            IORASimulatorOracle target, Alphabet inputs,
//            boolean uniform, double resetProbability, long maxRuns, int maxDepth,
//            Constants constants, boolean resetRuns) {
//        this.resetRuns = resetRuns;
//        this.rand = rand;
//        this.target = target;
//        this.inputs = new ParameterizedSymbol[inputs.size()];
//        int idx = 0;
//        for (Symbol s : inputs.getSymbolList()) {
//            ParameterizedSymbol ps = (ParameterizedSymbol) s;
//            this.inputs[idx++] = ps;
//        }
//
//        this.uniform = uniform;
//        this.resetProbability = resetProbability;
//        this.maxRuns = maxRuns;
//        this.constants = constants;
//        this.maxDepth = maxDepth;
//    }
//
//    public Word findCounterExample(IORAAutomaton hyp) {
//        updateHyp(hyp);
//        if (resetRuns) {
//            runs = 0;
//        }
//
//        while (runs < maxRuns) {
//            Word ce = run();
//            if (ce != null) {
//                return ce;
//            }
//        }
//
//        return null;
//    }
//
//    private Word run() {        
//        int depth = 0;
//        runs++;
//        List<DataValue> usedValsSys = new ArrayList<DataValue>();
//        List<DataValue> usedValsHyp = new ArrayList<DataValue>();
//        for (String k : this.constants.getKeys()) {
//            usedValsSys.add(this.constants.resolveLocal(new Reference(k)).getValue());
//            usedValsHyp.add(this.constants.resolveLocal(new Reference(k)).getValue());
//        }
//
//        Word testSys = new WordImpl();
//        Word testHyp = new WordImpl();
//        
//        hyp.reset();
//        target.reset();
//        
//        do {
//            PSymbolInstance[] in = pickInput(usedValsSys, usedValsHyp);
//            depth++;
//            PSymbolInstance outSys = target.step(in[0]);
//            PSymbolInstance outHyp = hyp.step(in[1]);
//
//            for (Object o : outHyp.getParameters()) {
//                if (!usedValsHyp.contains((DataValue) o)) {
//                    usedValsHyp.add((DataValue) o);
//                }
//            }          
//            for (Object o : outSys.getParameters()) {
//                if (!usedValsSys.contains((DataValue) o)) {
//                    usedValsSys.add((DataValue) o);
//                }
//            }     
//            
//            testSys = WordUtil.concat(WordUtil.concat(testSys, in[0]), outSys);
//            testHyp = WordUtil.concat(WordUtil.concat(testHyp, in[1]), outHyp);
//
//            Normalizer norm = new Normalizer(null, constants);
//            
//            Word traceSys = norm.normalize(testSys);
//            Word traceHyp = norm.normalize(testHyp);
//
//            if (!traceSys.equals(traceHyp)) {
//                //System.out.println("RUN: " + testSys);
//                return traceSys;
//            }
//                      
//        } while (rand.nextDouble() > resetProbability && depth < maxDepth);
//        
//        //System.out.println("RUN: " + testSys);
//        return null;
//    }
//
//    private PSymbolInstance[] pickInput(
//            List<DataValue> usedValsSys, List<DataValue> usedValsHyp) {
//        // pick symbol
//        ParameterizedSymbol ps = null;
//        if (uniform) {
//            ps = inputs[rand.nextInt(inputs.length)];
//        } else {
//            int MAX_WEIGHT = 0;
//            int[] weights = new int[inputs.length];
//            for (int i = 0; i < weights.length; i++) {
//                weights[i] = 1;
//                for (int j = 0; j < inputs[i].getArity(); j++) {
//                    weights[i] *= (usedValsSys.size() + j + 1);
//                }
//                MAX_WEIGHT += weights[i];
//            }
//
//            int idx = rand.nextInt(MAX_WEIGHT) + 1;
//            int sum = 0;
//            for (int i = 0; i < inputs.length; i++) {
//                sum += weights[i];
//                if (idx <= sum) {
//                    ps = inputs[i];
//                    break;
//                }
//            }
//        }
//        // pick data values
//        assert ps != null;
//        Object[] dataSys = new Object[ps.getArity()];
//        Object[] dataHyp = new Object[ps.getArity()];
//        for (int i = 0; i < ps.getArity(); i++) {
//            int idx = rand.nextInt(Math.max(usedValsSys.size() * 2, 1));
//            idx = Math.min(idx, usedValsSys.size());
//            DataValue dSys;
//            DataValue dHyp;
//            if (idx >= usedValsSys.size()) {
//                dSys = new DataValue(idx + 1);
//                dHyp = new DataValue(idx + 1);
//                usedValsSys.add(dSys);
//                usedValsHyp.add(dHyp);
//            } else {
//                dSys = usedValsSys.get(idx);
//                dHyp = usedValsHyp.get(idx);
//            }
//
//            dataSys[i] = dSys;
//            dataHyp[i] = dHyp; 
//        }
//
//        return new PSymbolInstance[] { 
//            new PSymbolInstance(ps, dataSys),
//            new PSymbolInstance(ps, dataHyp) };
//    }
//
//    
//    private void updateHyp(IORAAutomaton hyp) {        
//        this.hyp = new IORASimulatorOracle(hyp, new ValueGenerator() {
//            int vv = 2000000;
//            @Override
//            public DataValue generateValue(Register r, DataValue... blocked) {
//                return new DataValue(vv++);
//            }
//        });
//    }      

}
