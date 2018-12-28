/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.oracles.external;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.util.HashMap;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class VariableRepository {
    
    private final Map<Integer, SymbolicDataValue.Constant> constRepo = 
            new HashMap<>();
    
    private final Map<Integer, SymbolicDataValue.SuffixValue> parameterRepo =
            new HashMap<>();

    private final Map<Integer, SymbolicDataValue.Register> registerRepo =
            new HashMap<>();
    
    private final SymbolicDataValueGenerator.RegisterGenerator rgen = 
            new SymbolicDataValueGenerator.RegisterGenerator();
    
    private final Map<DataValuePrefixJSON,DataValue> vmap;
    
    private final Map<SDTVariableJSON, DataValuePrefixJSON> piv;

    public VariableRepository(
            Map<DataValuePrefixJSON, DataValue> vmap, 
            Map<SDTVariableJSON, DataValuePrefixJSON> piv,
            Word<ParameterizedSymbol> suffix,
            Constants consts) {
        this.vmap = vmap;
        this.piv = piv;
        
        SymbolicDataValueGenerator.SuffixValueGenerator sgen = 
                new SymbolicDataValueGenerator.SuffixValueGenerator();

        int i=1;
        for (ParameterizedSymbol ps : suffix) {
            for (DataType t : ps.getPtypes()) {
                parameterRepo.put(i++, sgen.next(t));
            }
        }
        
        SymbolicDataValueGenerator.ConstantGenerator cgen = 
                new SymbolicDataValueGenerator.ConstantGenerator();
        
        i=1;
        for (Constant c : consts.keySet()) {
            assert c.getId() == i;
            constRepo.put(i++, cgen.next(c.getType()));
        }        
    }
    
    public SymbolicDataValue.Register getOrCreateRegister(SDTVariableJSON var) {
        assert var.getType().equals(SDTVariableJSON.TYPE_SDT_REGISTER);
        SymbolicDataValue.Register r = registerRepo.get(var.getId());
        if (r == null) {
            r = rgen.next(vmap.get(piv.get(var)).getType());
            registerRepo.put(var.getId(), r);
            assert r.getId() == var.getId();
        }
        assert r!= null;
        return r;
    }

    public SymbolicDataValue.Constant getConstant(SDTVariableJSON var) {
        assert var.getType().equals(SDTVariableJSON.TYPE_CONSTANT);
        SymbolicDataValue.Constant c = constRepo.get(var.getId());
        assert c != null;
        return c;        
    }
    
    public SymbolicDataValue.SuffixValue getParameter(SDTVariableJSON var) {
        assert var.getType().equals(SDTVariableJSON.TYPE_GUARD_PARAM);        
        SymbolicDataValue.SuffixValue p = parameterRepo.get(var.getId());
        assert p != null;
        return p;
    }

    
    public SymbolicDataValue.SuffixValue getParameter(int pos) {
        SymbolicDataValue.SuffixValue p = parameterRepo.get(pos);
        assert p != null;
        return p;
    }    
    
    public PIV computePIV(Word<PSymbolInstance> prefix, PIVElementJSON ... pivJSON) {
        Map<DataValue, SymbolicDataValue.Parameter> pmap = new HashMap<>();
         SymbolicDataValueGenerator.ParameterGenerator pgen = 
                new SymbolicDataValueGenerator.ParameterGenerator();

         PIV retVal = new PIV();
         
        int i=1;
        for (PSymbolInstance ps : prefix) {
            for (DataValue v : ps.getParameterValues()) {
                Parameter p = pgen.next(v.getType());
                if (!pmap.containsKey(v)) {
                    pmap.put(v, p);
                }                
            }
        }
        
        for (PIVElementJSON pe : pivJSON) {            
            SymbolicDataValue.Register r = registerRepo.get(pe.getKey().getId());
            SymbolicDataValue.Parameter p = pmap.get(vmap.get(pe.getValue()));
            retVal.put(p, r);
        }
        
        return retVal;
    }
}
