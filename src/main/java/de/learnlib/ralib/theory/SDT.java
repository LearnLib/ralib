/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.theory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.RemappingIterator;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.smt.SMTUtil;
import de.learnlib.ralib.theory.SDTGuard;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.solvers.nativez3.NativeZ3Solver;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

/**
 * Implementation of Symbolic Decision Trees.
 *
 * @author Sofia Cassel
 */
public class SDT {

    private final Map<SDTGuard, SDT> children;

    public SDT(Map<SDTGuard, SDT> children) {
        this.children = children;
    }

    /**
     * Returns the registers of this SDT.
     *
     * @return
     */
    public Set<Register> getRegisters() {
        Set<Register> registers = new LinkedHashSet<>();
        this.getVariables().stream().filter((x) -> (x.isRegister())).forEach((x) -> {
            registers.add((Register) x);
        });
        return registers;
    }

    public int getHeight() {
        if (this instanceof SDTLeaf || children.size() == 0) {
            return 0;
        } else {
            return children.values().stream().map(c -> c.getHeight()).max( (i1, i2) -> i1.compareTo(i2)).get() + 1;
        }
    }

    public Set<SymbolicDataValue> getVariables() {
        Set<SymbolicDataValue> variables = new LinkedHashSet<>();
        for (Entry<SDTGuard, SDT> e : children.entrySet()) {
            SDTGuard g = e.getKey();
            if (g instanceof SDTGuard.EqualityGuard eg) {
                SymbolicDataValue r = eg.register();
                variables.add(r);
            } else if (g instanceof SDTGuard.DisequalityGuard dg) {
                SymbolicDataValue r = dg.register();
                variables.add(r);
            } else if (g instanceof SDTGuard.SDTAndGuard ag) {
                for (SDTGuard ifG : ag.conjuncts()) {
                    variables.addAll(ifG.getRegisters());
                }
            } else if (g instanceof SDTGuard.SDTOrGuard og) {
                for (SDTGuard ifG : og.disjuncts()) {
                    variables.addAll(ifG.getRegisters());
                }
            } else if (g instanceof SDTGuard.IntervalGuard iGuard) {
                if (!iGuard.isBiggerGuard()) {
                    variables.add(iGuard.rightLimit());
                }
                if (!iGuard.isSmallerGuard()) {
                    variables.add(iGuard.leftLimit());
                }
            } else if (!(g instanceof SDTGuard.SDTTrueGuard)) {
                throw new RuntimeException("unexpected case");
            }
            SDT child = e.getValue();
            // FIXME: this is bad style: it will break if the tree is only a leaf! I added code to SDTLeaf to prevent this
            if (child.getChildren() != null) {
                variables.addAll(child.getVariables());
            }
        }
        return variables;
    }

    public Set<SymbolicDataValue.SuffixValue> getSuffixValues() {
    	Set<SymbolicDataValue.SuffixValue> values = new LinkedHashSet<>();
    	if (this instanceof SDTLeaf)
    		return values;
    	for (Entry<SDTGuard, SDT> e : children.entrySet()) {
    		values.add(e.getKey().getParameter());
    		values.addAll(e.getValue().getSuffixValues());
    	}
    	return values;
    }

    @Override
    public boolean isAccepting() {

            for (Map.Entry<SDTGuard, SDT> e : children.entrySet()) {
                if (!e.getValue().isAccepting()) {
                    return false;
                }

        }

        return true;
        //return false;
    }

    public boolean isAccepting(Mapping<SymbolicDataValue, DataValue> vals, Constants consts) {
    	Mapping<SymbolicDataValue, DataValue> mapping = new Mapping<SymbolicDataValue, DataValue>();
    	mapping.putAll(vals);
    	mapping.putAll(consts);
        Expression<Boolean> expr = getAcceptingPaths(consts);
    	return expr.evaluateSMT(SMTUtil.compose(mapping));
    }

    public Map<SDTGuard, SDT> getChildren() {
        return this.children;
    }

    public boolean isEquivalent(
            SDT other, VarMapping renaming) {
        if (other instanceof SDTLeaf) {
            return false;
        }
        SDT otherSDT =  other;
        SDT otherRelabeled =  otherSDT.relabel(renaming);
        boolean regEq = this.regCanUse(otherRelabeled) && otherRelabeled.regCanUse(this);
        return regEq && this.canUse(otherRelabeled)
                && otherRelabeled.canUse(this);
    }

