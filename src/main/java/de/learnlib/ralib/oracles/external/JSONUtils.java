/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.oracles.external;

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTLeaf;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.PSymbolInstance;
import java.util.HashMap;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class JSONUtils {

    public static SDTIfGuard fromJSON(GuardJSON guardJSON, 
            Map<Integer, SymbolicDataValue.SuffixValue> params, 
            Map<Integer, SymbolicDataValue> registers) {
    
        SymbolicDataValue.SuffixValue par = params.get(guardJSON.getParameter());
        SymbolicDataValue reg = registers.get(guardJSON.getRegister());
        
        return guardJSON.isEquality() ? 
                new EqualityGuard(par, reg) : new DisequalityGuard(par, reg);
    }
    
    public static SDTGuard fromJSON(GuardJSON[] guardJSON, 
            Map<Integer, SymbolicDataValue.SuffixValue> params, 
            Map<Integer, SymbolicDataValue> registers) {
        
        assert guardJSON.length <= 1;
        if (guardJSON.length == 0) {
            // FIXME need to repair this ...
            return new SDTTrueGuard(params.get(1));
        }
        return fromJSON(guardJSON[0], params, registers);
    }
    
    public static SDT fromJSON(SdtJSON sdtJSON,
            Map<Integer, SymbolicDataValue.SuffixValue> params, 
            Map<Integer, SymbolicDataValue> registers) {
        
        if (sdtJSON.getChildren().length == 0) {
            return sdtJSON.isAccepting() ? SDTLeaf.ACCEPTING : SDTLeaf.REJECTING;
        }
        
        Map<SDTGuard, SDT> children = new HashMap<>();
        for (GuardedSubTreeJSON gtreeJSON : sdtJSON.getChildren()) {
            SDTGuard g = fromJSON(gtreeJSON.getGuards(), params, registers);
            SDT sdt = fromJSON(gtreeJSON.getTree(), params, registers);
            children.put(g,sdt);
        }
        
        return new SDT(children);
    }
    
    public static ConcreteSymbolJSON[] toJSON(Word<PSymbolInstance> pword) {
        ConcreteSymbolJSON[] ret = new ConcreteSymbolJSON[pword.length()];
        for (int i=0; i<pword.length(); i++) {
            PSymbolInstance psi = pword.getSymbol(i);
            ret[i] = new ConcreteSymbolJSON(
                    psi.getBaseSymbol().getName(), 
                    // FIXME??
                    (Integer) psi.getParameterValues()[0].getId());
        }
        return ret;
    }
    
    public static SymbolicSymbolJSON[] toJSON(SymbolicSuffix symSuffix) {
        
        symSuffix.getActions();
        return null;
    }    
}
