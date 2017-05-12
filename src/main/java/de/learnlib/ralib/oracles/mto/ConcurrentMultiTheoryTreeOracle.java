package de.learnlib.ralib.oracles.mto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.io.ConcurrentIOCacheOracle;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.tools.classanalyzer.SpecialSymbols;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

// Assumes a cache is shared among the two oracles. Anticipates traces to be run in a future tree query and runs them in batch 
// (possibly concurrently).
public class ConcurrentMultiTheoryTreeOracle extends MultiTheoryTreeOracle {

	private IOOracle cachedIOOracle;
	private Map<DataType, Theory> teach;

	public ConcurrentMultiTheoryTreeOracle(DataWordOracle membershipOracle, IOOracle traceOracle,
			Map<DataType, Theory> teachers, Constants constants, ConstraintSolver solver) {
		super(membershipOracle, traceOracle, teachers, constants, solver);
		this.cachedIOOracle = traceOracle;
		this.teach = teachers;
	}
    
    public void processTreeQueryBatch(Collection<SDTQuery> sdtQueries,  Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix, 
    		PIV piv, Constants constants) {
    	List<Word<PSymbolInstance>> anticipatedTraces = new LinkedList<>();
    	for (SDTQuery sdtQuery : sdtQueries) {
    		Word<PSymbolInstance> trace = this.anticipateTrace(prefix, suffix, sdtQuery.getWordValuation(), sdtQuery.getSuffValuation());
    		if (trace != null && !anticipatedTraces.stream().anyMatch(tr -> tr.asList().equals(trace.asList()))) {
    			anticipatedTraces.add(trace);
    		}
    	}
    	
    	if (anticipatedTraces.size() > 0) {
    		this.cachedIOOracle.traces(anticipatedTraces);
//    		System.out.println("Anticipated for " + anticipatedTraces.size() + " traces.");
//    		anticipatedTraces.forEach(tr -> {
//    			System.out.println(tr.toString());
//    		});
    	} 
//    	else {
//    		System.out.println("Couldn't anticipate for: " + prefix + " " + suffix);
//    	}
    	
    	// some hard coded check to ensure that no actual run is made on the SUL
    	ConcurrentIOCacheOracle.noRun = true;
    	
    	for (SDTQuery sdtQuery : sdtQueries) {
    		SDT sdt = super.treeQuery( prefix, suffix, sdtQuery.getWordValuation(), piv, constants, sdtQuery.getSuffValuation());
    		sdtQuery.setAnswer(sdt);
    	}
    	
    	ConcurrentIOCacheOracle.noRun = false;
    }
    
    
    // returns an anticipative trace that will have to be run in order to execute the tree query
    // or null if there is no anticipative trace (because there are input suffix parameters not yet instantiated).
    // anticipative traces can be run concurrently and will update the cache. Assumes presence of the cache
    // otherwise you are running the same thing twice. 
    public Word<PSymbolInstance> anticipateTrace(Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix,
            WordValuation values,SuffixValuation suffixValues) {
    	Word<PSymbolInstance> trace = null; 
    	if (values.size() == DataWords.paramLength(suffix.getActions())) {
             Word<PSymbolInstance> concSuffix = DataWords.instantiate(
                     suffix.getActions(), values);
             trace = prefix.concat(concSuffix);
    	 } else {
    		 Word<ParameterizedSymbol> instantiatableActions = suffix.getActions();
    		 WordValuation newValues = new WordValuation(values);
    		  
    		 // what are the actions that we need to instantiate ?
    		 while (!instantiatableActions.isEmpty() && (instantiatableActions.lastSymbol().getArity() == 0 || instantiatableActions.lastSymbol() instanceof OutputSymbol))
    			 instantiatableActions = instantiatableActions.prefix(-1);
    		 
    		 // in case we don't have all values needed to instantiate the suffix, look ahead and instantiate any fresh suffix params
    		 int pIndex = DataWords.paramLength(instantiatableActions);
    		 if (!instantiatableActions.isEmpty() && values.size() < pIndex) {
    			 ParameterizedSymbol act = instantiatableActions.lastSymbol();
    			 assert act instanceof InputSymbol;
    			 for (int i= 0; i<act.getArity(); i++) {
    				 if (!newValues.containsKey(pIndex-i) && suffix.isFresh(pIndex-i)) {
    					 DataType t = act.getPtypes()[act.getArity()-1-i];
    					 Theory th = teach.get(t);
    					 List vals = new ArrayList(DataWords.valSet(prefix,t));
    					 vals.addAll(newValues.values(t));
    					 DataValue fv = th.getFreshValue(vals);
    					 newValues.put(pIndex-i, fv);
    				 } else 
    					 break;
    			 }
    		 }
    		 
    		 if (newValues.size() == DataWords.paramLength(instantiatableActions)) {
    			 Word<ParameterizedSymbol> leftToInstantiate = suffix.getActions().suffix(instantiatableActions.length() * (-1));
    			 Word<ParameterizedSymbol> nonParamActions = leftToInstantiate.transform(a -> { 
    			 	if (a instanceof OutputSymbol) 
    			 		return SpecialSymbols.VOID; // add some parameterless output, it will be replaced by the actual output anyway
    			 	return a; });
    			 Word<ParameterizedSymbol> newActions = instantiatableActions.concat(nonParamActions);
    			 Word<PSymbolInstance> concSuffix = DataWords.instantiate(
                         newActions, newValues);
    			 
    			 trace = prefix.concat(concSuffix);
    			 
    		 }	
    		 
    	 }
    	
    	return trace;
    }


}
