/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.equivalence;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author fh
 */
public class EquivalenceTest 
{
    //TODO: port equivalence test
    
//
//    /* **********************************************************************
//     * state pair hash stuff ...
//     */
//    
//    private class Tuple {
//        
//        RALocation sys1loc;
//        RALocation sys2loc;
//        Register sys1reg;
//        Register sys2reg;
//        
//        public Tuple(RALocation l1, RALocation l2, Register r1, Register r2) {
//            sys1loc = l1;
//            sys2loc = l2;
//            sys1reg = RegisterUtil.cloneRegister(r1);
//            sys2reg = RegisterUtil.cloneRegister(r2);
//        }
//        
//        @Override
//        public int hashCode() {
//            int hash = 7;
//            hash = 31 * hash + (this.sys1loc != null ? this.sys1loc.hashCode() : 0);
//            hash = 31 * hash + (this.sys2loc != null ? this.sys2loc.hashCode() : 0);
//            return hash;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (obj == null) {
//                return false;
//            }
//            if (getClass() != obj.getClass()) {
//                return false;
//            }
//            final Tuple other = (Tuple) obj;
//            if (this.sys1loc != other.sys1loc && (this.sys1loc == null || !this.sys1loc.equals(other.sys1loc))) {
//                return false;
//            }
//            if (this.sys2loc != other.sys2loc && (this.sys2loc == null || !this.sys2loc.equals(other.sys2loc))) {
//                return false;
//            }
//            return true;
//        }
//                
//    }
//    
//    static boolean compatible(Tuple t1, Tuple t2, DataValue[] c) 
//    {
//        // compare lcoations ...
//        if (!t1.equals(t2))
//            return false;
//        
////        System.out.println(t1.sys1reg);
////        System.out.println(t1.sys2reg);
////        System.out.println(t2.sys1reg);
////        System.out.println(t2.sys2reg);
//        
//        // compare registers
//        HashMap<Object,Object> vMap = new HashMap<Object, Object>();
//        
//        return compareRegister(t1.sys1reg, t2.sys1reg, vMap, c) &&
//               compareRegister(t1.sys2reg, t2.sys2reg, vMap, c);
//    }
//
//    private static boolean compareRegister(
//            Register r1, Register r2, Map<Object,Object> vMap, DataValue[] c) {
//
//        for (String key : r1.getKeys()) 
//        {
//            DataValue v1 = r1.resolveLocal(new Reference(key)).getValue();
//            DataValue v2 = r2.resolveLocal(new Reference(key)).getValue();
//            
//            boolean n1 = v1.equals(DataValue.NULL);
//            boolean n2 = v2.equals(DataValue.NULL);
//            
//            if (n1 && n2)
//                continue;
//            
//            if (n1 || n2)
//                return false;
//            
//            Object o1 = vMap.get(v1.getValue());
//            if (o1 != null && !o1.equals(v2.getValue())) {
//                return false;
//            }
//
//            if (!v1.equals(v2)) {
//                for (DataValue cv : c)
//                {
//                    if (v1.equals(cv) || v2.equals(cv))
//                        return false;
//                }   
//            }
//            vMap.put( v1.getValue(), v2.getValue());            
//        }
//        
//        return true;
//    }
//    
//    
//    
//    /* **********************************************************************
//     * queue elements
//     */
//    
//    private class Triple {
//        
//        RALocation sys1loc;
//        RALocation sys2loc;
//        Register sys1reg;
//        Register sys2reg;
//        Word as;
//        Word trace;
//        
//        public Triple(RALocation l1, RALocation l2, Register r1, Register r2, Word w, Word t) {
//            sys1loc = l1;
//            sys2loc = l2;
//            sys1reg = RegisterUtil.cloneRegister(r1);
//            sys2reg = RegisterUtil.cloneRegister(r2);
//            as = w;
//            trace = t;
//        }
//                
//    }
//    
//    
//    /* **********************************************************************
//     * main class implementation
//     */
//    
//    private IORAAutomaton sys1;
//    private IORAAutomaton sys2;
//    
//    private ValueGenerator gen1 = null;
//    private ValueGenerator gen2 = null;
//    
//    public EquivalenceTest(IORAAutomaton in1, IORAAutomaton in2, ValueGenerator gen1, ValueGenerator gen2)
//    {
//        this.sys1 = in1;
//        this.sys2 = in2;
//        this.gen1 = gen1;
//        this.gen2 = gen2;
//    }
//    
//    public EquivalenceTest(IORAAutomaton in1, IORAAutomaton in2)
//    {
//        this(in1,in2,null,null);
//    }
//
//    public Word findCounterExample(boolean checkForEqualParameters)
//    {
//        int x=0;
//        
//        DataValue consts[] = new DataValue[sys1.getConstants().size()];
//        int i = 0;
//        for (String key : sys1.getConstants().getKeys()) {
//            consts[i++] = sys1.getConstants().resolveLocal(new Reference(key)).getValue();
//        }
//        
//        LinkedList<Triple> q = new LinkedList<Triple>();        
//        Triple start = new Triple(sys1.getInitial(), sys2.getInitial(), sys1.getRegister(), sys2.getRegister(), new WordImpl(), new WordImpl());
//        q.offer(start);
//        
//        HashMap<Tuple,ArrayList<Tuple>> visited = new HashMap<Tuple, ArrayList<Tuple>>();
//        Tuple st = new Tuple(start.sys1loc, start.sys2loc, start.sys1reg, start.sys2reg);
//        visited.put(st, new ArrayList<Tuple>());
//        visited.get(st).add(st);
//        
//        while (!q.isEmpty()) // && x<200) 
//        {
//            Triple t = q.poll();
//            
//            for (ParameterizedSymbol ps : sys1.getInputs())
//            {
//                List<Word> words = getNext(t.as, ps, t.sys1reg, t.sys2reg,checkForEqualParameters, consts);
//                for (Word w : words)
//                {
//                    //System.out.println(x + "----------------------------------------------------------------------");                    
//                    //System.out.println(w);
//                                        
//                    Triple next = new Triple(null, null, null, null, w, null);
//                    
//                    Pair<PSymbolInstance,PSymbolInstance> out = 
//                            executeStep(t, (PSymbolInstance)WordUtil.lastSymbol(w), next);
//                                        
//                    //System.out.println(w + " = " + out.getFirst() + " : " + out.getSecond());
//                    if (out.getSecond() == null || out.getFirst() == null) {
//                        throw new IllegalStateException();
//                    }
//                    // book keep the trace
//                    next.trace = WordUtil.concat(WordUtil.concat(t.trace, WordUtil.lastSymbol(w)), out.getFirst());
//                    
//                    // found counterexample
//                    if (!out.getFirst().equals(out.getSecond()))
//                        return next.trace;
//                            
//                    // FIXME: this may not be OK in general. I think it is ok 
//                    // for learning ....
//                    if (hasDoubles(next.sys2reg)) {
//                        continue;
//                    }
//                    
//                    st = new Tuple(next.sys1loc, next.sys2loc, next.sys1reg, next.sys2reg);
//                    ArrayList<Tuple> comp = visited.get(st);
//                    
//                    // first one
//                    if (comp == null) 
//                    {
//                        //System.out.println("First one in list");
//                        visited.put(st, new ArrayList<Tuple>());                        
//                    }
//                    else
//                    {
//                        //System.out.println("Found list");
//                        boolean found = false;
//                        for (Tuple xx : comp) {
//                            if (compatible(xx, st, consts))
//                            {
//                                found = true;
//                                //System.out.println("skip " + w);
//                                break;
//                            }
//                        }
//
//                        if (found)
//                            continue;
//                    }
//                    
//                    q.add(next);
//                    visited.get(st).add(st);
//                    //System.out.println("added " + w + " to queue");                    
//                    
//                }
//            }
//            
//            x++;
//            
//        }
//        
//        return null;
//    }
//    
//    private static boolean hasDoubles(Register r) {
//        
//        Set<Object> s = new HashSet<Object>();
//        int x=0;
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
//        
//        return (s.size() != x);
//    }
//    
//    
//    private Pair<PSymbolInstance,PSymbolInstance> executeStep(Triple in, PSymbolInstance psi, Triple out)
//    {
//        PSymbolInstance ret1 = null;
//        PSymbolInstance ret2 = null;
//
//        out.sys1reg = RegisterUtil.cloneRegister(in.sys1reg,sys1.getConstants());
//        out.sys2reg = RegisterUtil.cloneRegister(in.sys2reg,sys2.getConstants());
//        
//        // first sys
//        RALocation loc = null;
//        for (Pair<Transition,RALocation> t : sys1.getAdjacentOut(in.sys1loc))
//        {
//            if ( t.getFirst().isEnabled(psi, out.sys1reg))
//            {
//                loc = t.getSecond();
//                t.getFirst().execute(psi, out.sys1reg);
//                break;
//            }
//        }
//            
//        if (loc == null)
//            throw new IllegalStateException("No transition enabled (sys1):" + in.as + psi);
//
//        // output
//        for (Pair<Transition,RALocation> t : sys1.getAdjacentOut(loc))
//        {
//            OTransition ot = (OTransition)t.getFirst();
//            if ( ot.isEnabledO(out.sys1reg))
//            {
//                Pair<State,PSymbolInstance> xx = ot.executeOutput(out.sys1reg, gen1);
//                ret1 = xx.getSecond();
//                out.sys1loc = (RALocation) xx.getFirst();
//                break;
//            }
//        }
//            
//        if (out.sys1loc == null) {
//            throw new IllegalStateException("No transition enabled (sys1/o): " + in.as + "(" + loc + ")" + psi);
//        }
//        
//        // second sys
//        loc = null;
//        for (Pair<Transition,RALocation> t : sys2.getAdjacentOut(in.sys2loc))
//        {
//            if ( t.getFirst().isEnabled(psi, out.sys2reg))
//            {
//                loc = t.getSecond();
//                t.getFirst().execute(psi, out.sys2reg);
//                break;
//            }
//        }
//            
//        if (loc == null) {                       
//            throw new IllegalStateException("No transition enabled (sys2): " + in.as + " (" + in.sys2loc + ") " + psi);
//        }
//
//        // output
//        for (Pair<Transition,RALocation> t : sys2.getAdjacentOut(loc))
//        {
//            OTransition ot = (OTransition)t.getFirst();
//            if ( ot.isEnabledO(out.sys2reg))
//            {
//                Pair<State,PSymbolInstance> xx = ot.executeOutput(out.sys2reg, gen2);
//                ret2 = xx.getSecond();
//                out.sys2loc = (RALocation) xx.getFirst();
//                break;
//            }
//        }
//            
//        if (out.sys1loc == null) {
//            throw new IllegalStateException("No transition enabled (sys2/o): " + in.as + psi);      
//        }
//        
//        return new Pair<PSymbolInstance, PSymbolInstance>(ret1, ret2);
//    }
//    
//    
//    private List<Word> getNext(Word w, ParameterizedSymbol ps, Register r1, 
//            Register r2, boolean checkForEqualParameters, DataValue[] consts)
//    {
//        int states = Math.max(sys1.getAllStates().size(), sys2.getAllStates().size());
//        if (w.size() > states) {
//            //return new ArrayList<Word>();
//        }
//        
//        List<Word> words = new ArrayList<Word>();
//        
//        SortedSet<Integer> pot = new TreeSet<Integer>();
//        for (String key : r1.getKeys())
//            if (!r1.resolveLocal(new Reference(key)).getValue().equals(DataValue.NULL))
//                pot.add( (Integer) r1.resolveLocal(new Reference(key)).getValue().getValue());
//
////  this is maybe ok during learning        
////        for (String key : r2.getKeys())
////            if (!r2.resolveLocal(new Reference(key)).getValue().equals(DataValue.NULL))
////                pot.add( (Integer) r2.resolveLocal(new Reference(key)).getValue().getValue());
//
//        int max=0;
//        for (Symbol s : w.getSymbolList()) {
//            PSymbolInstance psi = (PSymbolInstance)s;
//            for (Object o : psi.getParameters())
//                max = java.lang.Math.max(max, (Integer) ((DataValue)o).getValue() );
//        }
//        //System.out.println(max);
//        
//        List<Integer> vars = new ArrayList<Integer>();
//        for (int i=0;i<ps.getArity();i++)
//            vars.add(i);
//        
//        // extend potential by new ones
//        int free = max+1;
//        for (int i=0;i<ps.getArity();i++)
//            pot.add(free++);
//        
//        // FIXME: what about constants???
//        for (DataValue d : consts) {
//            pot.add( (Integer) d.getValue());
//        }
//        
//        // generate all possible mappings ...
//        ExtendedNChooseKIterator<Integer,Integer> nk = null;
//
//        // FIXME: checking too many possibilities currently ...
//        
//        if (checkForEqualParameters)
//                nk= new ExtendedNChooseKIterator<Integer,Integer>(pot, vars,true,false);
//        else
//                nk= new ExtendedNChooseKIterator<Integer,Integer>(pot, vars,true,true);
//            
//        //Mapping<Integer,Integer> m = new Mapping<Integer, Integer>();
//        Mapping<Integer,Integer> m = nk.nextInvereseMapping();
//        if (m==null)
//            m = new Mapping<Integer, Integer>();
//        
//        while (m != null)
//        {
//            //int free = max+1;
//            //System.out.println(m);        
//            
//            Object[] pv = new Object[ps.getArity()];
//            for (int i=0;i<ps.getArity();i++)
//                //pv[i] = m.containsKey(i) ? new DataValue(m.get(i)) : new DataValue(free++);
//                pv[i] = new DataValue(m.get(i));
//            
//            PSymbolInstance psi = new PSymbolInstance(ps, pv);
//            Word wa = WordUtil.clone(w);
//            wa.addSymbol(psi);
//            words.add(wa);
//            
//            m = nk.nextInvereseMapping();
//        }
//        
//        return words;
//    }
    
}
