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
package de.learnlib.ralib.equivalence;

import java.util.*;

import com.google.common.collect.Sets;

import de.learnlib.query.DefaultQuery;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.output.OutputMapping;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParameterValuation;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

/**
 *
 * @author fh
 */
public class IOEquivalenceTest implements IOEquivalenceOracle
{

    /* **********************************************************************
     * object pairs ...
     */

    private static class Pair<T1, T2> {

        private final T1 first;
        private final T2 second;

        public Pair(T1 first, T2 second) {
            this.first = first;
            this.second = second;
        }

        public T1 getFirst() {
            return first;
        }

        public T2 getSecond() {
            return second;
        }
    }


    /* **********************************************************************
     * state pair hash stuff ...
     */

    private static class Tuple {

        RALocation sys1loc;
        RALocation sys2loc;
        RegisterValuation sys1reg;
        RegisterValuation sys2reg;

        public Tuple(RALocation l1, RALocation l2, RegisterValuation r1, RegisterValuation r2) {
            sys1loc = l1;
            sys2loc = l2;
            sys1reg = RegisterValuation.copyOf(r1);
            sys2reg = RegisterValuation.copyOf(r2);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 31 * hash + (this.sys1loc != null ? this.sys1loc.hashCode() : 0);
            hash = 31 * hash + (this.sys2loc != null ? this.sys2loc.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Tuple other = (Tuple) obj;
            if (!Objects.equals(this.sys1loc, other.sys1loc)) {
                return false;
            }
            return Objects.equals(this.sys2loc, other.sys2loc);
        }

    }

    private boolean compatible(Tuple t1, Tuple t2)
    {
        // compare locations ...
        if (!t1.equals(t2))
            return false;

//        log.trace(t1.sys1reg);
//        log.trace(t1.sys2reg);
//        log.trace(t2.sys1reg);
//        log.trace(t2.sys2reg);

        // compare registers
        LinkedHashMap<Object,Object> vMap = new LinkedHashMap<>();

        return compareRegister(t1.sys1reg, t2.sys1reg, vMap) &&
               compareRegister(t1.sys2reg, t2.sys2reg, vMap);
    }

    private boolean compareRegister(
            RegisterValuation r1, RegisterValuation r2, Map<Object,Object> vMap) {

        for (Map.Entry<Register,DataValue> entry : r1.entrySet()) {
            DataValue v1 = entry.getValue();
            DataValue v2 = r2.get(entry.getKey());

            boolean n1 = (v1 == null);
            boolean n2 = (v2 == null);

            if (n1 && n2)
                continue;

            if (n1 || n2)
                return false;

            Object o1 = vMap.get(v1.getValue());
            if (o1 != null && !o1.equals(v2.getValue())) {
                return false;
            }

            if (!v1.equals(v2)) {
                for (DataValue cv : consts.values()) {
                    if (v1.equals(cv) || v2.equals(cv))
                        return false;
                }
            }
            vMap.put( v1.getValue(), v2.getValue());
        }

        return true;
    }

    /* **********************************************************************
     * queue elements
     */

    private static class Triple {

        RALocation sys1loc;
        RALocation sys2loc;
        RegisterValuation sys1reg;
        RegisterValuation sys2reg;
        Word<PSymbolInstance> as;
        Word<PSymbolInstance> trace;

        public Triple(RALocation l1, RALocation l2, RegisterValuation r1, RegisterValuation r2, Word<PSymbolInstance> w, Word<PSymbolInstance> t) {
            sys1loc = l1;
            sys2loc = l2;
            sys1reg = RegisterValuation.copyOf(r1);
            sys2reg = RegisterValuation.copyOf(r2);
            as = w;
            trace = t;
        }

    }

    /* **********************************************************************
     * main class implementation
     */

    private final RegisterAutomaton sys1;
    private RegisterAutomaton sys2;

    private final Map<DataType, Theory> teacher;

    private final Constants consts;

    private final ParameterizedSymbol[] actions;

