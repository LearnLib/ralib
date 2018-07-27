/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.json;

import de.learnlib.ralib.oracles.external.*;
import com.google.gson.Gson;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTLeaf;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTIfGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import net.automatalib.words.Word;
import org.testng.annotations.Test;

/**
 *
 * @author falk
 */
public class JSONTest {
    

    
    @Test
    public void testLoadSDTFromJSON() {
        InputStream is = JSONTest.class.getResourceAsStream("/json/sdt2.json");
        InputStreamReader reader = new InputStreamReader(is);
        
        Gson gson = new Gson();
        TreeQueryResultJSON tqrJSON = gson.fromJson(reader, TreeQueryResultJSON.class);
        System.out.println("TQR: " + tqrJSON);
        
        Map<Integer, Parameter> params = new HashMap<>();
        Map<Integer, SuffixValue> svs = new HashMap<>();
        
        SymbolicDataValueGenerator.ParameterGenerator pgen = 
                new SymbolicDataValueGenerator.ParameterGenerator();
        
        SymbolicDataValueGenerator.SuffixValueGenerator sgen = 
                new SymbolicDataValueGenerator.SuffixValueGenerator();
        
        SymbolicDataValueGenerator.RegisterGenerator rgen = 
                new SymbolicDataValueGenerator.RegisterGenerator();
        
        DataType tInt = new DataType("int", Integer.class);
        
        params.put(1, pgen.next(tInt));
        params.put(2, pgen.next(tInt));
        
        svs.put(1, sgen.next(tInt));
        svs.put(2, sgen.next(tInt));
        
        PIV piv = new PIV();
        SDT sdt = JSONUtils.fromJSON(tqrJSON.getSdt(), params, svs, 1, piv, rgen);
        System.out.println(sdt);
    }
    
    
    @Test
    public void testQueryToJSON() {
        DataType tInt = new DataType("int", Integer.class);        
        ParameterizedSymbol in = new InputSymbol("IIn", tInt);
        ParameterizedSymbol out = new OutputSymbol("OOut", tInt);
        
        Word<PSymbolInstance> pword = Word.<PSymbolInstance>fromSymbols(
                new PSymbolInstance(in, new DataValue(tInt, 5)),
                new PSymbolInstance(out, new DataValue(tInt, 5))
        );
        
        Word<PSymbolInstance> psuffix = Word.<PSymbolInstance>fromSymbols(
                new PSymbolInstance(in, new DataValue(tInt, 5)),
                new PSymbolInstance(out, new DataValue(tInt, 5))
        );
        SymbolicSuffix symSuffix = new SymbolicSuffix(pword, psuffix);
        
        ConcreteSymbolJSON[] prefix = JSONUtils.toJSON(pword);        
        SymbolicSymbolJSON[] suffix = JSONUtils.toJSON(symSuffix);
        
        TreeQueryJSON tq = new TreeQueryJSON(prefix, suffix);
        
        Gson gson = new Gson();
        System.out.println(gson.toJson(tq));
        
    }


}
