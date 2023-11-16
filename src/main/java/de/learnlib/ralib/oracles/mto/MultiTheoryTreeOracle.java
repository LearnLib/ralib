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
package de.learnlib.ralib.oracles.mto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;

import de.learnlib.logging.LearnLogger;
import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.automata.guards.FalseGuardExpression;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.ParValuation;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.VarValuation;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.SuffixValueGenerator;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryBranching.Node;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;

/**
 *
 * @author falk
 */
public class MultiTheoryTreeOracle implements TreeOracle, SDTConstructor {

    private final DataWordOracle oracle;

    private final Constants constants;

    private final Map<DataType, Theory> teachers;

    private final ConstraintSolver solver;

    private static LearnLogger log = LearnLogger.getLogger(MultiTheoryTreeOracle.class);

    public MultiTheoryTreeOracle(DataWordOracle oracle, Map<DataType, Theory> teachers, Constants constants,
            ConstraintSolver solver) {
        this.oracle = oracle;
        this.teachers = teachers;
        this.constants = constants;
        this.solver = solver;
    }

    @Override
    public TreeQueryResult treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
        PIV pir = new PIV();
        SDT sdt = treeQuery(prefix, suffix, new WordValuation(), pir, constants, new SuffixValuation());

//        System.out.println(prefix + " . " + suffix);
//        System.out.println(sdt);

        // move registers to 1 ... n
        VarMapping rename = new VarMapping();
        RegisterGenerator gen = new RegisterGenerator();
        Set<Register> regs = sdt.getRegisters();
        PIV piv = new PIV();

        for (Entry<Parameter, Register> e : pir.entrySet()) {
            if (regs.contains(e.getValue())) {
                Register r = e.getValue();
                rename.put(r, gen.next(r.getType()));
                piv.put(e.getKey(), (Register) rename.get(r));
            }
        }

        TreeQueryResult tqr = new TreeQueryResult(piv, sdt.relabel(rename));
        log.debug("PIV: " + piv);

