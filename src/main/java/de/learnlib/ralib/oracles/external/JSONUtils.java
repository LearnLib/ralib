/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.oracles.external;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTLeaf;
import de.learnlib.ralib.theory.SDTAndGuard;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.HashMap;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class JSONUtils {

    public static SDTIfGuard fromJSON(GuardJSON guardJSON, 
            Map<Integer, SymbolicDataValue> vmap,
            int pos, PIV piv, SymbolicDataValueGenerator.RegisterGenerator rgen) {
    
        //assert pos == guardJSON.getParameter();
        SymbolicDataValue p = vmap.get(guardJSON.getRegister());
        SymbolicDataValue r;
        if (p instanceof Parameter) {        
            r = piv.get( (Parameter)p);
            if (r == null) {
                r = rgen.next(p.getType());
                piv.put( (Parameter)p, (Register)r );
            }
        }
        else {
            r = p;
        }
        
        SymbolicDataValue.SuffixValue sv = (SymbolicDataValue.SuffixValue) vmap.get(pos);
        
        return guardJSON.isEquality() ? 
                new EqualityGuard(sv, r) : new DisequalityGuard(sv, r);
    }
    
    public static SDTGuard fromJSON(GuardJSON[] guardJSON, 
            Map<Integer, SymbolicDataValue> vmap, 
            int pos, PIV piv, SymbolicDataValueGenerator.RegisterGenerator rgen) {
        
        SuffixValue sv = (SuffixValue) vmap.get(pos);
        switch (guardJSON.length) {
            case 0:
                return new SDTTrueGuard(sv);
            case 1:
                return fromJSON(guardJSON[0], vmap, pos, piv, rgen);
            default:
                SDTIfGuard[] ifguards = new SDTIfGuard[guardJSON.length];
                for (int i=0; i<guardJSON.length; i++) {
                    ifguards[i] =  fromJSON(guardJSON[i], vmap, pos, piv, rgen);
                }
                return new SDTAndGuard(sv, ifguards);
        }
    }
    
    public static SDT fromJSON(SdtJSON sdtJSON,
            Map<Integer, SymbolicDataValue> vmap, 
            int pos, PIV piv, SymbolicDataValueGenerator.RegisterGenerator rgen) {
        
        if (sdtJSON.getChildren().length == 0) {
            return sdtJSON.isAccepting() ? SDTLeaf.ACCEPTING : SDTLeaf.REJECTING;
        }
        
        if (sdtJSON.getChildren().length == 1 && 
                sdtJSON.getChildren()[0].getSuffix().getParameters().length == 0) {
            
            return fromJSON(sdtJSON.getChildren()[0].getTree(), vmap, pos, piv, rgen);
        }
        
        Map<SDTGuard, SDT> children = new HashMap<>();
        for (GuardedSubTreeJSON gtreeJSON : sdtJSON.getChildren()) {
            
            SDTGuard g = fromJSON(gtreeJSON.getGuards(), vmap, pos, piv, rgen);
            SDT sdt = fromJSON(gtreeJSON.getTree(), vmap, pos+1, piv, rgen);
            children.put(g,sdt);
        
        }
        
        return new SDT(children);
    }
    
    public static ConcreteSymbolJSON[] toJSON(Word<PSymbolInstance> pword) {
        ConcreteSymbolJSON[] ret = new ConcreteSymbolJSON[pword.length()];
        for (int i=0; i<pword.length(); i++) {
            PSymbolInstance psi = pword.getSymbol(i);
            
            int[] vals = new int[psi.getBaseSymbol().getArity()];
            for (int j=0; j<vals.length; j++) {
                vals[j] = (Integer) psi.getParameterValues()[j].getId();
            }
            
            ret[i] = new ConcreteSymbolJSON(psi.getBaseSymbol().getName(), vals);
        }
        return ret;
    }
    
    public static SymbolicSymbolJSON[] toJSON(SymbolicSuffix symSuffix) {
        Word<ParameterizedSymbol> actions = symSuffix.getActions();
        SymbolicSymbolJSON[] ret = new SymbolicSymbolJSON[symSuffix.getActions().length()];
        
        int sc = 1;
        
        for (int i=0; i<actions.length(); i++) {
            ParameterizedSymbol ps = actions.getSymbol(i);
            
            int[] vals = new int[ps.getArity()];
            for (int j=0; j<vals.length; j++) {
                vals[j] = sc++;
            }
            
            // FIXME: what about symbols without parameters
            ret[i] = new SymbolicSymbolJSON(actions.getSymbol(i).getName(), vals);
        }
        return ret;
    }    
}
