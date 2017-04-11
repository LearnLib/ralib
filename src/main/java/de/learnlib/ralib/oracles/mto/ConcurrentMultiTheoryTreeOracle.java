package de.learnlib.ralib.oracles.mto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.GeneralizedSymbolicSuffix;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class ConcurrentMultiTheoryTreeOracle extends MultiTheoryTreeOracle {

	private DataWordOracle concurrentOracle;
	private Map<SDTQuery, DefaultQuery<PSymbolInstance, Boolean>> queriesToExecute;

	public ConcurrentMultiTheoryTreeOracle(DataWordOracle membershipOracle, IOOracle traceOracle,
			Map<DataType, Theory> teachers, Constants constants, ConstraintSolver solver) {
		super(membershipOracle, traceOracle, teachers, constants, solver);
		this.concurrentOracle = membershipOracle;
		this.queriesToExecute = new LinkedHashMap<>();
	}

    public void concurrentTreeQuery(
    		SDTQuery sdtQuery,
            Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix,
            WordValuation values, PIV piv,
            Constants constants, SuffixValuation suffixValues) {
        if (values.size() == DataWords.paramLength(suffix.getActions())) {
            Word<PSymbolInstance> concSuffix = DataWords.instantiate(
                    suffix.getActions(), values);
            DefaultQuery<PSymbolInstance, Boolean> query
                    = new DefaultQuery<>(prefix, concSuffix);
            queriesToExecute.put(sdtQuery, query);        
        } else {
        	SDT sdt = super.treeQuery(prefix, suffix, values, piv, constants, suffixValues);
        	sdtQuery.setAnswer(sdt);
        }
    }
    
    public void processConcurrentTreeQueries() {
    	List<DefaultQuery<PSymbolInstance, Boolean>> queries = new ArrayList<>(queriesToExecute.values());
    	this.concurrentOracle.processQueries(queries);
    	for (SDTQuery id : queriesToExecute.keySet()) {
    		DefaultQuery<PSymbolInstance, Boolean> query = queriesToExecute.get(id);
    		Boolean output = query.getOutput();
    		id.setAnswer(output.equals(Boolean.TRUE) ? SDTLeaf.ACCEPTING  : SDTLeaf.REJECTING);
    	}
    	this.queriesToExecute.clear();
    }


}