    public boolean isEquivalentUnder(
            SDT deqSDT, List<SDTGuard.EqualityGuard> ds) {
        if (deqSDT instanceof SDTLeaf) {
            if (this instanceof SDTLeaf) {
                return (this.isAccepting() == deqSDT.isAccepting());
            }
            return false;
        }
        VarMapping eqRenaming = new VarMapping<>();
        for (SDTGuard.EqualityGuard d : ds) {
            eqRenaming.put(d.parameter(), d.register());
        }
//        System.out.println(eqRenaming);
//        System.out.println(this + " vs " + deqSDT);
        boolean x = this.canUse( deqSDT.relabel(eqRenaming));
        return x;
    }

    public SDT relabelUnderEq(List<SDTGuard.EqualityGuard> ds) {
        VarMapping eqRenaming = new VarMapping<>();
        for (SDTGuard.EqualityGuard d : ds) {
            eqRenaming.put(d.parameter(), d.register());
        }
        return  this.relabel(eqRenaming);
    }

    public SDT relabel(VarMapping relabelling) {
        //System.out.println("relabeling " + relabelling);
        SDT thisSdt = this;
        if (relabelling.isEmpty()) {
            return this;
        }

        Map<SDTGuard, SDT> reChildren = new LinkedHashMap<>();
        // for each of the kids
        for (Entry<SDTGuard, SDT> e : thisSdt.children.entrySet()) {
                reChildren.put(SDTGuard.relabel(e.getKey(), relabelling),
                     e.getValue().relabel(relabelling));
            }
        SDT relabelled = new SDT(reChildren);
        assert !relabelled.isEmpty();
        return relabelled;
    }