    private final boolean checkForEqualParameters;

    public IOEquivalenceTest(RegisterAutomaton in1,
            Map<DataType, Theory> teacher, Constants consts,
            boolean checkForEqualParameters,
            ParameterizedSymbol ... actions) {

        this.sys1 = in1;
        this.teacher = teacher;
        this.consts = consts;
        this.actions = actions;
        this.checkForEqualParameters = checkForEqualParameters;
    }

    @Override
    public DefaultQuery<PSymbolInstance, Boolean> findCounterExample(
            RegisterAutomaton a, Collection<? extends PSymbolInstance> clctn) {

        this.sys2 = a;

        LinkedList<Triple> q = new LinkedList<>();
        Triple start = new Triple(sys1.getInitialState(), sys2.getInitialState(),
                sys1.getInitialRegisters(), sys2.getInitialRegisters(),
                Word.epsilon(), Word.epsilon());

        q.offer(start);

        LinkedHashMap<Tuple,ArrayList<Tuple>> visited = new LinkedHashMap<>();
        Tuple st = new Tuple(start.sys1loc, start.sys2loc, start.sys1reg, start.sys2reg);
        visited.put(st, new ArrayList<Tuple>());
        visited.get(st).add(st);

        while (!q.isEmpty())
        {
            Triple t = q.poll();

            for (ParameterizedSymbol ps : actions)
            {
                if (!(ps instanceof InputSymbol)) {
                    continue;
                }

                List<Word<PSymbolInstance>> words =
                        getNext(t.as, ps, t.sys1reg, checkForEqualParameters);

                for (Word<PSymbolInstance> w : words)
                {
                    //log.trace("----------------------------------------------------------------------");
                    //log.trace(w);

                    Triple next = new Triple(null, null, null, null, w, null);

                    Pair<PSymbolInstance,PSymbolInstance> out =
                            executeStep(t, w.lastSymbol(), next);

                    //log.trace(w + " = " + out.getFirst() + " : " + out.getSecond());
                    if (out.getFirst()== null) {
                        throw new IllegalStateException();
                    }
                    // book keep the trace
                    next.trace = t.trace.append(w.lastSymbol()).append(out.getFirst());

                    // found counterexample
                    if (out.getSecond() == null || !out.getFirst().equals(out.getSecond())) {
                        //System.out.println("CE: " + out.getFirst() + " : " + out.getSecond());
                        return new DefaultQuery<>(next.trace, true);
                    }
                    // FIXME: this may not be OK in general. I think it is ok
                    // for learning ....
                    //if (hasDoubles(next.sys2reg)) {
                    //    continue;
                    //}

                    st = new Tuple(next.sys1loc, next.sys2loc, next.sys1reg, next.sys2reg);
                    ArrayList<Tuple> comp = visited.get(st);

                    // first one
                    if (comp == null)
                    {
                        //log.trace("First one in list");
                        visited.put(st, new ArrayList<Tuple>());
                    }
                    else
                    {
                        //log.trace("Found list");
                        boolean found = false;
                        for (Tuple xx : comp) {
                            if (compatible(xx, st))
                            {
                                found = true;
                                //log.trace("skip " + w);
                                break;
                            }
                        }
                        if (found)
                            continue;
                    }
                    q.add(next);
                    visited.get(st).add(st);
                    //log.trace("added " + w + " to queue");
                }
            }
        }

        return null;
    }

//    private static boolean hasDoubles(RegisterValuation r) {
//        Set<Object> s = new LinkedHashSet<>();
//        int x = 0;
//        for (String key : r.getKeys()) {
//            if (!key.startsWith("r")) {
//                continue;
//            }
//            DataValue v = r.resolveLocal(new Reference(key)).getValue();
//            if (!v.equals(DataValue.NULL)) {
//                x++;
//                s.add(v.getValue());
//            }
//        }
//        return (s.size() != x);
//    }

