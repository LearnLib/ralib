/*
 * Copyright (C) 2014-2025 The LearnLib Contributors
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.learnlib.logging.Category;
import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryBranching.Node;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTLeaf;
import de.learnlib.ralib.theory.Theory;
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

    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;

    private final ConstraintSolver solver;

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiTheoryTreeOracle.class);

    public MultiTheoryTreeOracle(DataWordOracle oracle, Map<DataType, Theory> teachers, Constants constants,
            ConstraintSolver solver) {
        this.oracle = oracle;
        this.teachers = teachers;
        this.constants = constants;
        this.solver = solver;
        this.restrictionBuilder = new SymbolicSuffixRestrictionBuilder(constants, teachers);
    }

    @Override
    public TreeQueryResult treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
        SDT sdt = treeQuery(prefix, suffix, new WordValuation(), constants, new SuffixValuation());
        //System.out.println(sdt);
        return new TreeQueryResult(sdt);
    }

    @Override
    public SDT treeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix, WordValuation values,
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

        Theory teach = teachers.get(sd.getDataType());

        // make a new tree query for prefix, suffix, prefix valuation, ...
        // to the correct teacher (given by type of first DV in suffix)
        return teach.treeQuery(prefix, suffix, values, constants, suffixValues, this);
    }

    /**
     * This method computes the initial branching for an SDT. It reuses existing
     * valuations where possible.
     *
     */
    @Override
    public Branching getInitialBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, SDT... sdts) {

        LOGGER.info(Category.QUERY, "computing initial branching for {0} after {1}", new Object[] { ps, prefix });

        MultiTheoryBranching mtb;
        Node n;

        if (sdts.length == 0) {
            n = createFreshNode(1, prefix, ps, new SuffixValuation());
            mtb = new MultiTheoryBranching(prefix, ps, n, constants, sdts);
        } else {
            n = createNode(1, prefix, ps, new SuffixValuation(), new LinkedHashMap<>(), sdts);
            MultiTheoryBranching fluff = new MultiTheoryBranching(prefix, ps, n, constants, sdts);
            mtb = fluff;
        }

        LOGGER.trace(Category.QUERY, mtb.toString());

        return mtb;
    }

    private Node createFreshNode(int i, Word<PSymbolInstance> prefix, ParameterizedSymbol ps, SuffixValuation pval) {

        if (i == ps.getArity() + 1) {
            return new Node(new SuffixValue(null, i));
        } else {
            Map<DataValue, Node> nextMap = new LinkedHashMap<>();
            Map<DataValue, SDTGuard> guardMap = new LinkedHashMap<>();

            DataType type = ps.getPtypes()[i - 1];
            LOGGER.trace(Category.QUERY, "current type: " + type.getName());
            SuffixValue p = new SuffixValue(type, i);
            SDTGuard guard = new SDTGuard.SDTTrueGuard(new SuffixValue(type, i));
            Theory teach = teachers.get(type);
            DataValue dvi = teach.instantiate(prefix, ps, pval, constants, guard, p, new LinkedHashSet<>());
            pval.put(p, dvi);

            nextMap.put(dvi, createFreshNode(i + 1, prefix, ps, pval));

            guardMap.put(dvi, guard);
            return new Node(p, nextMap, guardMap);
        }
    }

    private Node createNode(int i, Word<PSymbolInstance> prefix, ParameterizedSymbol ps, SuffixValuation pval,
            SDT... sdts) {
        Node n = createNode(i, prefix, ps, pval, new LinkedHashMap<>(), sdts);
        return n;
    }

    private Node createNode(int i, Word<PSymbolInstance> prefix, ParameterizedSymbol ps, SuffixValuation pval,
            Map<SuffixValue, Set<DataValue>> oldDvMap, SDT... sdts) {

        if (i == ps.getArity() + 1) {
            return new Node(new SuffixValue(null, i));
        } else {
            // obtain the data type, teacher, parameter
            DataType type = ps.getPtypes()[i - 1];
            Theory teach = teachers.get(type);
            SuffixValue p = new SuffixValue(type, i);

            // valuation
            Mapping<SymbolicDataValue, DataValue> valuation = buildValuation(pval, prefix, constants);

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
                DataValue dvi = teach.instantiate(prefix, ps, pval, constants, guard, p, oldDvs);
                // if merging of guards is done properly, there should be no case where the
                // guard cannot be instantiated.
                assert (dvi != null);

                SDT[] nextLevelSDTs = oldGuards.stream().map(g -> nextSDTs.get(g)).flatMap(g -> g.stream()) // stream
                                                                                                            // with of
                                                                                                            // sdt lists
                                                                                                            // for old
                                                                                                            // guards
                        .distinct().toArray(SDT[]::new); // merge and pick distinct elements

                SuffixValuation otherPval = new SuffixValuation();
                otherPval.putAll(pval);
                otherPval.put(p, dvi);

                nextMap.put(dvi, createNode(i + 1, prefix, ps, otherPval, oldDvMap, nextLevelSDTs));
                if (guardMap.containsKey(dvi)) {
                    throw new IllegalStateException(
                            "Guard instantiated using a dvi that was already used to instantiate a prior guard.");
                }
                guardMap.put(dvi, guard);
            }

            LOGGER.trace(Category.QUERY, "guardMap: " + guardMap);
            LOGGER.trace(Category.QUERY, "nextMap: " + nextMap);
            assert !nextMap.isEmpty();
            assert !guardMap.isEmpty();
            return new Node(p, nextMap, guardMap);
        }
    }

    // conjoins the initial guards of the SDTs producing a map from new refined
    // (initial) guards to the set of guards they originated from
    private Map<SDTGuard, Set<SDTGuard>> getNewRefinedInitialGuards(SDT[] sdts, MultiTheorySDTLogicOracle mlo,
            Mapping<SymbolicDataValue, DataValue> valuation) {
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
            MultiTheorySDTLogicOracle mlo, Mapping<SymbolicDataValue, DataValue> valuation) {
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

        if (guard1 instanceof SDTGuard.SDTTrueGuard) {
            return guard2;
        }

        if (guard2 instanceof SDTGuard.SDTTrueGuard) {
            return guard1;
        }

        if (guard1 instanceof SDTGuard.SDTAndGuard && guard2 instanceof SDTGuard.SDTAndGuard) {
            List<SDTGuard> guards = new ArrayList<SDTGuard>(((SDTGuard.SDTAndGuard) guard1).conjuncts());
            guards.addAll(((SDTGuard.SDTAndGuard) guard2).conjuncts());
            return new SDTGuard.SDTAndGuard(guard1.getParameter(), guards);
        }

        if (guard1 instanceof SDTGuard.SDTAndGuard || guard2 instanceof SDTGuard.SDTAndGuard) {
            SDTGuard.SDTAndGuard andGuard = guard1 instanceof SDTGuard.SDTAndGuard ?
                    (SDTGuard.SDTAndGuard) guard1 : (SDTGuard.SDTAndGuard) guard2;
            SDTGuard otherGuard = guard2 instanceof SDTGuard.SDTAndGuard ? guard1 : guard2;
            List<SDTGuard> conjuncts = andGuard.conjuncts();
            conjuncts.add(otherGuard);
            return new SDTGuard.SDTAndGuard(guard1.getParameter(), conjuncts);
        }
        return new SDTGuard.SDTAndGuard(guard1.getParameter(), List.of(guard1, guard2));
    }

    private boolean canBeMerged(SDTGuard a, SDTGuard b, MultiTheorySDTLogicOracle mlo,
            Mapping<SymbolicDataValue, DataValue> valuation) {
        if (a.equals(b) || a instanceof SDTGuard.SDTTrueGuard || b instanceof SDTGuard.SDTTrueGuard)
            return true;

        // FIXME: Falk added this to prevent and of two equals
        if (a instanceof SDTGuard.EqualityGuard && b instanceof SDTGuard.EqualityGuard)
            return false;

        // some quick answers, implemented for compatibility with older theories.
        if (a instanceof SDTGuard.EqualityGuard)
            if (b.equals( SDTGuard.toDeqGuard(a) ))
                return false;
        if (b instanceof SDTGuard.EqualityGuard)
            if (a.equals( SDTGuard.toDeqGuard(b) ))
                return false;
        return !mlo.areMutuallyExclusive(SDTGuard.toExpr(a), SDTGuard.toExpr(b), valuation);
    }

    private boolean refines(SDTGuard a, SDTGuard b, MultiTheorySDTLogicOracle mlo,
            Mapping<SymbolicDataValue, DataValue> valuation) {
        if (b instanceof SDTGuard.SDTTrueGuard)
            return true;
        boolean ref1 = mlo.doesRefine(SDTGuard.toExpr(a), SDTGuard.toExpr(b), valuation);
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

    private Mapping<SymbolicDataValue, DataValue> buildValuation(SuffixValuation suffixValuation,
            Word<PSymbolInstance> prefix, Constants constants) {
        Mapping<SymbolicDataValue, DataValue> valuation = new Mapping<SymbolicDataValue, DataValue>();
        //DataValue[] values = DataWords.valsOf(prefix);
        //piv.forEach((param, reg) -> valuation.put(reg, values[param.getId() - 1]));
        valuation.putAll(suffixValuation);
        valuation.putAll(constants);
        return valuation;
    }

    @Override
    public Map<Word<PSymbolInstance>, Boolean> instantiate(Word<PSymbolInstance> prefix, SymbolicSuffix suffix,
            SDT sdt) {

        Map<Word<PSymbolInstance>, Boolean> words = new LinkedHashMap<Word<PSymbolInstance>, Boolean>();
        instantiate(words, prefix, suffix,  sdt, 0, 0,
                new SuffixValuation(), new ParameterGenerator(), new SuffixValuation(), new ParameterGenerator());
        return words;
    }

    private void instantiate(Map<Word<PSymbolInstance>, Boolean> words, Word<PSymbolInstance> prefix,
            SymbolicSuffix suffix, SDT sdt, int aidx, int pidx,
                             SuffixValuation pval, ParameterGenerator pgen, SuffixValuation gpval, ParameterGenerator gpgen) {
        if (aidx == suffix.getActions().length()) {
            words.put(prefix, sdt.isAccepting());
        } else {
            ParameterizedSymbol ps = suffix.getActions().getSymbol(aidx);
            if (ps.getArity() == pidx) {
                DataValue[] vals = pval.values().toArray(new DataValue [] {});
                PSymbolInstance psi = new PSymbolInstance(ps, vals);
                Word<PSymbolInstance> newPrefix = prefix.append(psi);
                instantiate(words, newPrefix, suffix, sdt, aidx+1, 0, new SuffixValuation(), new ParameterGenerator(), gpval, gpgen);
            } else {
                SuffixValue p = new SuffixValue(ps.getPtypes()[pidx], pgen.next(ps.getPtypes()[pidx]).getId());
                SuffixValue gp = new SuffixValue( ps.getPtypes()[pidx], gpgen.next(ps.getPtypes()[pidx]).getId() );
                Theory t = teachers.get(ps.getPtypes()[pidx]);
                for (Map.Entry<SDTGuard, SDT> entry : sdt.getChildren().entrySet()) {
                    DataValue val = t.instantiate(prefix, ps, gpval, constants, entry.getKey(), p, Collections.emptySet());
                    SuffixValuation newPval = new SuffixValuation();
                    newPval.putAll(pval);
                    newPval.put(p, val);
                    SuffixValuation newGpval = new SuffixValuation();
                    newGpval.putAll(gpval);
                    newGpval.put(gp, val);
                    ParameterGenerator newPgen = new ParameterGenerator();
                    newPgen.set(pgen);
                    ParameterGenerator newGpgen = new ParameterGenerator();
                    newGpgen.set(gpgen);
                    instantiate(words, prefix, suffix, entry.getValue(), aidx, pidx+1, newPval, newPgen, newGpval, newGpgen);
                }
            }
        }
    }

    /**
     * This method computes the initial branching for an SDT. It reuses existing
     * valuations where possible.
     *
     */
    @Override
    public Branching updateBranching(Word<PSymbolInstance> prefix, ParameterizedSymbol ps, Branching current,
            SDT... sdts) {

        //System.out.println(current);
        //for (SDT s: sdts) {
        //    System.out.println(s);
        //}

        MultiTheoryBranching oldBranching = (MultiTheoryBranching) current;

        Map<SuffixValue, Set<DataValue>> oldDvs = oldBranching.getDVs();

        SDT[] casted = new SDT[sdts.length + 1];
        casted[0] = oldBranching.buildFakeSDT();


        //VarMapping remapping = piv.createRemapping(oldBranching.getPiv());
        // todo: this can be cleaned up
        for (int i = 0; i < sdts.length; i++) {
            if (sdts[i] instanceof SDTLeaf) {
                casted[i + 1] = (SDTLeaf) sdts[i];
            } else {
                casted[i + 1] =  sdts[i];
            }
        }

        Node n = createNode(1, prefix, ps, new SuffixValuation(),oldDvs, casted);

        MultiTheoryBranching fluff = new MultiTheoryBranching(prefix, ps, n, constants, casted);
        return fluff;
    }

    public Map<DataType, Theory> getTeachers() {
    	return teachers;
    }

    @Override
    public SymbolicSuffixRestrictionBuilder getRestrictionBuilder() {
    	return restrictionBuilder;
    }
}
