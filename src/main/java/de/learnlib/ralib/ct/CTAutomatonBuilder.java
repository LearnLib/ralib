package de.learnlib.ralib.ct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.logging.Category;
import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.RegisterAssignment;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.learning.AutomatonBuilder;
import de.learnlib.ralib.learning.Hypothesis;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.smt.ReplacingValuesVisitor;
import de.learnlib.ralib.smt.SMTUtil;
import de.learnlib.ralib.words.DataWords;
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
//	private final Map<Word<PSymbolInstance>, Bijection> rpRenamings;
	
//	private final Set<Word<PSymbolInstance>> visitedTransitions;
	
	private final CTHypothesis hyp;
	
//	private final Constants consts;
	
	private boolean ioMode;
	
	public CTAutomatonBuilder(ClassificationTree ct, Constants consts, boolean ioMode) {
		this.ct = ct;
//		this.consts = consts;
		this.ioMode = ioMode;
		
		locations = new LinkedHashMap<>();
		leaves = new LinkedHashMap<>();
//		rpRenamings = new LinkedHashMap<>();
//		visitedTransitions = new LinkedHashSet<>();
		hyp = new CTHypothesis(consts, ct.getLeaves().size());
	}
	
	public Hypothesis buildHypothesis() {
		computeLocations();
		computeTransitions();
		hyp.putLeaves(leaves);
		return hyp;
	}
	
	private void computeLocations() {
		CTLeaf initial = ct.getLeaf(RaStar.EMPTY_PREFIX);
		RALocation l0 = hyp.addInitialState(initial.isAccepting());
		locations.put(RaStar.EMPTY_PREFIX, l0);
		hyp.setAccessSequence(l0, RaStar.EMPTY_PREFIX);
//		rpRenamings.put(RaStar.EMPTY_PREFIX, new Bijection());
		leaves.put(initial, l0);
		
		for (CTLeaf leaf : ct.getLeaves()) {
			if (leaf != initial) {
				RALocation l = hyp.addState(leaf.isAccepting());
				hyp.setAccessSequence(l, leaf.getRepresentativePrefix());
//				locations.put(leaf.getRepresentativePrefix(), l);
				for (Word<PSymbolInstance> sp : leaf.getShortPrefixes()) {
					locations.put(sp, l);
				}
				leaves.put(leaf, l);
			}
		}
	}
	
	private void computeTransitions() {
		for (CTLeaf leaf : ct.getLeaves()) {
//			computeTransition(leaf, leaf.getRepresentativePrefix());
			for (Prefix prefix : leaf.getPrefixes()) {
				computeTransition(leaf, prefix);
			}
		}
	}
	
	private void computeTransition(CTLeaf dest_l, Prefix prefix) {
//		if (visitedTransitions.contains(prefix)) {
//			return;
//		}
		
		if (prefix.length() < 1) {
			return;
		}
		
//		Word<PSymbolInstance> dest_id = prefix;
		Prefix dest_rp = dest_l.getRepresentativePrefix();
		Word<PSymbolInstance> src_id = prefix.prefix(prefix.length() - 1);
		CTLeaf src_l = ct.getLeaf(src_id);
		
		assert src_l != null : "Source prefix not present in classification tree: " + src_id;
		assert src_l.getPrefix(src_id) instanceof ShortPrefix : "Source prefix is not short: " + src_id;
		assert dest_rp instanceof ShortPrefix : "Representative prefix is not short: " + dest_rp;
		
		RALocation src_loc = locations.get(src_id);
		RALocation dest_loc = locations.get(dest_rp);
		
		ParameterizedSymbol action = prefix.lastSymbol().getBaseSymbol();

		assert src_l.getRepresentativePrefix() instanceof ShortPrefix : "Representative prefix is not a short prefix: " + src_l;
		
		Prefix src_prefix = src_l.getPrefix(src_id);
		ShortPrefix src_u = (ShortPrefix)(src_prefix instanceof ShortPrefix ?
				src_prefix :
					src_l.getRepresentativePrefix());
		
		// guard
		Branching b = src_u.getBranching(action);
		Expression<Boolean> guard = b.getBranches().get(prefix);
		
		assert guard != null : "No guard for prefix " + prefix;

        ReplacingValuesVisitor rvv = new ReplacingValuesVisitor();
        guard = rvv.apply(guard, src_u.getAssignment());
        
        RegisterAssignment srcAssign = src_u.getAssignment();
        RegisterAssignment destAssign = dest_rp.getAssignment();
        Bijection<DataValue> remapping = prefix.getRpBijection();
        Assignment assign = AutomatonBuilder.computeAssignment(prefix, srcAssign, destAssign, remapping);

        Transition  t = createTransition(action, guard, src_loc, dest_loc, assign);
        if (t != null) {
            hyp.addTransition(src_loc, action, t);
            hyp.setTransitionSequence(t, prefix);
        }
//		
//		Word<PSymbolInstance> src_rp = src_l.getRepresentativePrefix();
//		Bijection src_renaming = rpRenamings.get(src_rp);
//		if (src_renaming == null) {
//			computeTransition(src_l, src_l.getRepresentativePrefix());
//			src_renaming = rpRenamings.get(src_rp);
//		}
//		
//		int max = DataWords.paramValLength(src_id);
//		List<Register> regs = new ArrayList<>(prefix.getRegisters());
//		regs.sort((r1, r2) -> Integer.compare(r1.getId(), r2.getId()));
//		RegisterGenerator rgen = new RegisterGenerator();
//
//		Map<Register, SymbolicDataValue> mapping = new LinkedHashMap<>();
//		
//		Bijection dest_renaming;
//		if (prefix == dest_l.getRepresentativePrefix()) {
//			// case 1 : prefix is the rp
//			dest_renaming = new Bijection();
//			for (Register r : regs) {
//				Register reg = rgen.next(r.getDataType());
//				dest_renaming.put(r, reg);
//				if (r.getId() > max) {
//					Parameter p = new Parameter(r.getDataType(), r.getId() - max);
//					mapping.put(reg, p);
//				} else {
//					Register p = src_renaming.get(r);
//					assert p != null : "Register not memorable in source location: " + r;
//					mapping.put(reg, p);
//				}
//			}
//			rpRenamings.put(prefix, dest_renaming);
//		} else {
//			// case 2 : prefix is not the rp
////			Word<PSymbolInstance> dest_rp = dest_l.getRepresentativePrefix().getPrefix();
//			Bijection rp_renaming = rpRenamings.get(dest_rp);
//			assert rp_renaming != null : "No rp mapping: " + dest_rp;
//			dest_renaming = prefix.getRpBijection().compose(rp_renaming);
//			for (Register r : regs) {
//				Register reg = dest_renaming.get(r);
//				assert reg != null : "Register not compatible with rp: " + r;
//				if (r.getId() > max) {
//					Parameter p = new Parameter(r.getDataType(), r.getId() - max);
//					mapping.put(reg, p);
//				} else {
//					Register src_r = src_renaming.get(r);
//					assert src_r != null : "Register not memorable in source location: " + r;
//					mapping.put(reg, src_r);
//				}
//			}
//		}
//		
//		VarMapping<Register, SymbolicDataValue> vars = new VarMapping<>();
//		vars.putAll(mapping);
//		Assignment assignment = new Assignment(vars);
//		
//		VarMapping<Register, Register> guardRenaming = new VarMapping<>();
//		guardRenaming.putAll(src_renaming);
//		Expression<Boolean> guardRenamed = SMTUtil.renameVars(guard, guardRenaming);
//		
//		Transition transition = createTransition(action, guardRenamed, src_loc, dest_loc, assignment);
//		if (transition != null) {
//			hyp.addTransition(src_loc, action, transition);
//			hyp.setTransitionSequence(transition, dest_id);
//		}
//		
//		visitedTransitions.add(dest_id);
	}
	
	private Transition createTransition(ParameterizedSymbol action, Expression<Boolean> guard,
			RALocation src_loc, RALocation dest_loc, Assignment assignment) {
		if (ioMode && !dest_loc.isAccepting()) {
			return null;
		}
		
		if (!ioMode || !(action instanceof OutputSymbol)) {
			return new Transition(action, guard, src_loc, dest_loc, assignment);
		}
		
        //IfGuard _guard = (IfGuard) guard;
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
                //System.out.println(expr);
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
        else {
            // true and false ...
            //throw new IllegalStateException("Unsupported: " + expr.getClass());
        }
    }
}