        return tqr;
    }

    @Override
    public SDT treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, WordValuation values, PIV pir,
            Constants constants, SuffixValuation suffixValues) {

//        System.out.println("prefix = " + prefix + "   suffix = " + suffix + "    values = " + values);

        if (values.size() == DataWords.paramLength(suffix.getActions())) {
            Word<PSymbolInstance> concSuffix = DataWords.instantiate(suffix.getActions(), values);

//            Word<PSymbolInstance> trace = prefix.concat(concSuffix);
            DefaultQuery<PSymbolInstance, Boolean> query = new DefaultQuery<>(prefix, concSuffix);
            oracle.processQueries(Collections.singletonList(query));
            boolean qOut = query.getOutput();

//            System.out.println("Trace = " + trace.toString() + " >>> "
//                    + (qOut ? "ACCEPT (+)" : "REJECT (-)"));
            return qOut ? SDTLeaf.ACCEPTING : SDTLeaf.REJECTING;

            // return accept / reject as a leaf
        }

        // OTHERWISE get the first noninstantiated data value in the suffix and its type
        SymbolicDataValue sd = suffix.getDataValue(values.size() + 1);

        Theory teach = teachers.get(sd.getType());

        // make a new tree query for prefix, suffix, prefix valuation, ...
        // to the correct teacher (given by type of first DV in suffix)
        return teach.treeQuery(prefix, suffix, values, pir, constants, suffixValues, this);
    }

    /**
     * This method computes the initial branching for an SDT. It re-uses existing
     * valuations where possible.
     *
     */
    @Override
    public Branching getInitialBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv,
            SymbolicDecisionTree... sdts) {

        log.info("computing initial branching for {0} after {1}", new Object[] { ps, prefix });

        // TODO: check if this casting can be avoided by proper use of generics
        // TODO: the problem seems to be
        // System.out.println("using " + sdts.length + " SDTs");
        SDT[] casted = new SDT[sdts.length];
        for (int i = 0; i < casted.length; i++) {
            if (sdts[i] instanceof SDTLeaf) {
                casted[i] = (SDTLeaf) sdts[i];
            } else {
                casted[i] = (SDT) sdts[i];
            }
        }

        MultiTheoryBranching mtb = this.getInitialBranching(prefix, ps, piv, new ParValuation(), casted);

        log.trace(mtb.toString());

        return mtb;
    }

    // get the initial branching for the symbol ps after prefix given a certain tree
    private MultiTheoryBranching getInitialBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv,
            ParValuation pval, SDT... sdts) {
        Node n;

        if (sdts.length == 0) {
            n = createFreshNode(1, prefix, ps, piv, pval);
            return new MultiTheoryBranching(prefix, ps, n, piv, pval, constants, sdts);
        } else {
            n = createNode(1, prefix, ps, piv, pval, sdts);
            MultiTheoryBranching fluff = new MultiTheoryBranching(prefix, ps, n, piv, pval, constants, sdts);
            return fluff;
        }

    }

    private Node createFreshNode(int i, Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv,
            ParValuation pval) {

        if (i == ps.getArity() + 1) {
            return new Node(new Parameter(null, i));
        } else {
            Map<DataValue, Node> nextMap = new LinkedHashMap<>();
            Map<DataValue, SDTGuard> guardMap = new LinkedHashMap<>();

            DataType type = ps.getPtypes()[i - 1];
            log.trace("current type: " + type.getName());
            Parameter p = new Parameter(type, i);
            SDTGuard guard = new SDTTrueGuard(new SuffixValue(type, i));
            Theory teach = teachers.get(type);

            DataValue dvi = teach.instantiate(prefix, ps, piv, pval, constants, guard, p, new LinkedHashSet<>());
            ParValuation otherPval = new ParValuation();
            otherPval.putAll(pval);
            otherPval.put(p, dvi);

            nextMap.put(dvi, createFreshNode(i + 1, prefix, ps, piv, otherPval));

            guardMap.put(dvi, guard);
            return new Node(p, nextMap, guardMap);
        }
    }

    private Node createNode(int i, Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv, ParValuation pval,
            SDT... sdts) {
        Node n = createNode(i, prefix, ps, piv, pval, new LinkedHashMap<>(), sdts);
        return n;
    }

    private Node createNode(int i, Word<PSymbolInstance> prefix, ParameterizedSymbol ps, PIV piv, ParValuation pval,
            Map<Parameter, Set<DataValue>> oldDvMap, SDT... sdts) {

        if (i == ps.getArity() + 1) {
            return new Node(new Parameter(null, i));
        } else {
            // obtain the data type, teacher, parameter
            DataType type = ps.getPtypes()[i - 1];
            Theory teach = teachers.get(type);
            Parameter p = new Parameter(type, i);

            // valuation
            Mapping<SymbolicDataValue, DataValue<?>> valuation = buildValuation(pval, prefix, piv, constants);

            // the map may contain no old values for p, in which case we use an empty set
            // (to avoid potential NPE when instantiating guards)
            Set<DataValue> oldDvs = oldDvMap.getOrDefault(p, Collections.emptySet());

            // initialize maps for next nodes
            Map<DataValue, Node> nextMap = new LinkedHashMap<>();
            Map<DataValue, SDTGuard> guardMap = new LinkedHashMap<>();

            MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(constants, solver);
            // get merged guards mapped to the set of old guards from which they are
            // generated
            Map<SDTGuard, Set<SDTGuard>> mergedGuards = getNewRefinedInitialGuards(sdts, mlo, valuation);
            // get old guards mapped to the child SDT they connect to
            Map<SDTGuard, List<SDT>> nextSDTs = getChildren(sdts);

            for (Map.Entry<SDTGuard, Set<SDTGuard>> mergedGuardEntry : mergedGuards.entrySet()) {
                SDTGuard guard = mergedGuardEntry.getKey();
                Set<SDTGuard> oldGuards = mergedGuardEntry.getValue();

                // first solve using a constraint solver
                DataValue dvi = teach.instantiate(prefix, ps, piv, pval, constants, guard, p, oldDvs);
                // if merging of guards is done properly, there should be no case where the
                // guard cannot be instantiated.
                assert (dvi != null);

                SDT[] nextLevelSDTs = oldGuards.stream().map(g -> nextSDTs.get(g)).flatMap(g -> g.stream()) // stream
                                                                                                            // with of
                                                                                                            // sdt lists
                                                                                                            // for old
                                                                                                            // guards
                        .distinct().toArray(SDT[]::new); // merge and pick distinct elements

                ParValuation otherPval = new ParValuation();
                otherPval.putAll(pval);
                otherPval.put(p, dvi);

                nextMap.put(dvi, createNode(i + 1, prefix, ps, piv, otherPval, oldDvMap, nextLevelSDTs));
                if (guardMap.containsKey(dvi)) {
                    throw new IllegalStateException(
                            "Guard instantiated using a dvi that was already used to instantiate a prior guard.");
                }
                guardMap.put(dvi, guard);
            }

            log.trace("guardMap: " + guardMap.toString());
            log.trace("nextMap: " + nextMap.toString());
            assert !nextMap.isEmpty();
            assert !guardMap.isEmpty();
            return new Node(p, nextMap, guardMap);
        }
    }

    // conjoins the initial guards of the SDTs producing a map from new refined
    // (initial) guards to the set of guards they originated from
    private Map<SDTGuard, Set<SDTGuard>> getNewRefinedInitialGuards(SDT[] sdts, MultiTheorySDTLogicOracle mlo,
            Mapping<SymbolicDataValue, DataValue<?>> valuation) {
        Map<SDTGuard, Set<SDTGuard>> mergedGroup = new LinkedHashMap<>();
        for (SDT sdt : sdts) {
            Set<SDTGuard> nextGuardGroup = sdt.getChildren().keySet();
            mergedGroup = combineGroups(mergedGroup, nextGuardGroup, mlo, valuation);
        }
        return mergedGroup;
    }

    // merges the next set of initial guards to the map of new initial guards,
    // producing an new map of refined guards
    // merging involves:
    // 1. conjoining each initial guard with each refined guard in the map where
    // this is possible in the sense that the guards are not mutually exclusive
    // 2. updating the map
    private Map<SDTGuard, Set<SDTGuard>> combineGroups(Map<SDTGuard, Set<SDTGuard>> mergedHead, Set<SDTGuard> nextGroup,
            MultiTheorySDTLogicOracle mlo, Mapping<SymbolicDataValue, DataValue<?>> valuation) {
        Map<SDTGuard, Set<SDTGuard>> mergedGroup = new LinkedHashMap<>();
        if (mergedHead.isEmpty()) {
            nextGroup.forEach(next -> {
                mergedGroup.put(next, Sets.newHashSet(next));
            });
            return mergedGroup;
        }

        // pairs of guards that have been conjoined
        Set<Pair<SDTGuard, SDTGuard>> headNextPairs = new HashSet<>();

        for (Map.Entry<SDTGuard, Set<SDTGuard>> entry : mergedHead.entrySet()) {
            SDTGuard head = entry.getKey();
            Set<SDTGuard> oldGuards = entry.getValue();

            // we filter out pairs already covered
            SDTGuard[] notCoveredPairs = nextGroup.stream()
                    .filter(next -> !headNextPairs.contains(Pair.of(next, head))).toArray(SDTGuard[]::new);

            // we then select only the next guards which can be conjoined with the head
            // guard, i.e.
            SDTGuard[] compatibleNextGuards = Stream.of(notCoveredPairs)
                    .filter(next -> canBeMerged(head, next, mlo, valuation)).toArray(SDTGuard[]::new);

            for (SDTGuard next : compatibleNextGuards) {
                SDTGuard refinedGuard = null;
                if (head.equals(next))
                    refinedGuard = next;
                else if (refines(next, head, mlo, valuation))
                    refinedGuard = next;
                else if (refines(head, next, mlo, valuation))
                    refinedGuard = head;
                else
                    refinedGuard = conjoin(head, next);

                // we compute the old guard set, that is the guards over which conjunction was
                // applied to form the refined guard
                LinkedHashSet<SDTGuard> newOldGuards = Sets.newLinkedHashSet(oldGuards);
                newOldGuards.add(next);
                mergedGroup.put(refinedGuard, newOldGuards);
                headNextPairs.add(Pair.of(head, next));
            }
        }
        return mergedGroup;
    }

    public SDTGuard conjoin(SDTGuard guard1, SDTGuard guard2) {
        assert guard1.getParameter().equals(guard2.getParameter());
        if (guard1.equals(guard2))
            return guard1;

        if (guard1 instanceof SDTTrueGuard) {
            return guard2;
        }

        if (guard2 instanceof SDTTrueGuard) {
            return guard1;
        }

        if (guard1 instanceof SDTAndGuard && guard2 instanceof SDTAndGuard) {
            List<SDTGuard> guards = new ArrayList<SDTGuard>(((SDTAndGuard) guard1).getGuards());
            guards.addAll(((SDTAndGuard) guard2).getGuards());
            return new SDTAndGuard(guard1.getParameter(), guards.toArray(new SDTGuard[] {}));
        }

        if (guard1 instanceof SDTAndGuard || guard2 instanceof SDTAndGuard) {
            SDTAndGuard andGuard = guard1 instanceof SDTAndGuard ? (SDTAndGuard) guard1 : (SDTAndGuard) guard2;
            SDTGuard otherGuard = guard2 instanceof SDTAndGuard ? guard1 : guard2;
            SDTGuard[] conjuncts = andGuard.getGuards().toArray(new SDTGuard[andGuard.getGuards().size() + 1]);
            conjuncts[conjuncts.length - 1] = otherGuard;
            return new SDTAndGuard(guard1.getParameter(), conjuncts);
        }
        return new SDTAndGuard(guard1.getParameter(), guard1, guard2);
    }

    private boolean canBeMerged(SDTGuard a, SDTGuard b, MultiTheorySDTLogicOracle mlo,
            Mapping<SymbolicDataValue, DataValue<?>> valuation) {
        if (a.equals(b) || a instanceof SDTTrueGuard || b instanceof SDTTrueGuard)
            return true;

        // some quick answers, implemented for compatibility with older theories.
        if (a instanceof EqualityGuard)
            if (b.equals(((EqualityGuard) a).toDeqGuard()))
                return false;
        if (b instanceof EqualityGuard)
            if (a.equals(((EqualityGuard) b).toDeqGuard()))
                return false;
        return !mlo.areMutuallyExclusive(a.toTG(), new PIV(), b.toTG(), new PIV(), valuation);
    }

    private boolean refines(SDTGuard a, SDTGuard b, MultiTheorySDTLogicOracle mlo,
            Mapping<SymbolicDataValue, DataValue<?>> valuation) {
        if (b instanceof SDTTrueGuard)
            return true;
        boolean ref1 = mlo.doesRefine(a.toTG(), new PIV(), b.toTG(), new PIV(), valuation);
        return ref1;
    }

    // produces a mapping from top level SDT guards to the next level SDTs. Since
    // the same guard can appear
    // in multiple SDTs, the guard maps to a list of SDTs
    private Map<SDTGuard, List<SDT>> getChildren(SDT[] sdts) {
        List<Map<SDTGuard, SDT>> sdtChildren = Stream.of(sdts).map(sdt -> sdt.getChildren())
                .collect(Collectors.toList());
        Map<SDTGuard, List<SDT>> children = new LinkedHashMap<>();
        for (Map<SDTGuard, SDT> child : sdtChildren) {
            child.forEach((guard, nextSdt) -> {
                children.putIfAbsent(guard, new ArrayList<>());
                children.get(guard).add(nextSdt);
            });
        }

        return children;
    }

    private Mapping<SymbolicDataValue, DataValue<?>> buildValuation(ParValuation parValuation,
            Word<PSymbolInstance> prefix, PIV piv, Constants constants) {
        Mapping<SymbolicDataValue, DataValue<?>> valuation = new Mapping<SymbolicDataValue, DataValue<?>>();
        DataValue<?>[] values = DataWords.valsOf(prefix);
        piv.forEach((param, reg) -> valuation.put(reg, values[param.getId() - 1]));
        parValuation.forEach((param, dv) -> valuation.put(new SuffixValue(param.getType(), param.getId()), dv));
        constants.forEach((c, dv) -> valuation.put(c, dv));
        return valuation;
    }

    public Map<Word<PSymbolInstance>, Boolean> instantiate(Word<PSymbolInstance> prefix, SymbolicSuffix suffix,
            SymbolicDecisionTree sdt, PIV piv) {

        assert (sdt instanceof SDT);
        Map<Word<PSymbolInstance>, Boolean> words = new LinkedHashMap<Word<PSymbolInstance>, Boolean>();
        instantiate(words, prefix, suffix, (SDT) sdt, piv, 0, 0,
                new ParValuation(), new ParameterGenerator(), new ParValuation(), new ParameterGenerator());
        return words;
    }

    private void instantiate(Map<Word<PSymbolInstance>, Boolean> words, Word<PSymbolInstance> prefix,
            SymbolicSuffix suffix, SDT sdt, PIV piv, int aidx, int pidx,
            ParValuation pval, ParameterGenerator pgen, ParValuation gpval, ParameterGenerator gpgen) {
        if (aidx == suffix.getActions().length()) {
            words.put(prefix, sdt.isAccepting());
        } else {
            ParameterizedSymbol ps = suffix.getActions().getSymbol(aidx);
            if (ps.getArity() == pidx) {
                DataValue[] vals = pval.values().toArray(new DataValue [] {});
                PSymbolInstance psi = new PSymbolInstance(ps, vals);
                Word<PSymbolInstance> newPrefix = prefix.append(psi);
                instantiate(words, newPrefix, suffix, sdt, piv, aidx+1, 0, new ParValuation(), new ParameterGenerator(), gpval, gpgen);
            } else {
                Parameter p = pgen.next(ps.getPtypes()[pidx]);
                Parameter gp = gpgen.next(ps.getPtypes()[pidx]);
                Theory t = teachers.get(ps.getPtypes()[pidx]);
                for (Map.Entry<SDTGuard, SDT> entry : sdt.getChildren().entrySet()) {
                    DataValue val = t.instantiate(prefix, ps, piv, gpval, constants, entry.getKey(), p, Collections.emptySet());
                    ParValuation newPval = new ParValuation();
                    newPval.putAll(pval);
                    newPval.put(p, val);
                    ParValuation newGpval = new ParValuation();
                    newGpval.putAll(gpval);
                    newGpval.put(gp, val);
                    ParameterGenerator newPgen = new ParameterGenerator();
                    newPgen.set(pgen);
                    ParameterGenerator newGpgen = new ParameterGenerator();
                    newGpgen.set(gpgen);
                    instantiate(words, prefix, suffix, entry.getValue(), piv, aidx, pidx+1, newPval, newPgen, newGpval, newGpgen);
                }
            }
        }
    }

    /**
     * This method computes the initial branching for an SDT. It re-uses existing
     * valuations where possible.
     *
     */
    @Override
    public Branching updateBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, Branching current, PIV piv,
            SymbolicDecisionTree... sdts) {

        MultiTheoryBranching oldBranching = (MultiTheoryBranching) current;

        Map<Parameter, Set<DataValue>> oldDvs = oldBranching.getDVs();

        SDT[] casted = new SDT[sdts.length + 1];
        casted[0] = oldBranching.buildFakeSDT();


        VarMapping remapping = piv.createRemapping(oldBranching.getPiv());

        for (int i = 0; i < sdts.length; i++) {
            if (sdts[i] instanceof SDTLeaf) {
                casted[i + 1] = (SDTLeaf) sdts[i];
            } else {
                casted[i + 1] = (SDT) sdts[i].relabel(remapping);
            }
        }

        PIV relabeledPiv = piv.relabel(remapping);
        PIV newPiv = new PIV();
        newPiv.putAll(oldBranching.getPiv());
        newPiv.putAll(relabeledPiv);

        ParValuation pval = new ParValuation();

        Node n = createNode(1, prefix, ps, newPiv, pval, oldDvs, casted);

        MultiTheoryBranching fluff = new MultiTheoryBranching(prefix, ps, n, newPiv, pval, constants, casted);
        return fluff;
    }

    public boolean accepts(Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix, SymbolicDecisionTree sdt, PIV piv) {
    	Mapping<SymbolicDataValue, DataValue<?>> mapping = new Mapping<>();
    	mapping.putAll(constants);

    	int index = 0;
    	for (PSymbolInstance psi : prefix) {
    		index = index + psi.getBaseSymbol().getArity();
    	}

    	SuffixValueGenerator svGen = new SuffixValueGenerator();
    	for (PSymbolInstance psi : suffix) {
    		DataType[] dts = psi.getBaseSymbol().getPtypes();
    		DataValue[] dvs = psi.getParameterValues();
    		for (int i = 0; i < dts.length; i++) {
    			SuffixValue sv = svGen.next(dts[i]);
    			mapping.put(sv, dvs[i]);
    		}
    	}

    	ParValuation pars = DataWords.computeParValuation(prefix);
    	VarValuation vars = DataWords.computeVarValuation(pars, piv);
    	mapping.putAll(vars);

    	SDT _sdt = (SDT)sdt;
    	GuardExpression expr = _sdt.getAcceptingPaths(constants);
    	if (expr instanceof FalseGuardExpression)
    		return false;
    	for (SymbolicDataValue sdv : expr.getSymbolicDataValues()) {
    		if (sdv instanceof Register && mapping.get(sdv) == null) {
    			Theory teach = teachers.get(sdv.getType());
    			List<DataValue<?>> values = new ArrayList<>();
    			values.addAll(mapping.values());
    			values.addAll(pars.values());
    			DataValue dv = teach.getFreshValue(values);
    			mapping.put(sdv, dv);
    		}
    	}

    	return expr.isSatisfied(mapping);
    }
}
