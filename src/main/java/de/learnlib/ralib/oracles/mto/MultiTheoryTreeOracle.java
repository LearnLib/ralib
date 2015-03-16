/*
 * Copyright (C) 2014 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package de.learnlib.ralib.oracles.mto;

import de.learnlib.logging.LearnLogger;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
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
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.RegisterGenerator;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryBranching.Node;
import de.learnlib.ralib.theory.SDTCompoundGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class MultiTheoryTreeOracle implements TreeOracle, SDTConstructor {

    private final DataWordOracle oracle;

    private final Map<DataType, Theory> teachers;

    private static LearnLogger log = LearnLogger.getLogger(MultiTheoryTreeOracle.class);

    public MultiTheoryTreeOracle(DataWordOracle oracle, Map<DataType, Theory> teachers) {
        this.oracle = oracle;
        this.teachers = teachers;
    }

    @Override
    public TreeQueryResult treeQuery(
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
        //WordValuation prefixValuation = new WordValuation();
        //DataValue[] prefixValues = DataWords.valsOf(prefix);
        //for (int k = 0; k<prefixValues.length; k++) {
        //    prefixValuation.put(k, prefixValues[k]);
        //}
        PIV pir = new PIV();
        SDT sdt = treeQuery(prefix, suffix,
                new WordValuation(), pir, new SuffixValuation());

        // move registers to 1 ... n
        VarMapping rename = new VarMapping();
        RegisterGenerator gen = new RegisterGenerator();
        for (Register r : pir.values()) {
            rename.put(r, gen.next(r.getType()));
        }

        PIV piv = new PIV();
        Set<Register> regs = sdt.getRegisters();
        for (Entry<Parameter, Register> e : pir.entrySet()) {
            if (regs.contains(e.getValue())) {
                piv.put(e.getKey(), (Register) rename.get(e.getValue()));
            }
        }
        TreeQueryResult tqr = new TreeQueryResult(piv, sdt.relabel(rename));
        log.finer("PIV: " + piv);

        return tqr;
    }

    @Override
    public SDT treeQuery(
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix,
            WordValuation values, PIV pir,
            SuffixValuation suffixValues) {

        //log.log(Level.FINEST,"suffix length: " + DataWords.paramLength(suffix.getActions()) + ", values size: " + values.size() + ", suffixValues size " + suffixValues.size());
        // IF at the end of the word!
        if (values.size() == DataWords.paramLength(suffix.getActions())) {
            //log.log(Level.FINEST,"we're at the end of the wor(l)d!");
            // if we are at the end of the word, i.e., nothing more to 
            // instantiate, the number of values in the whole word is equal to 
            // the number of actions in the suffix

            //log.log(Level.FINEST,"attempting to concatenate suffix: " + suffix.getActions().toString() + " AND " + values.toString());
//            int startingPoint = DataWords.paramLength(DataWords.actsOf(prefix));
//            Map<
//          
            Word<PSymbolInstance> concSuffix = DataWords.instantiate(
                    suffix.getActions(), values);

            Word<PSymbolInstance> trace = prefix.concat(concSuffix);
            DefaultQuery<PSymbolInstance, Boolean> query
                    = new DefaultQuery<>(prefix, concSuffix);
            oracle.processQueries(Collections.singletonList(query));
            boolean qOut = query.getOutput();

            log.log(Level.FINEST, "Trace = " + trace.toString() + " >>> "
                    + (qOut ? "ACCEPT (+)" : "REJECT (-)"));
            return qOut ? SDTLeaf.ACCEPTING : SDTLeaf.REJECTING;

            // return accept / reject as a leaf
        }

//        log.log(Level.FINEST,"values passed to theory: " + values.toString());
        // OTHERWISE get the first noninstantiated data value in the suffix and its type
        SymbolicDataValue sd = suffix.getDataValue(values.size() + 1);
        //if (sd == null) {log.log(Level.FINEST,"breaking");}
        //log.log(Level.FINEST,"first uninstantiated value in suffix... " + sd.toString() + " of type " + sd.getType().getName());

        Theory teach = teachers.get(sd.getType());
        //log.log(Level.FINEST,"Teacher theory: " + sd.getType().toString());
        // make a new tree query for prefix, suffix, prefix valuation, ...
        // to the correct teacher (given by type of first DV in suffix)
        return teach.treeQuery(prefix, suffix, values, pir, suffixValues, this);
    }

    /**
     * This method computes the initial branching for an SDT. It re-uses
     * existing valuations where possible.
     *
     */
    @Override
    public Branching getInitialBranching(Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, PIV piv, SymbolicDecisionTree... sdts) {

        log.log(Level.INFO, "computing initial branching for {0} after {1}", new Object[]{ps, prefix});

        //TODO: check if this casting can be avoided by proper use of generics
        //TODO: the problem seems to be 
        SDT[] casted = new SDT[sdts.length];
        for (int i = 0; i < casted.length; i++) {
            casted[i] = (SDT) sdts[i];
            System.out.println("using " + sdts.length + " SDTs");
            //log.log(Level.FINE, "Using SDT \n{0}", sdts[i].toString());
        }

        MultiTheoryBranching mtb = getInitialBranching(
                prefix, ps, piv, new ParValuation(),
                new ArrayList<SDTGuard>(), casted);

        log.log(Level.FINEST, mtb.toString());

        return mtb;
    }

    // TODO: is this method actually needed??
    public Branching getInitialBranching(Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, PIV piv, ParValuation pval, SymbolicDecisionTree... sdts) {
        SDT[] casted = new SDT[sdts.length];
        for (int i = 0; i < casted.length; i++) {
            casted[i] = (SDT) sdts[i];
        }

        MultiTheoryBranching mtb = getInitialBranching(
                prefix, ps, piv, pval,
                new ArrayList<SDTGuard>(), casted);

        return mtb;
    }

    @Override
    // get the initial branching for the symbol ps after prefix given a certain tree
    public MultiTheoryBranching getInitialBranching(Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, PIV piv, ParValuation pval,
            List<SDTGuard> guards, SDT... sdts) {
        //List<Node> candidateNodes = new ArrayList<>();
        Node n;

        if (sdts.length == 0) {
            n = createFreshNode(1, prefix, ps, piv, pval);
            return new MultiTheoryBranching(prefix, ps, n, piv, pval, sdts);
        } else {
//            System.out.println("THESE ARE THE " + sdts.length + " SDTS WE'RE USING!!!: ----\n" + Arrays.toString(sdts));

//            SDT s = merge(sdts);
            //for (SDT s : sdts) {
//                for (Register r : s.getRegisters()) {
//                    int i = r.getId();
//                    //piv.put(new Parameter(r.getType(), r.getId()), r);
//                    DataValue[] prefValues = DataWords.valsOf(prefix);
//                    if (prefValues.length != 0) {
//                        pval.put(new Parameter(r.getType(), r.getId()), prefValues[i - 1]);
//                    }
//                }
//            }
            n = createNode(1, prefix, ps, piv, pval, sdts);
//            candidateNodes.add(n);
            //new MultiTheoryBranching(prefix, ps, n, piv, pval, sdts));
            //           }
            return new MultiTheoryBranching(prefix, ps, n, piv, pval, sdts);
        }

    }

   

    public Map<SymbolicDataValue, Variable> makeVarMapping(Set<SymbolicDataValue> regsAndParams) {
        Map<SymbolicDataValue, Variable> vars = new LinkedHashMap<SymbolicDataValue, Variable>();
        for (SymbolicDataValue s : regsAndParams) {
            SymbolicDataValue z = s;
            String xpre = "";
            if (s instanceof SuffixValue) {
                xpre = "y" + s.getId();
                z = new Parameter(s.getType(), s.getId());
            }
            if (s instanceof Register) {
                xpre = "x" + s.getId();
            }
//            String xname = xpre + s.getId() + "_" + s.getType().getName();
            Variable x = new Variable(BuiltinTypes.SINT32, xpre);
            vars.put(z, x);
        }
        return vars;
    }

    private Map<SDTGuard, SDT> makeGiantSdtMap(SDT sdt, Map<SDTGuard, SDT> giantChildMap) {
        Map<SDTGuard, SDT> children = sdt.getChildren();
        giantChildMap.putAll(children);
        for (Map.Entry<SDTGuard, SDT> e : children.entrySet()) {
            giantChildMap.put(e.getKey(), e.getValue());
            makeGiantSdtMap(e.getValue(), giantChildMap);
        }
        return giantChildMap;
    }

    private Map<SDTGuard, SDT> makeGiantSdtMap(SDT... sdts) {
        Map<SDTGuard, SDT> manyChildren = new LinkedHashMap<>();
        for (SDT sdt : sdts) {
            manyChildren.putAll(makeGiantSdtMap(sdt, manyChildren));
        }
        return manyChildren;
    }

    private Set<SymbolicDataValue> makeVarSet(SDTGuard guard) {
        Set<SymbolicDataValue> currRegsAndParams = new HashSet<>();
        currRegsAndParams.add(guard.getParameter());
        if (guard instanceof SDTCompoundGuard) {
            currRegsAndParams.addAll(((SDTCompoundGuard) guard).getAllRegs());
        } else if (guard instanceof SDTIfGuard) {
            currRegsAndParams.add(((SDTIfGuard) guard).getRegister());
        }
        return currRegsAndParams;
    }

    private int getEqId(SDTGuard g) {
        int i = 0;
        if (g instanceof SDTCompoundGuard) {
            for (SDTIfGuard x : ((SDTCompoundGuard) g).getGuards()) {
                if ((x instanceof EqualityGuard) || (x instanceof DisequalityGuard)) {
                    i = x.getRegister().getId();
                }
            }
        } else if (g instanceof SDTIfGuard) {
            i = ((SDTIfGuard) g).getRegister().getId();

        }
        return i;
    }

    private Set<SDTGuard> preprocess(int i, Set<SDTGuard> guards) {
        Set<SDTGuard> processed = new HashSet<>();
        for (SDTGuard guard : guards) {
            if (guard instanceof EqualityGuard) {
                processed.add(new EqualityGuard(guard.getParameter(), ((EqualityGuard) guard).getRegister()));
            } else if (guard instanceof DisequalityGuard) {
                processed.add(new DisequalityGuard(guard.getParameter(), ((DisequalityGuard) guard).getRegister()));
            } else {
                processed.add(guard);
            }
        }
        return processed;
    }

    private Set<SDTGuard> getFinestGuards(Map<SDTGuard, SDTGuard> gMap) {
        Set<SDTGuard> retSet = new LinkedHashSet<>();
        for (SDTGuard s : gMap.keySet()) {
            if (!gMap.containsValue(s)) {
                retSet.add(s);
            }

        }
        return retSet;
    }

    private Map<SDTGuard, SDTGuard> mapGuards(Set<SDTGuard> guards, Parameter param) {

        //Set<SDTGuard> guards = preprocess(getEqId(g), unrefined);
        SDTIfGuard[] ifg = new SDTIfGuard[0];
        SDTGuard g = new SDTTrueGuard(new SuffixValue(param.getType(),param.getId()));

        if (guards.contains(g)) {
            log.log(Level.FINEST, "!!!!!!! " + g.toString() + " is in  " + guards.toString());
            guards.remove(g);
        }
        SDTGuard finer = g;
        SDTGuard coarser = g;
        Map<SDTGuard, SDTGuard> refines = new LinkedHashMap<>();
        Map<SymbolicDataValue, Variable> gVars = makeVarMapping(makeVarSet(g));
        MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle();
        for (SDTGuard n : guards) {
            Map<SymbolicDataValue, Variable> nVars = makeVarMapping(makeVarSet(n));

            if (mlo.doesRefine(g.toTG(gVars), new PIV(), n.toTG(nVars), new PIV())
                    && (!mlo.doesRefine(n.toTG(nVars), new PIV(), g.toTG(gVars), new PIV()))) {
                finer = g;
                coarser = n;
                System.out.println("!!!!!!! " + g.toString() + " refines " + n.toString());

                    //if (mlo.doesRefine(n.toTG(nVars), new PIV(), g.toTG(gVars), new PIV())) {
                //    throw new IllegalStateException("Can't refine in the wrong direction");
                //}
            } else if (mlo.doesRefine(n.toTG(nVars), new PIV(), g.toTG(gVars), new PIV())
                    && (!mlo.doesRefine(g.toTG(gVars), new PIV(), n.toTG(nVars), new PIV()))) {
                //if (mlo.doesRefine(n.toTG(nVars), new PIV(), g.toTG(gVars), new PIV())){
                System.out.println("!!!!!!! " + n.toString() + " refines " + g.toString());
                finer = n;
                coarser = g;
            }
            refines.put(finer, coarser);
        }

//        List<SDTGuard> retList = new ArrayList();
//
//        retList.add(guard);
//        log.log(Level.FINEST,
//                "!!!!!!! retList " + retList.toString());
      log.log(Level.FINEST,
                "!!!!!!! refines " + refines.toString());
  //      retList.addAll(retSet);
        return refines;
    }

    private Map<SDTGuard, SDT> collectKids(SDT... sdts) {
        log.log(Level.FINEST, "!!!!!!!!!!!!!    " + Arrays.toString(sdts));
        Map<SDTGuard, SDT> allKids = new LinkedHashMap<>();
        for (SDT sdt : sdts) {
            if (sdt != null && !sdt.getChildren().isEmpty()) {
                for (Map.Entry<SDTGuard, SDT> e : sdt.getChildren().entrySet()) {
                    allKids.put(e.getKey(), e.getValue());
                }
            }
        }
        return allKids;
    }

    private List<SDTGuard> getAllCoarser(SDTGuard finer, Map<SDTGuard, SDTGuard> guardChain) {
        Set<SDTGuard> retSet = accGuards(new HashSet<SDTGuard>(), guardChain);

        return new ArrayList<>(retSet);
    }

    private Set<SDTGuard> accGuards(Set<SDTGuard> acc, Map<SDTGuard, SDTGuard> guardChain) {
        Set<SDTGuard> retSet = new HashSet<>();
        boolean flag = false;
        for (Map.Entry<SDTGuard, SDTGuard> e : guardChain.entrySet()) {
            if (retSet.contains(e.getKey())) {
                retSet.add(e.getValue());
                flag = true;
            }
        }
        if (flag == true) {
            return accGuards(retSet, guardChain);
        }
        else {
            return retSet;
        }
    }

     private Node createFreshNode(int i, Word<PSymbolInstance> prefix, ParameterizedSymbol ps,
            PIV piv, ParValuation pval) {
         
         if (i == ps.getArity() + 1) {
             return new Node(new Parameter(null, i));
         }
         else {
        Map<DataValue, Node> nextMap = new LinkedHashMap<>();
        Map<DataValue, SDTGuard> guardMap = new LinkedHashMap<>();

        DataType type = ps.getPtypes()[i-1];
            log.log(Level.FINEST, "current type: " + type.getName());
            Parameter p = new Parameter(type, i);
            SDTGuard guard = new SDTTrueGuard(new SuffixValue(type, i));
            //SDTGuard guard = new SDTCompoundGuard(new SuffixValue(type,j), new SDTIfGuard[0]);
            Theory teach = teachers.get(type);
            //ParValuation jpval = new ParValuation();
            //jpval.putAll(pval);
            //jpval.putAll(ipval);
            DataValue dvi = teach.instantiate(prefix, ps, piv, pval, guard, p);
            // try commenting out this
            ParValuation otherPval = new ParValuation();
            otherPval.putAll(pval);
            otherPval.put(p, dvi);

            nextMap.put(dvi, createFreshNode(i+1, prefix, ps, piv, otherPval));
            //pval.put(p,dvi);

            guardMap.put(dvi, guard);
            return new Node(p, nextMap, guardMap);
         }
    }

    private Node createNode(int i, Word<PSymbolInstance> prefix, ParameterizedSymbol ps,
            PIV piv, ParValuation pval, SDT... sdts) {
        
            
        if (i ==  ps.getArity() + 1) {
            return new Node(new Parameter(null, i));
        }

      //  if (i < ps.getArity()) {
            System.out.println("!!!Psymbol: " + ps.toString() + " i " + i);
            DataType type = ps.getPtypes()[i-1];
            log.log(Level.FINEST, "current type: " + type.getName());
            int numSdts = sdts.length;
            Parameter p = new Parameter(type, i);
            Map<DataValue, Node> nextMap = new LinkedHashMap<>();
            Map<DataValue, SDTGuard> guardMap = new LinkedHashMap<>();
            log.log(Level.FINEST, "number of sdts: " + numSdts);
            Theory teach = teachers.get(type);
                    

            if (numSdts == 1) {
                SDT sdt = sdts[0];
                for (Map.Entry<SDTGuard, SDT> e : sdt.getChildren().entrySet()) {
                    SDTGuard guard = e.getKey();
                    log.log(Level.FINEST, "processing guard: " + guard.toString());
                    //log.log(Level.FINEST,"...... wh piv is " + piv.toString());
                    //jpval.putAll(ipval);
                    DataValue dvi = teach.instantiate(prefix, ps, piv, pval, guard, p);
                    log.log(Level.FINEST, dvi.toString() + " maps to " + guard.toString());
                    // try commenting out this
                    //log.log(Level.FINEST,"dvi = " + dvi.toString());
                    ParValuation otherPval = new ParValuation();
                    otherPval.putAll(pval);
                    otherPval.put(p, dvi);
                    nextMap.put(dvi, createNode(i+1, prefix, ps, piv, otherPval, e.getValue()));
                    guardMap.put(dvi, guard);
                    
                }
                log.log(Level.FINEST, "guardMap: " + guardMap.toString());
                log.log(Level.FINEST, "nextMap: " + nextMap.toString());
                return new Node(p, nextMap, guardMap);
            } else {
//                for (int k = 0; k < numSdts - 1; k++) {
//                    int l = k + 1;
//                    SDT curr = sdts[k];
//                    SDT[] nxt = new SDT[numSdts - l];
//                    for (int y = l; y < numSdts; y++) {
//                        nxt[y - l] = sdts[y];
//                    }

//                    log.log(Level.FINEST, "current: " + curr.toString() + "\nnext: " + nxt[0].toString());

                // temporary test -----
                    SDT curr = sdts[0];
                    SDT[] nxt = new SDT[numSdts-1];
                    for (int y = 1; y < numSdts; y++) {
                        nxt[y - 1] = sdts[y];
                    }
                    // end test -------
                    
                    Map<SDTGuard, SDT> currChildren = curr.getChildren();
                    Map<SDTGuard, SDT> nxtChildren = collectKids(nxt);
                    Set<SDTGuard> finest = new LinkedHashSet<>();
                    List<SDTGuard> friends = new ArrayList<>();
                    log.log(Level.FINEST, "curr guards are: " + currChildren.keySet().toString());

                    Map<SDTGuard, SDTGuard> refines = mapGuards(currChildren.keySet(), p);
                    if (refines.isEmpty()) {
                        SDTGuard c = new SDTTrueGuard(new SuffixValue(p.getType(), p.getId()));
//                        SDTGuard c = new SDTCompoundGuard(new SuffixValue(p.getType(),p.getId()), new SDTIfGuard[0]);
                        finest = Collections.singleton(c);
                        friends.add(c);
                    }
                    else {
                        finest = getFinestGuards(refines);
                    }
                    //for (Map.Entry<SDTGuard, SDT> e : currChildren.entrySet()) {
                    //    SDTGuard cGuard = e.getKey();
                    //    Map<SDTGuard,SDTGuard> guardAndFriends = getRefinedVersionOf(cGuard, nxtChildren.keySet());
                    //    System.out.println("guard and friends: " + cGuard.toString() + "    and    " + guardAndFriends.toString());
                    for (SDTGuard guard : finest) {
                        //SDTGuard guard = guardAndFriends.get(0);
                        log.log(Level.FINEST, "!!!! guard is: " + guard.toString());
                        
                        friends.addAll(getAllCoarser(guard, refines));

                        //List<SDTGuard> friends = new ArrayList<>(guardAndFriends.subList(1, guardAndFriends.size()));
                        //log.log(Level.FINEST, "!!!! friends are: " + guardAndFriends.toString());
                        //if (!visited.contains(nextNode)) {
                        log.log(Level.FINEST, "processing guard: " + guard.toString());
                        //log.log(Level.FINEST,"...... wh piv is " + piv.toString());
                        //jpval.putAll(ipval);
                        DataValue dvi = teach.instantiate(prefix, ps, piv, pval, guard, p);
                        log.log(Level.FINEST, dvi.toString() + " maps to " + guard.toString());
                        // try commenting out this
                        //log.log(Level.FINEST,"dvi = " + dvi.toString());
                        ParValuation otherPval = new ParValuation();
                        otherPval.putAll(pval);
                        otherPval.put(p, dvi);

                        SDT[] newSdts = new SDT[friends.size()];
                        for (int x = 0; x < newSdts.length; x++) {
                            newSdts[x] = nxtChildren.get(friends.get(x));
                        }
                        
                        nextMap.put(dvi, createNode(i+1, prefix, ps, piv, otherPval, newSdts));

                        // another ugly hack because yuck
                        //SDTGuard newGuard = new ElseGuard(s);
                        //if (!guardList.isEmpty()) {
                        //    newGuard = guardList.get(0);
                        //}
                        //pval.put(p,dvi);
                        guardMap.put(dvi, guard); }
                    
                    //}
                // comment this back in //}
                log.log(Level.FINEST, "guardMap: " + guardMap.toString());
                log.log(Level.FINEST, "nextMap: " + nextMap.toString());

                return new Node(p, nextMap, guardMap);

            }
   //     } else {
   //         return new Node(new Parameter(null, ps.getArity()));
   // }

    }

    private VarValuation generateVarVal(PIV oldPiv, ParValuation oldPval) {
        // add all parameters in the piv
        VarValuation oldVal = new VarValuation();
        for (Parameter rp : oldPiv.keySet()) {
            for (Parameter pp : oldPval.keySet()) {
                // ugly equality check
                if (rp.getId() == pp.getId()) {
                    log.log(Level.FINEST, rp.toString() + " and " + pp.toString());
                    oldVal.put(oldPiv.get(rp), oldPval.get(pp));
                }
            }
        }
        return oldVal;
    }

    private List<SDTIfGuard> makeGuardList(Set<SDTGuard> guards) {
        List<SDTIfGuard> ifs = new ArrayList<>();
        for (SDTGuard guard : guards) {
            if (guard instanceof SDTCompoundGuard) {
                ifs.addAll(((SDTCompoundGuard) guard).getGuards());
            } else if (guard instanceof SDTIfGuard) {
                ifs.add((SDTIfGuard) guard);
            }
        }
        return ifs;
    }

    private VarValuation updateVarVal(VarValuation oldVal, List<DataValue> oldPrefixValues, MultiTheoryBranching newBranching) {
        VarValuation updatedVal = new VarValuation();
        updatedVal.putAll(oldVal);

        // collect all the guards
        List<SDTIfGuard> guards = makeGuardList(newBranching.getGuards());
        for (SDTIfGuard s : guards) {
            SymbolicDataValue sreg = s.getRegister();
            assert sreg instanceof Register;
            updatedVal.put((Register) sreg, oldPrefixValues.get(sreg.getId() - 1));
        }
        return updatedVal;
    }

    /**
     * This method computes the initial branching for an SDT. It re-uses
     * existing valuations where possible.
     *
     */
    @Override
    public Branching updateBranching(Word<PSymbolInstance> prefix,
            ParameterizedSymbol ps, Branching current,
            PIV piv, SymbolicDecisionTree... sdts) {

        System.out.println("here are the sdts " + Arrays.toString(sdts));

        MultiTheoryBranching oldBranching = (MultiTheoryBranching) current;
        MultiTheoryBranching newBranching = (MultiTheoryBranching) getInitialBranching(prefix, ps, piv, sdts);
        //List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(oldBranching.getPrefix()));

        Map<Word<PSymbolInstance>, TransitionGuard> oldBranches = oldBranching.getBranches();
        Map<Word<PSymbolInstance>, TransitionGuard> newBranches = newBranching.getBranches();

        System.out.println(">>>>Updating old branching: " + oldBranching.toString());
        System.out.println(".... according to new SDT: " + newBranching.toString());

//        assert oldBranches.size() <= newBranches.size();
        if (oldBranches.isEmpty()) {
            if (newBranches.isEmpty()) {
                return oldBranching;
            }
        }

        // what we need for an updated branching: 
        // prefix: CHECK, 
        // ps: CHECK, 
        // piv: NOT YET, 
        // sdts: CHECK
        PIV updatedPiv = new PIV();
        updatedPiv.putAll(oldBranching.getPiv());

        Branching updatedBranching = getInitialBranching(oldBranching.getPrefix(), ps, piv, sdts);

        log.log(Level.FINEST, ".... where the new branches are: ");
        for (Map.Entry<Word<PSymbolInstance>, TransitionGuard> e : newBranches.entrySet()) {
            log.log(Level.FINEST, e.toString());
        }
        // parvaluation : param to dv
        // piv : param to reg
        // need: varvaluation (reg to dv), parvaluation
//        ParValuation oldPval = new ParValuation();
//        PIV oldPiv = new PIV();
//        
//        oldPval.putAll(oldBranching.getPval());
//        oldPiv.putAll(oldBranching.getPiv());
//        VarValuation oldValuation = generateVarVal(oldPiv, oldPval);
//        log.log(Level.FINEST,"old stuff size: " + oldPiv.size() + " " + oldPval.size() + " " + oldBranches.size());
//        log.log(Level.FINEST,"old piv: " + oldPiv.toString() + " old pval: " + oldPval.toString());
//        Map<Word<PSymbolInstance>, TransitionGuard> updated = new LinkedHashMap<>();
//        
        //VarValuation updatedOldVarVal = updateVarVal(oldValuation, prefixValues, newBranching);
        //log.log(Level.FINEST,"updating: " + oldValuation.toString() + " to " + updatedOldVarVal.toString());

//        Boolean[] canUseBranching = new Boolean[newBranches.size()];
//        Boolean canUse = false;
//        int i = 0;
        //log.log(Level.FINEST,"begin comparison");
        // for each guard
//        for (Map.Entry<Word<PSymbolInstance>, TransitionGuard> f : oldBranches.entrySet()) {
//            log.log(Level.FINEST,f);
//            Word<PSymbolInstance> fword = f.getKey();
//            for (Map.Entry<Word<PSymbolInstance>, TransitionGuard> e : newBranches.entrySet()) {
//                log.log(Level.FINEST,e);
//                Word<PSymbolInstance> eword = e.getKey();
//                // if the words are equal (same dvs) we just use the old one
//                if (fword.lastSymbol().equals(eword.lastSymbol())) {
//                    canUseBranching[i] = true;
//                    i++
//                }
//                else{
//                    updatedBranching..put(eword, e.getValue());
//                }
//                // if the words are not equal, 
//                else {
//                    
//                }
//                // if the data values are the same: return the old mapping
//                // otherwise, return a new instantiation
//                TransitionGuard newGuard = e.getValue();
//                log.log(Level.FINEST,"checking if " + newGuard.toString() + " is sat. by piv " + updatedOldVarVal.toString() + " and PVal " + oldPval.toString());
//                if (newGuard.isSatisfied(updatedOldVarVal, oldPval, new Constants())) {
//                    log.log(Level.FINEST,"yes");
//                    canUse = true;
//                }
//            }
//            i++;
//        }
//
//        if (isArrayTrue(canUse)) {
//            return getInitialBranching(prefix, ps, oldPiv, oldPval, sdts);
//        } else {
//            return oldBranching;
//        }
        return updatedBranching;
    }

}
