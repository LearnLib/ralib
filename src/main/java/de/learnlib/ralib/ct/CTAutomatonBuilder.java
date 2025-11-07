package de.learnlib.ralib.ct;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParameterValuation;
import de.learnlib.ralib.data.RegisterAssignment;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.learning.AutomatonBuilder;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.smt.ReplacingValuesVisitor;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

public class CTAutomatonBuilder {

	private final ClassificationTree ct;

	private final Map<Word<PSymbolInstance>, RALocation> locations;
	private final Map<CTLeaf, RALocation> leaves;

	private final CTHypothesis hyp;

	private final ConstraintSolver solver;

	private boolean ioMode;

	public CTAutomatonBuilder(ClassificationTree ct, Constants consts, boolean ioMode, ConstraintSolver solver) {
		this.ct = ct;
		this.ioMode = ioMode;
		this.solver = solver;

		locations = new LinkedHashMap<>();
		leaves = new LinkedHashMap<>();
		hyp = new CTHypothesis(consts, ct.getLeaves().size(), ioMode);
	}

	public CTHypothesis buildHypothesis() {
		computeLocations();
		computeTransitions();
		hyp.putLeaves(leaves);
		Optional<CTLeaf> sink = ct.getSink();
		if (sink.isPresent()) {
			hyp.setSink(hyp.getLocation(sink.get()));
		}
		return hyp;
	}

	private void computeLocations() {
		CTLeaf initial = ct.getLeaf(RaStar.EMPTY_PREFIX);
		RALocation l0 = hyp.addInitialState(initial.isAccepting());
		locations.put(RaStar.EMPTY_PREFIX, l0);
		for (Word<PSymbolInstance> sp : initial.getShortPrefixes()) {
			locations.put(sp, l0);
		}
		hyp.setAccessSequence(l0, RaStar.EMPTY_PREFIX);
		leaves.put(initial, l0);

		for (CTLeaf leaf : ct.getLeaves()) {
			if (leaf != initial) {
				RALocation l = hyp.addState(leaf.isAccepting());
				hyp.setAccessSequence(l, leaf.getRepresentativePrefix());
				for (Word<PSymbolInstance> sp : leaf.getShortPrefixes()) {
					locations.put(sp, l);
				}
				locations.put(leaf.getRepresentativePrefix(), l);
				leaves.put(leaf, l);
			}
		}
	}

	private void computeTransitions() {
		for (CTLeaf leaf : ct.getLeaves()) {
			for (Prefix prefix : leaf.getPrefixes()) {
				computeTransition(leaf, prefix);
			}
		}
	}

	private void computeTransition(CTLeaf dest_l, Prefix prefix) {
		if (prefix.length() < 1) {
			return;
		}

		Prefix dest_rp = dest_l.getRepresentativePrefix();
		Word<PSymbolInstance> src_id = prefix.prefix(prefix.length() - 1);
		CTLeaf src_l = ct.getLeaf(src_id);

		assert src_l != null : "Source prefix not present in classification tree: " + src_id;
		assert src_l.getPrefix(src_id) instanceof ShortPrefix : "Source prefix is not short: " + src_id;

		RALocation src_loc = locations.get(src_id);
		RALocation dest_loc = locations.get(dest_rp);

		assert src_loc != null;
		assert dest_loc != null;

		ParameterizedSymbol action = prefix.lastSymbol().getBaseSymbol();

		Prefix src_prefix = src_l.getPrefix(src_id);
		ShortPrefix src_u = (ShortPrefix)(src_prefix instanceof ShortPrefix ?
				src_prefix :
					src_l.getRepresentativePrefix());

		// guard
		Branching b = src_u.getBranching(action);
		Expression<Boolean> guard = b.getBranches().get(prefix);

		if (guard == null) {
			for (Expression<Boolean> g : b.getBranches().values()) {
				DataValue[] vals = prefix.lastSymbol().getParameterValues();
				ParameterValuation pars = new ParameterValuation();
				for (int i = 0; i < vals.length; i++) {
					Parameter p = new Parameter(vals[i].getDataType(), i+1);
					pars.put(p, vals[i]);
				}
				if (solver.isSatisfiable(g, pars)) {
					guard = g;
					break;
				}
			}
		}

		assert guard != null : "No guard for prefix " + prefix;

        for (Transition tr : hyp.getTransitions(src_loc, action)) {
        	if (tr.getGuard().equals(guard)) {
        		return;
        	}
        }

        ReplacingValuesVisitor rvv = new ReplacingValuesVisitor();
        RegisterAssignment srcAssign = src_u.getAssignment();
        RegisterAssignment rpAssign = src_l.getRepresentativePrefix().getAssignment();
        RegisterAssignment srcAssignRemapped = srcAssign.relabel(registerRemapping(srcAssign, rpAssign, src_u.getRpBijection()));
        guard = rvv.apply(guard, srcAssignRemapped);

        RegisterAssignment destAssign = dest_rp.getAssignment();
        Bijection<DataValue> remapping = prefix.getRpBijection();
        Assignment assign = AutomatonBuilder.computeAssignment(prefix, srcAssignRemapped, destAssign, remapping);

        Transition  t = createTransition(action, guard, src_loc, dest_loc, assign);

        if (t != null) {
            hyp.addTransition(src_loc, action, t);
            hyp.setTransitionSequence(t, prefix);
        }
	}