    private Pair<PSymbolInstance, PSymbolInstance> executeStep(
            Triple in, PSymbolInstance psi, Triple out)
    {
        out.sys1reg = RegisterValuation.copyOf(in.sys1reg);
        out.sys2reg = RegisterValuation.copyOf(in.sys2reg);

        ParameterValuation pval = ParameterValuation.fromPSymbolInstance(psi);

        // first sys input
        RALocation loc1 = null;
        //System.out.println(psi + " from " + in.sys1loc);
        for (Transition t : in.sys1loc.getOut(psi.getBaseSymbol()))
        {
            if ( t.isEnabled(out.sys1reg, pval, consts) )
            {
                loc1 = t.getDestination();
                out.sys1reg = t.execute(out.sys1reg, pval, consts);
                break;
            }
        }

        if (loc1 == null)
            throw new IllegalStateException("No transition enabled (sys1):" + in.as + psi);

        // second sys input
        RALocation loc2 = null;
        for (Transition t : in.sys2loc.getOut(psi.getBaseSymbol()))
        {
            if ( t.isEnabled(out.sys2reg, pval, consts) )
            {
                loc2 = t.getDestination();
                out.sys2reg = t.execute(out.sys2reg, pval, consts);
                break;
            }
        }

        if (loc2 == null) {
            throw new IllegalStateException("No transition enabled (sys2): " + in.as + " (" + in.sys2loc + ") " + psi);
        }

        // first sys output prepare
        OutputTransition ot1 = getOutputTransition(loc1, out.sys1reg);
        if (ot1 == null)
            throw new IllegalStateException("No output transition enabled (sys1)");

        PSymbolInstance ret1 = createOutputSymbol(ot1, out.sys1reg, out.sys2reg);

        // second sys output prepare
        OutputTransition ot2 = getOutputTransition(loc2, out.sys2reg);
        if (ot2 == null) {
            return new Pair<>(ret1, null);
        }
        PSymbolInstance ret2 = createOutputSymbol(ot2, out.sys2reg, out.sys1reg);

        // first sys output commit
        out.sys1reg = ot1.execute(out.sys1reg, ParameterValuation.fromPSymbolInstance(ret1), consts);
        out.sys1loc = ot1.getDestination();

        if (out.sys1loc == null) {
            throw new IllegalStateException("No transition enabled (sys1/o): " + in.as + "(" + loc1 + ")" + psi);
        }

        // second sys output commit
        out.sys2reg = ot2.execute(out.sys2reg, ParameterValuation.fromPSymbolInstance(ret2), consts);
        out.sys2loc = ot2.getDestination();

        if (out.sys2loc == null) {
            throw new IllegalStateException("No transition enabled (sys2/o): " + in.as + psi);
        }

        return new Pair<>(ret1, ret2);
    }

    private List<Word<PSymbolInstance>> getNext(Word<PSymbolInstance> w,
            ParameterizedSymbol ps, RegisterValuation r1, //RegisterValuation r2,
            boolean checkForEqualParameters) {

        Set<DataValue> potential = new LinkedHashSet<>();
        potential.addAll(r1.values());
        //  this is maybe ok during learning
        //potential.addAll(r2.values());

        //System.out.println("Base: " + w + " : " + Arrays.toString(potential.toArray()));

        // TODO: this should become part of the teacher
        potential.addAll(consts.values());

        List<DataValue[]> valuations = new ArrayList<>();
        computeValuations(ps, valuations, potential,
                new ArrayList<DataValue>(),checkForEqualParameters, DataWords.valSet(w));

        List<Word<PSymbolInstance>> ret = new ArrayList<>();
        for (DataValue[] data : valuations) {
            Word<PSymbolInstance> next = w.append(new PSymbolInstance(ps, data));
            ret.add(next);
        }
        return ret;
    }

