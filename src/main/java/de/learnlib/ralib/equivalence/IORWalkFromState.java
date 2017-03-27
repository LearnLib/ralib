package de.learnlib.ralib.equivalence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.exceptions.SULRestartException;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class IORWalkFromState implements IOEquivalenceOracle{
	
	private IOHypVerifier hypVerifier;
	private DataWordSUL target;
	private Random rand;
	private boolean resetRuns;
	private ParameterizedSymbol[] inputs;
	private boolean uniform;
	private double resetProbability;
	private long maxRuns;
	private Constants constants;
	private int maxDepth;
	private Map<DataType, Theory> teachers;
	private double drawRegister = 0.4; 
	private double drawHistory = 0.2;
	private double drawRelated=0.2; 
	private AccessSequenceProvider accSeqProvider;
	private RegisterAutomaton hyp;
	private int runs;
	private ParameterizedSymbol error;
	private static LearnLogger log = LearnLogger.getLogger(IORWalkFromState.class);

	public IORWalkFromState(Random rand, DataWordSUL target, boolean uniform,
            double resetProbability, double regProb, double hisProb, double relatedProb, long maxRuns, int maxDepth, Constants constants,
            boolean resetRuns, Map<DataType, Theory> teachers, AccessSequenceProvider accessSequenceProvider, ParameterizedSymbol... inputs) {

        this.resetRuns = resetRuns;
        this.rand = rand;
        this.target = target;
        this.hypVerifier = new IOHypVerifier(teachers, constants);
        this.inputs = inputs;
        this.uniform = uniform;
        this.resetProbability = resetProbability;
        this.drawHistory = hisProb;
        this.drawRegister = regProb;
        this.drawRelated = relatedProb;
        this.maxRuns = maxRuns;
        this.constants = constants;
        this.maxDepth = maxDepth;
        this.teachers = teachers;
        this.accSeqProvider = accessSequenceProvider;
    }

    @Override
    public DefaultQuery<PSymbolInstance, Boolean> findCounterExample(
            RegisterAutomaton a, Collection<? extends PSymbolInstance> clctn) {

        if (clctn != null && !clctn.isEmpty()) {
            log.warning("set of inputs is ignored by this equivalence oracle");
        }
        
        this.hyp = a;
		// reset the counter for number of runs?
        if (resetRuns) {
            runs = 0;
        }
        // find counterexample ...
        while (runs < maxRuns) {
            Word<PSymbolInstance> ce = runExc();
            if (ce != null) {
                return new DefaultQuery<>(ce, true);
            }
        }
        return null;
    }
    
    private Word<PSymbolInstance> runExc() {
    	Word<PSymbolInstance> run =null;
    	try {
    		run = this.run();
    	} catch(SULRestartException exc) {
    		run = this.runExc();
    	}
    	return run;
    }

    private Word<PSymbolInstance> run() {
        int depth = 0;
        runs++;
        target.pre();
        Word<PSymbolInstance> run = Word.epsilon();
        PSymbolInstance out;
        Word<PSymbolInstance> accessSequence = pickRandomAccessSequence(this.hyp, this.accSeqProvider, this.rand);
        for (int i=0; i < accessSequence.length(); i=i+2) {
        	PSymbolInstance next = accessSequence.getSymbol(i);
        	run = run.append(next);
        	out = target.step(next);
        	run =run.append(out);
        	if (this.hypVerifier.isCEForHyp(run, hyp) != null) 
        		throw new DecoratedRuntimeException("An access sequence should never yield a CE")
        		.addDecoration("access sequence", accessSequence).addDecoration("ce run", run);
        } 
        	
        try {
	        do {
	            PSymbolInstance next = nextInput(run);
	            depth++;
	            out = null;
	            run = run.append(next);
	            out = target.step(next);
	            run = run.append(out);
	
	            if (this.hypVerifier.isCEForHyp(run, hyp) != null) {
	            //	if (!run.toString().contains("CLOSE") ||  !run.toString().contains("OFA")) { 
		                log.log(Level.FINE, "Run with CE: {0}", run);     
		                System.out.format("Run with CE: {0}", run);
		                target.post();
		                
		                return run;
//	            	} else {
//	            		target.post();
//	            		System.out.println("IGNORED " + run);
//	            		return null;
//	            	}
	            }
	        } while (rand.nextDouble() > resetProbability && depth < maxDepth && 
	                !out.getBaseSymbol().equals(error));
        } catch(DecoratedRuntimeException exc) {
        	throw exc.addDecoration("run", run);
        }
//        System.out.println(run);
        log.log(Level.FINE, "Run /wo CE: {0}", run);
        target.post();
        return null;
    }
    
    private Word<PSymbolInstance> pickRandomAccessSequence(RegisterAutomaton hyp, AccessSequenceProvider accSeqProvider, Random rand) {
        Collection<RALocation> locations = hyp.getInputStates();
        RALocation randLocation = new ArrayList<>(locations).get(this.rand.nextInt(locations.size()));
        Word<PSymbolInstance> randAccSeq = this.accSeqProvider.getAccessSequence(hyp, randLocation);
        return randAccSeq;
    }
     
    public void setError(ParameterizedSymbol error) {
        this.error = error;
    }

    private PSymbolInstance nextInput(Word<PSymbolInstance> run) {
        ParameterizedSymbol ps = nextSymbol(run); 
        RALocation location = this.hyp.getLocation(run);
        List<Transition> transitions = this.hyp.getInputTransitions().stream()
        		.filter(tr -> tr.getSource().equals(location))
        		.collect(Collectors.toList());
        Transition trans = transitions.get(this.rand.nextInt(transitions.size()));
        trans.getGuard().getCondition();
        PSymbolInstance psi = nextDataValues(run, ps);
        return psi;
    }

    private PSymbolInstance nextDataValues(
            Word<PSymbolInstance> run, ParameterizedSymbol ps) {

        DataValue[] vals = new DataValue[ps.getArity()];

        int i = 0;
        for (DataType t : ps.getPtypes()) {
            Theory teacher = teachers.get(t);
            // TODO: generics hack?
            // TODO: add constants?
            Set<DataValue<Object>> oldSet = DataWords.valSet(run, t);
            for (int j = 0; j < i; j++) {
                if (vals[j].getType().equals(t)) {
                    oldSet.add(vals[j]);
                }
            }
            List<DataValue<Object>> old = new ArrayList<>(oldSet);
        	List<DataValue<Object>> regs = getRegisterValuesForType(run, t);
            Double draw = rand.nextDouble();
            if (draw <= drawRegister && !regs.isEmpty()) {
            	 vals[i] = pick(regs);
            }
            
          	List<DataValue<Object>> history = new ArrayList<>(oldSet);
        	history.removeAll(regs);
            if (draw > drawRegister && draw <= drawHistory + drawRegister && !history.isEmpty()) {
               	 vals[i] = pick(history);
            }
            
            List<DataValue<Object>> related = new ArrayList<>(oldSet);
            related = new ArrayList<>(teacher.getAllNextValues(related));
            if (draw > drawRegister + drawHistory && draw <= drawRegister + drawHistory + drawRelated && !related.isEmpty()) 
            	vals[i] = pick(related);
            
            if (vals[i] == null)
            	vals[i] = teacher.getFreshValue(old);
            i++;
        }
        return new PSymbolInstance(ps, vals);
    }
    
    private <T> T pick(List<T> list) {
    	return list.get(rand.nextInt(list.size()));
    }

	private List<DataValue<Object>> getRegisterValuesForType(Word<PSymbolInstance> run, DataType t) {
		return hyp.getRegisterValuation(run).values().stream()
				.filter(reg -> reg.getType().equals(t))
				.map(dv -> (DataValue<Object>) dv)
				.collect(Collectors.toList());
	}

    private ParameterizedSymbol nextSymbol(Word<PSymbolInstance> run) {
        ParameterizedSymbol ps = null;
        Map<DataType, Integer> tCount = new LinkedHashMap<>();
        if (uniform) {
            ps = inputs[rand.nextInt(inputs.length)];
        } else {
            int MAX_WEIGHT = 0;
            int[] weights = new int[inputs.length];
            for (int i = 0; i < weights.length; i++) {
                weights[i] = 1;
                for (DataType t : inputs[i].getPtypes()) {
                    Integer old = tCount.get(t);
                    if (old == null) {
                        // TODO: what about constants?
                        old = 0;
                    }
                    weights[i] *= (old + 1);
                    tCount.put(t, ++old);
                }
                MAX_WEIGHT += weights[i];
            }

            int idx = rand.nextInt(MAX_WEIGHT) + 1;
            int sum = 0;
            for (int i = 0; i < inputs.length; i++) {
                sum += weights[i];
                if (idx <= sum) {
                    ps = inputs[i];
                    break;
                }
            }
        }
        return ps;
    }

}