	private Transition createTransition(ParameterizedSymbol action, Expression<Boolean> guard,
			RALocation src_loc, RALocation dest_loc, Assignment assignment) {
		if (ioMode && !dest_loc.isAccepting()) {
			return null;
		}

		if (!ioMode || !(action instanceof OutputSymbol)) {
			return new Transition(action, guard, src_loc, dest_loc, assignment);
		}

        Expression<Boolean> expr = guard;

        VarMapping<Parameter, SymbolicDataValue> outmap = new VarMapping<>();
        analyzeExpression(expr, outmap);

        Set<Parameter> fresh = new LinkedHashSet<>();
        ParameterGenerator pgen = new ParameterGenerator();
        for (DataType t : action.getPtypes()) {
            Parameter p = pgen.next(t);
            if (!outmap.containsKey(p)) {
                fresh.add(p);
            }
        }

        OutputMapping outMap = new OutputMapping(fresh, outmap);

        return new OutputTransition(ExpressionUtil.TRUE,
                outMap, (OutputSymbol) action, src_loc, dest_loc, assignment);
	}


    private void analyzeExpression(Expression<Boolean> expr,
            VarMapping<Parameter, SymbolicDataValue> outmap) {

        if (expr instanceof PropositionalCompound pc) {
            analyzeExpression(pc.getLeft(), outmap);
            analyzeExpression(pc.getRight(), outmap);
        }
        else if (expr instanceof NumericBooleanExpression nbe) {
            if (nbe.getComparator() == NumericComparator.EQ) {
                // FIXME: this is unchecked!
                SymbolicDataValue left = (SymbolicDataValue) nbe.getLeft();
                SymbolicDataValue right = (SymbolicDataValue) nbe.getRight();

                Parameter p = null;
                SymbolicDataValue sv = null;

                if (left instanceof Parameter) {
                    if (right instanceof Parameter) {
                        throw new UnsupportedOperationException("not implemented yet.");
                    }
                    else {
                        p = (Parameter) left;
                        sv = right;
                    }
                }
                else {
                    p = (Parameter) right;
                    sv = left;
                }

                outmap.put(p, sv);
            }
        }
    }

    private VarMapping<Register, Register> registerRemapping(RegisterAssignment raa, RegisterAssignment rab, Bijection<DataValue> bijection) {
    	VarMapping<Register, Register> ret = new VarMapping<>();

    	for (Map.Entry<DataValue, DataValue> be : bijection.entrySet()) {
    		Register replace = raa.get(be.getKey());
    		Register by = rab.get(be.getValue());
    		ret.put(replace, by);
    	}

    	return ret;
    }
}