    // FIXME: this works only for the equality case!!!!
    private void computeValuations(ParameterizedSymbol ps, List<DataValue[]> valuations,
            Set<DataValue> potential, List<DataValue> val,
            boolean checkForEqualParameters, Set<DataValue> prefixVals) {

        int idx = val.size();
        if (idx >= ps.getArity()) {
            valuations.add(val.toArray(new DataValue[] {}));
            return;
        }

        DataType t = ps.getPtypes()[idx];

        Set<DataValue> next = valSet(potential, t);
        Set<DataValue> forFresh = new LinkedHashSet<>(Sets.union(next, valSet(val, t)));
        if (checkForEqualParameters) {
            next.addAll(forFresh);
        }
        forFresh.addAll(valSet(prefixVals, t));
        Theory teach = teacher.get(t);
        //System.out.println("FOR FRESH: " + Arrays.toString(forFresh.toArray()) + " for " + t);
        DataValue fresh = teach.getFreshValue(new ArrayList<>(forFresh));
        //next.add(fresh);
        List<DataValue> nextValFresh = new ArrayList<>(val);
        nextValFresh.add(fresh);
        computeValuations(ps, valuations, potential,
                nextValFresh, checkForEqualParameters, prefixVals);

        for (DataValue d : next) {
            List<DataValue> nextVal = new ArrayList<>(val);
            nextVal.add(d);
            computeValuations(ps, valuations, potential,
                    nextVal, checkForEqualParameters, prefixVals);
        }
    }

    private Set<DataValue> valSet(Collection<DataValue> in, DataType t) {
        Set<DataValue> out = new LinkedHashSet<>();
        for (DataValue dv : in) {
                if (dv.getDataType().equals(t)) {
                    out.add(dv);
                }
        }
        return out;
    }

    private PSymbolInstance createOutputSymbol(OutputTransition ot, RegisterValuation register, RegisterValuation register2) {
        ParameterizedSymbol ps = ot.getLabel();
        OutputMapping mapping = ot.getOutput();
        DataValue[] vals = new DataValue[ps.getArity()];
        SymbolicDataValueGenerator.ParameterGenerator pgen =
                new SymbolicDataValueGenerator.ParameterGenerator();
        ParameterValuation pval = new ParameterValuation();
        int i = 0;
        for (DataType t : ps.getPtypes()) {
            SymbolicDataValue.Parameter p = pgen.next(t);
            if (!mapping.getOutput().containsKey(p)) {
                Set<DataValue> forFresh = new LinkedHashSet<>();
                forFresh.addAll(register.values());
                forFresh.addAll(register2.values());
                List<DataValue> old = computeOld(t, pval, valSet(forFresh, t));
                //System.out.println("OLD:" + Arrays.toString(old.toArray()));
                vals[i] = teacher.get(t).getFreshValue(old);
            }
            else {
                SymbolicDataValue sv = mapping.getOutput().get(p);
                if (sv.isRegister()) {
                    vals[i] = register.get( (Register) sv);
                }
                else if (sv.isConstant()) {
                    vals[i] = consts.get( (SymbolicDataValue.Constant) sv);
                }
                else if (sv.isParameter()) {
                    throw new UnsupportedOperationException("not supported yet.");
                }
                else {
                    throw new IllegalStateException("this case is not supported.");
                }
            }
            assert vals[i] != null;
            pval.put(p, vals[i]);
            i++;
        }
        return new PSymbolInstance(ot.getLabel(), vals);
    }

    private OutputTransition getOutputTransition(RALocation loc, RegisterValuation reg) {
        for (Transition t : loc.getOut()) {
            OutputTransition ot = (OutputTransition) t;
            if (ot.canBeEnabled(reg, consts) && ot.getDestination().isAccepting()) {
                return ot;
            }
        }
        return null;
    }

    private List<DataValue> computeOld(DataType t,
            ParameterValuation pval, Set<DataValue> stored) {
        stored.addAll(consts.values());
        for (DataValue d : pval.values()){
            if (d.getDataType().equals(t)) {
                stored.add(d);
            }
        }
        return new ArrayList<>(stored);
    }

}