    /* ***
     *
     * Logging helpers
     *
     */

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String regs = Arrays.toString(getRegisters().toArray());
        sb.append(regs).append("-+\n");
        toString(sb, spaces(regs.length()));
        return sb.toString();
    }

    void toString(StringBuilder sb, String indentation) {
        SDT idioticSdt = this;
        sb.append(indentation).append("[]");
        final int childCount = idioticSdt.children.size();
        int count = 1;
        for (Entry<SDTGuard, SDT> e : idioticSdt.children.entrySet()) {
            SDTGuard g = e.getKey();
            String gString = g.toString();
            String nextIndent;
            if (count == childCount) {
                nextIndent = indentation + "      ";
            } else {
                nextIndent = indentation + " |    ";
            }

            if (count > 1) {
                sb.append(indentation).append(" +");
            }
            sb.append("-").append(gString).append("\n");
            e.getValue().toString(sb, nextIndent);

            count++;
        }
    }

    private String spaces(int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    // Returns true if all elements of a boolean array are true.
    private boolean isArrayTrue(Boolean[] maybeArr) {
        boolean maybe = true;
        for (int c = 0; c < (maybeArr.length); c++) {
            //log.trace(maybeArr[c]);
            if (!maybeArr[c]) {
                maybe = false;
                break;
            }
        }
        return maybe;
    }

    // Returns true if this SDT can use the registers of another SDT.  Registers
    // are matched by name (no remapping)
    private boolean regCanUse(SDT other) {

        Set<Register> otherRegisters = other.getRegisters();
        Set<Register> thisRegisters = this.getRegisters();

        if (otherRegisters.isEmpty() && thisRegisters.isEmpty()) {
            return true;
        } else {
            Boolean[] regEqArr = new Boolean[thisRegisters.size()];
            Integer i = 0;
            for (SymbolicDataValue thisReg : thisRegisters) {
                // if the trees have the same type and size
                regEqArr[i] = false;
                for (SymbolicDataValue otherReg : otherRegisters) {
                    if (thisReg.equals(otherReg)) {
                        regEqArr[i] = true;
                        break;
                    }
                }
                i++;
            }
            return isArrayTrue(regEqArr);
        }
    }

    private boolean hasPair(
            SDTGuard thisGuard, SDT thisSdt, Map<SDTGuard, SDT> otherBranches) {
        for (Map.Entry<SDTGuard, SDT> otherB : otherBranches.entrySet()) {
            // FIXME: this should be done semantically!
            if (thisGuard.equals(otherB.getKey())) {
                if (thisSdt.canUse(otherB.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canPairBranches(
            Map<SDTGuard, SDT> thisBranches, Map<SDTGuard, SDT> otherBranches) {
        if (thisBranches.size() != otherBranches.size()) {
            return false;
        }
        Boolean[] pairedArray = new Boolean[thisBranches.size()];
        Integer i = 0;
        for (Map.Entry<SDTGuard, SDT> thisB : thisBranches.entrySet()) {
            pairedArray[i] = hasPair(
                    thisB.getKey(), thisB.getValue(), otherBranches);
            i++;
        }
        return isArrayTrue(pairedArray);

    }

    public boolean canUse(SDT other) {
        SDT thisSdt = this;
        if (other instanceof SDTLeaf) { // trees with incompatible sizes can't use each other
            return false;
        } else {
            boolean accEq = (thisSdt.isAccepting() == other.isAccepting());
            boolean chiEq = canPairBranches(thisSdt.getChildren(),
                    other.getChildren());
            return accEq && chiEq;
        }
    }

    public boolean isEmpty() {
        return this.getChildren().isEmpty();
    }

    public Expression<Boolean> getAcceptingPaths(Constants consts) {

        List<List<SDTGuard>> paths = getPaths(new ArrayList<SDTGuard>());
        if (paths.isEmpty()) {
            return ExpressionUtil.FALSE;
        }
        Expression<Boolean> dis = null;
        for (List<SDTGuard> list : paths) {
            List<Expression<Boolean>> expr = new ArrayList<>();
            for (SDTGuard g : list) {
                expr.add(SDTGuard.toExpr(g));
            }
            Expression<Boolean> con = ExpressionUtil.and(
                    expr.toArray(new Expression[] {}));

            dis = (dis == null) ? con : ExpressionUtil.or(dis, con);
        }

        return dis;
    }

    public Map<Expression<Boolean>, Boolean> getGuardExpressions(Constants consts) {
    	Map<Expression<Boolean>, Boolean> expressions = new LinkedHashMap<>();
    	Map<List<SDTGuard>, Boolean> paths = getAllPaths(new ArrayList<SDTGuard>());
    	if (paths.isEmpty()) {
    		expressions.put(ExpressionUtil.FALSE, false);
    		return expressions;
    	}
    	for (Map.Entry<List<SDTGuard>, Boolean> e : paths.entrySet()) {
    		List<SDTGuard> list = e.getKey();
    		List<Expression<Boolean>> expr = new ArrayList<>();
    		for (SDTGuard g : list) {
    			expr.add(SDTGuard.toExpr(g));
    		}
            Expression<Boolean> con = ExpressionUtil.and(
    				expr.toArray(new Expression[] {}));
    		expressions.put(con, e.getValue());
    	}

    	return expressions;
    }

    List<List<SDTGuard>> getPaths(List<SDTGuard> path) {
        List<List<SDTGuard>> ret = new ArrayList<>();
        for (Entry<SDTGuard, SDT> e : this.children.entrySet()) {
            List<SDTGuard> nextPath = new ArrayList<>(path);
            nextPath.add(e.getKey());
            List<List<SDTGuard>> nextRet = e.getValue().getPaths(nextPath);
            ret.addAll(nextRet);
        }

        return ret;
    }

    public List<List<SDTGuard>> getPaths(boolean accepting) {
        List<List<SDTGuard>> collectedPaths = new ArrayList<List<SDTGuard>>();
        getPaths(accepting, new ArrayList<>(), this, collectedPaths);
        return collectedPaths;
    }

    private void getPaths(boolean accepting, List<SDTGuard> path, SDT sdt, List<List<SDTGuard>> collectedPaths) {
        if (sdt instanceof SDTLeaf) {
            if (sdt.isAccepting() == accepting) {
                collectedPaths.add(path);
            }
        } else {
            for (Entry<SDTGuard, SDT> e : sdt.children.entrySet()) {
                List<SDTGuard> nextPath = new ArrayList<>(path);
                nextPath.add(e.getKey());
                SDT nextSdt = e.getValue();
                getPaths(accepting, nextPath, nextSdt, collectedPaths);
            }
        }
    }

    public Map<List<SDTGuard>, Boolean> getAllPaths(List<SDTGuard> path) {
        Map<List<SDTGuard>, Boolean> ret = new LinkedHashMap<>();
        for (Entry<SDTGuard, SDT> e : this.children.entrySet()) {
            List<SDTGuard> nextPath = new ArrayList<>(path);
            nextPath.add(e.getKey());
            Map<List<SDTGuard>, Boolean> nextRet = e.getValue().getAllPaths(nextPath);
            ret.putAll(nextRet);
        }

        return ret;
    }

    private static SDT findFinest(int i, List<SDT> sdts, SDT curr) {
        i++;
        if (sdts.size() == i) {
            return curr;
        } else {
            SDT nxt = sdts.get(i);
            if (curr.getChildren().size() >= nxt.getChildren().size()) {
                return findFinest(i, sdts, curr);
            } else {
                return findFinest(i, sdts, nxt);
            }
        }
    }

	public SDT copy() {
		Map<SDTGuard, SDT> cc = new LinkedHashMap<>();
		if (children != null) {
			for (Map.Entry<SDTGuard, SDT> e : children.entrySet()) {
				cc.put(SDTGuard.copy(e.getKey()), e.getValue().copy());
			}
		}
		return new SDT(cc);
	}

	/**
	 * Check if a SDT is semantically equivalent to another, given some conditions on the other SDT
	 *
	 * @param other - the SDT to compare to
	 * @param condition - the conditions to apply to other
	 * @param solver - a constraint solver
	 */
	public boolean isEquivalentUnderCondition(SDT other, Expression<Boolean> condition, NativeZ3Solver solver) {
		Map<GuardExpression, Boolean> expressions = this.getGuardExpressions(new Constants());
		Map<GuardExpression, Boolean> otherExpressions = other.getGuardExpressions(new Constants());
		for (Map.Entry<GuardExpression, Boolean> entry : expressions.entrySet()) {
			Expression<Boolean> x = toExpression(entry.getKey());
			Boolean outcome = entry.getValue();
			for (Map.Entry<GuardExpression, Boolean> otherEntry : otherExpressions.entrySet()) {
				if (outcome != otherEntry.getValue()) {
					Expression<Boolean> otherX = toExpression(otherEntry.getKey());
					Expression<Boolean> renamed = ExpressionUtil.and(otherX, condition);
					Expression<Boolean> con = ExpressionUtil.and(x, renamed);
					if (solver.isSatisfiable(con) == Result.SAT) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Returns a bijection b such that b and bi agree on the registers in the intersection
	 * of their domains and such that sdt1 and sdt2 are equivalent under b.
	 * Returns null if no such bijection can be found.
	 *
	 * @param sdt1
	 * @param sdt2
	 * @param bi
	 * @param solver
	 * @return
	 */
	public static Bijection equivalentUnderBijection(SDT sdt1, SDT sdt2, Bijection bi, ConstraintSolver solver) {
		sdt1 = sdt1.relabel(bi.toVarMapping());
		Set<Register> regs1 = sdt1.getRegisters();
		Set<Register> regs2 = sdt2.getRegisters();

		if (regs1.size() != regs2.size()) {
			return null;
		}

		if (regs1.containsAll(regs2)) {
			return sdt1.isEquivalent(sdt2, solver) ? bi : null;
		}

		Set<Register> replace = new LinkedHashSet<>(regs1);
		replace.removeAll(bi.values());
		Set<Register> by = new LinkedHashSet<>(regs2);
		by.removeAll(bi.values());

		RemappingIterator it = new RemappingIterator(replace, by);
		while (it.hasNext()) {
			Bijection vars = it.next();
			if (sdt2.isEquivalent(sdt1.relabel(vars.toVarMapping()), solver)) {
				Bijection b = new Bijection();
				b.putAll(bi);
				b.putAll(vars);
				return b;
			}
		}

		return null;
	}

	/**
	 * Returns a bijection b such that sdt1 and sdt2 are semantically equivalent under b
	 *
	 * @param sdt1
	 * @param sdt2
	 * @param solver
	 * @return
	 */
	public static Bijection equivalentUnderBijection(SDT sdt1, SDT sdt2, ConstraintSolver solver) {
		return equivalentUnderBijection(sdt1, sdt2, new Bijection(), solver);
	}

	/**
	 * Returns true if sdt1 and sdt2 are semantically equivalent
	 *
	 * @param sdt1
	 * @param sdt2
	 * @param solver
	 * @return
	 */
	public static boolean equivalentUnderId(SDT sdt1, SDT sdt2, ConstraintSolver solver) {
		return sdt1.isEquivalent(sdt2, solver);
	}

	/**
	 * Returns the set of registers of used by the sdts
	 *
	 * @param sdts
	 * @return
	 */
	public static Set<Register> registersOf(SDT ... sdts) {
		Set<Register> regs = new LinkedHashSet<>();
		for (SDT sdt : sdts) {
			regs.addAll(sdt.getRegisters());
		}
		return regs;
	}
}
