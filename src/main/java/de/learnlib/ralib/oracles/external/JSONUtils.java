/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.oracles.external;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
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
    
    public static SymbolicDataValue fromJSON(
            SDTVariableJSON v, VariableRepository repo) {
        switch (v.getType()) {
            case SDTVariableJSON.TYPE_CONSTANT:
                return repo.getConstant(v);
            case SDTVariableJSON.TYPE_GUARD_PARAM:
                return repo.getParameter(v);
            case SDTVariableJSON.TYPE_SDT_REGISTER:
                return repo.getOrCreateRegister(v);
            default:
                throw new IllegalStateException("Illegal type.");
        }
    }
    
    public static SDTIfGuard fromJSON(
            GuardJSON guardJSON, VariableRepository repo) {
        
        String cmp = guardJSON.getComparator();        
        SymbolicDataValue.SuffixValue p = 
                (SuffixValue) fromJSON(guardJSON.getParameter(), repo);
        SymbolicDataValue other = fromJSON(guardJSON.getOther(), repo);
        
        switch (guardJSON.getComparator()) {
            case "==": return new EqualityGuard(p, other);
            case "!=": return new DisequalityGuard(p, other);
            default:
                throw new IllegalStateException("Comparator not supported: " + cmp);
        }
    }
            
    public static SDTGuard fromJSON(GuardJSON[] guardJSON, 
            VariableRepository repo, int pos) {
        
        SuffixValue p = (SuffixValue) repo.getParameter(pos);
        switch (guardJSON.length) {
            case 0:
                return new SDTTrueGuard(p);
            case 1:
                return fromJSON(guardJSON[0], repo);
            default:
                SDTIfGuard[] ifguards = new SDTIfGuard[guardJSON.length];
                for (int i=0; i<guardJSON.length; i++) {
                    ifguards[i] =  fromJSON(guardJSON[i], repo);
                }
                return new SDTAndGuard(p, ifguards);
        }
    }

    public static SDT fromJSON(SdtJSON sdtJSON, VariableRepository repo) {
        return fromJSON(sdtJSON, repo, 1);
    }   
    
    public static SDT fromJSON(SdtJSON sdtJSON, VariableRepository repo, int pos) {
        
        if (sdtJSON instanceof SdtLeafJSON) {
            SdtLeafJSON leaf = (SdtLeafJSON) sdtJSON;
            return leaf.isAccepting() ? SDTLeaf.ACCEPTING : SDTLeaf.REJECTING;
        }
        
        SdtInnerNodeJSON inner = (SdtInnerNodeJSON) sdtJSON;
        
        Map<SDTGuard, SDT> children = new HashMap<>();
        for (GuardedSubTreeJSON gtreeJSON : inner.getChildren()) {
            
            SDTGuard g = fromJSON(gtreeJSON.getGuards(), repo, pos);
            SDT sdt = fromJSON(gtreeJSON.getTree(), repo, pos+1);
            children.put(g,sdt);     
        }
        
        return new SDT(children);
    }
    
    public static ConcreteSymbolJSON[] toJSON(
            Word<PSymbolInstance> pword, Constants consts, 
            Map<DataValuePrefixJSON,DataValue> vmap) {
        
        ConcreteSymbolJSON[] ret = new ConcreteSymbolJSON[pword.length()];
        for (int i=0; i<pword.length(); i++) {
            PSymbolInstance psi = pword.getSymbol(i);
            
            DataValuePrefixJSON[] vals = 
                    new DataValuePrefixJSON[psi.getBaseSymbol().getArity()];
            
            for (int j=0; j<vals.length; j++) {
                vals[j] = toJSON(psi.getParameterValues()[j], consts);
                vmap.put(vals[j], psi.getParameterValues()[j]);
            }
            
            ret[i] = new ConcreteSymbolJSON(psi.getBaseSymbol().getName(), vals);
        }
        return ret;
    }
    
    public static DataValuePrefixJSON toJSON(DataValue dv, Constants consts) {
        int cv = (Integer) dv.getId();
        boolean isConst = consts.containsValue(dv);
        return new DataValuePrefixJSON(
                isConst ? DataValuePrefixJSON.TYPE_CONSTANT 
                        : DataValuePrefixJSON.TYPE_CONCRETE, cv);
    }
        
    public static SymbolicSymbolJSON[] toJSON(SymbolicSuffix symSuffix) {
        Word<ParameterizedSymbol> actions = symSuffix.getActions();
        SymbolicSymbolJSON[] ret = new SymbolicSymbolJSON[symSuffix.getActions().length()];
        
        int sc = 1;
        
        for (int i=0; i<actions.length(); i++) {
            ParameterizedSymbol ps = actions.getSymbol(i);
            
            DataValueSuffixJSON[] vals = new DataValueSuffixJSON[ps.getArity()];
            for (int j=0; j<vals.length; j++) {
                boolean symbolic = symSuffix.getFreeValues().contains(
                        symSuffix.getDataValue(j+1));
                vals[j] = toJSON(symbolic, sc);
                sc++;
            }
            
            // FIXME: what about symbols without parameters
            ret[i] = new SymbolicSymbolJSON(actions.getSymbol(i).getName(), vals);
        }
        return ret;
    }   
    
    public static DataValueSuffixJSON toJSON(boolean symbolic, int id) {
        return new DataValueSuffixJSON(
                symbolic ? DataValueSuffixJSON.TYPE_SYMBOLIC 
                    : DataValueSuffixJSON.TYPE_DONTCARE, id);
    }
    
}
