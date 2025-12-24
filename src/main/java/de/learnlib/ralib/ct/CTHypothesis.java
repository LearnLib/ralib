package de.learnlib.ralib.ct;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.checkerframework.checker.nullness.qual.Nullable;

import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RARun;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.ParameterValuation;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

/**
 * Hypothesis constructed from a {@link ClassificationTree}. Maintains a mapping between
 * locations in the hypothesis and leaf nodes of the {@code ClassificationTree} from which the
 * hypothesis was constructed.
 *
 * @author fredrik
 * @see Hypothesis
 */
public class CTHypothesis extends Hypothesis {

	private final BiMap<CTLeaf, RALocation> leaves;
	private RALocation sink = null;
	private final boolean ioMode;

	public CTHypothesis(Constants consts, int leaves, boolean ioMode) {
		super(consts);
		this.ioMode = ioMode;
		this.leaves = HashBiMap.create(leaves);
	}

	public CTHypothesis(Constants consts, Map<CTLeaf, RALocation> leaves, boolean ioMode) {
		super(consts);
		this.ioMode = ioMode;
		this.leaves = HashBiMap.create(leaves.size());
		this.leaves.putAll(leaves);
	}

	public void putLeaves(Map<CTLeaf, RALocation> leaves) {
		this.leaves.putAll(leaves);
	}

	public void setSink(RALocation sink) {
		this.sink = sink;
	}

	public RALocation getSink() {
		return sink;
	}

	@Override
	public @Nullable RALocation getSuccessor(RALocation state, ParameterizedSymbol input) {
		return super.getSuccessor(state, input);
	}

	/**
	 * Get location corresponding to {@code leaf}.
	 *
	 * @param leaf
	 * @return {@link RALocation} corresponding to {@code leaf}
	 */
	public RALocation getLocation(CTLeaf leaf) {
		return leaves.get(leaf);
	}

	/**
	 * Get leaf node of {@link ClassificationTree} from which this hypothesis was constructed.
	 * which corresponds to {@code location}.
	 *
	 * @param location
	 * @return leaf node corresponding to {@code location}
	 * @see CTLeaf
	 * @see RALocation
	 */
	public CTLeaf getLeaf(RALocation location) {
		return leaves.inverse().get(location);
	}

	@Override
	protected List<Transition> getTransitions(Word<PSymbolInstance> dw) {
		List<Transition> tseq = new LinkedList<>();
		RARun run = getRun(dw);
		for (int i = 1; i <= dw.size(); i++) {
			tseq.add(run.getRATransition(i));
		}
		return tseq;
	}

	@Override
	public RALocation getLocation(Word<PSymbolInstance> dw) {
		return getRun(dw).getLocation(dw.size());
	}

	@Override
    public RARun getRun(Word<PSymbolInstance> word) {
        int n = word.length();
        RALocation[] locs = new RALocation[n+1];
        RegisterValuation[] vals = new RegisterValuation[n+1];
        PSymbolInstance[] symbols = new PSymbolInstance[n];
        Transition[] transitions = new Transition[n];

        locs[0] = getInitialState();
        vals[0] = new RegisterValuation();

        for (int i = 0; i < n; i++) {
            symbols[i] = word.getSymbol(i);
            ParameterValuation pars = ParameterValuation.fromPSymbolInstance(symbols[i]);

            Collection<Transition> candidates = locs[i].getOut(symbols[i].getBaseSymbol());
            if (candidates == null) {
                return null;
            }

            boolean found = false;
            boolean output = candidates.isEmpty() ? false :
            	candidates.iterator().next() instanceof OutputTransition;

            for (Transition t : candidates) {
                if (t.isEnabled(vals[i], pars, constants)) {
                	transitions[i] = t;
                	vals[i+1] = t.valuation(vals[i], pars, constants);
                    locs[i+1] = t.getDestination();
                    found = true;
                    break;
                }
            }

            if (!found) {
            	// in IO automata, invalid sequences go to sink
            	if (ioMode) {
            		if ((symbols[i].getBaseSymbol() instanceof OutputSymbol && (output || candidates.isEmpty()))
            				|| locs[i].equals(sink)) {
            			vals[i+1] = new RegisterValuation();
            			locs[i+1] = getSink();
            			transitions[i] = createSinkTransition(locs[i], locs[i+1], symbols[i].getBaseSymbol());
            		}
            	} else {
            		return null;
            	}
            }
        }

        return new RARun(locs, vals, symbols, transitions);
    }

	private Transition createSinkTransition(RALocation src, RALocation dest, ParameterizedSymbol ps) {
		if (ps instanceof OutputSymbol outputSymbol) {
			return new OutputTransition(new OutputMapping(),
					outputSymbol,
					src, dest,
					new Assignment(new VarMapping<>()));
		}
		if (ps instanceof InputSymbol inputSymbol) {
			return new InputTransition(ExpressionUtil.TRUE,
					inputSymbol,
					src, dest,
					new Assignment(new VarMapping<>()));
		}
		throw new IllegalArgumentException("Not input or output symbol: " + ps);
	}
}
