/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.json;

import de.learnlib.ralib.oracles.external.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.mto.SDT;
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
    public void testLoadSDTFromJSON1() {
        InputStream is = JSONTest.class.getResourceAsStream("/json/sdt1.json");
        InputStreamReader reader = new InputStreamReader(is);

        RuntimeTypeAdapterFactory<SdtJSON> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
                .of(SdtJSON.class, "type")
                .registerSubtype(SdtInnerNodeJSON.class, SdtJSON.TYPE_INNER)
                .registerSubtype(SdtLeafJSON.class, SdtJSON.TYPE_LEAF);

        Gson gson = new GsonBuilder().registerTypeAdapterFactory(runtimeTypeAdapterFactory).create();

        TreeQueryResultJSON tqrJSON = gson.fromJson(reader, TreeQueryResultJSON.class);
        System.out.println("TQR: " + tqrJSON);

        Word<ParameterizedSymbol> suffix = Word.<ParameterizedSymbol>epsilon();

        VariableRepository repo = new VariableRepository(
                new HashMap<>(), new HashMap<>(), suffix, new Constants());

        SDT sdt = JSONUtils.fromJSON(tqrJSON.getSdt(),repo);
        System.out.println(sdt);
    }


    @Test
    public void testLoadSDTFromJSON2() {
        InputStream is = JSONTest.class.getResourceAsStream("/json/sdt2.json");
        InputStreamReader reader = new InputStreamReader(is);
        
        RuntimeTypeAdapterFactory<SdtJSON> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
                .of(SdtJSON.class, "type")
                .registerSubtype(SdtInnerNodeJSON.class, SdtJSON.TYPE_INNER)
                .registerSubtype(SdtLeafJSON.class, SdtJSON.TYPE_LEAF);

        Gson gson = new GsonBuilder().registerTypeAdapterFactory(runtimeTypeAdapterFactory).create();
        
        TreeQueryResultJSON tqrJSON = gson.fromJson(reader, TreeQueryResultJSON.class);
        System.out.println("TQR: " + tqrJSON);

        DataType tInt = new DataType("int", Integer.class);
        ParameterizedSymbol in = new InputSymbol("IIn", tInt);
        ParameterizedSymbol out = new OutputSymbol("OOut", tInt);
        
        Word<PSymbolInstance> pword = Word.<PSymbolInstance>fromSymbols(
                new PSymbolInstance(in, new DataValue(tInt, 5)),
                new PSymbolInstance(out, new DataValue(tInt, 6))
        );

        Word<PSymbolInstance> psuffix = Word.<PSymbolInstance>fromSymbols(
                new PSymbolInstance(in, new DataValue(tInt, 5)),
                new PSymbolInstance(out, new DataValue(tInt, 6))
        );
        SymbolicSuffix symSuffix = new SymbolicSuffix(pword, psuffix);        
        
        Map<DataValuePrefixJSON,DataValue> vmap = new HashMap<>();
        Map<SDTVariableJSON, DataValuePrefixJSON> piv = new HashMap<>();
        Word<ParameterizedSymbol> suffix = symSuffix.getActions();
        Constants consts = new Constants();
        
        vmap.put(
                new DataValuePrefixJSON(DataValuePrefixJSON.TYPE_CONCRETE, 1), 
                new DataValue(tInt, 5));
        vmap.put(
                new DataValuePrefixJSON(DataValuePrefixJSON.TYPE_CONCRETE, 2), 
                new DataValue(tInt, 6));
        
        piv.put(
                new SDTVariableJSON(SDTVariableJSON.TYPE_SDT_REGISTER, 1), 
                new DataValuePrefixJSON(DataValuePrefixJSON.TYPE_CONCRETE, 2));

        piv.put(
                new SDTVariableJSON(SDTVariableJSON.TYPE_SDT_REGISTER, 2), 
                new DataValuePrefixJSON(DataValuePrefixJSON.TYPE_CONCRETE, 2));
        
        VariableRepository repo = new VariableRepository(vmap, piv, suffix, consts);
        
        SDT sdt = JSONUtils.fromJSON(tqrJSON.getSdt(), repo);
        System.out.println(sdt);
        System.out.println("PIV: " + repo.computePIV(pword, tqrJSON.getPiv()));
    }
/*
    @Test
    public void testLoadSDTFromJSON3() {
        InputStream is = JSONTest.class.getResourceAsStream("/json/sdt3.json");
        InputStreamReader reader = new InputStreamReader(is);
        
        //{"prefix":[{"symbol":"IGet","dataValues":[]}],"suffix":[{"symbol":"ONOK","parameters":[]}]}
        
        RuntimeTypeAdapterFactory<SdtJSON> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
                .of(SdtJSON.class, "type")
                .registerSubtype(SdtInnerNodeJSON.class, SdtJSON.TYPE_INNER)
                .registerSubtype(SdtLeafJSON.class, SdtJSON.TYPE_LEAF);

        Gson gson = new GsonBuilder().registerTypeAdapterFactory(runtimeTypeAdapterFactory).create();
        
        TreeQueryResultJSON tqrJSON = gson.fromJson(reader, TreeQueryResultJSON.class);
        System.out.println("TQR: " + tqrJSON);

        DataType tInt = new DataType("int", Integer.class);
        ParameterizedSymbol in = new InputSymbol("IIn", tInt);
        ParameterizedSymbol out = new OutputSymbol("OOut", tInt);
        
        Word<PSymbolInstance> pword = Word.<PSymbolInstance>fromSymbols(
                new PSymbolInstance(in, new DataValue(tInt, 5)),
                new PSymbolInstance(out, new DataValue(tInt, 6))
        );

        Word<PSymbolInstance> psuffix = Word.<PSymbolInstance>fromSymbols(
                new PSymbolInstance(in, new DataValue(tInt, 5)),
                new PSymbolInstance(out, new DataValue(tInt, 6))
        );
        SymbolicSuffix symSuffix = new SymbolicSuffix(pword, psuffix);        
        
        Map<DataValuePrefixJSON,DataValue> vmap = new HashMap<>();
        Map<SDTVariableJSON, DataValuePrefixJSON> piv = new HashMap<>();
        Word<ParameterizedSymbol> suffix = symSuffix.getActions();
        Constants consts = new Constants();
        
        vmap.put(
                new DataValuePrefixJSON(DataValuePrefixJSON.TYPE_CONCRETE, 1), 
                new DataValue(tInt, 5));
        vmap.put(
                new DataValuePrefixJSON(DataValuePrefixJSON.TYPE_CONCRETE, 2), 
                new DataValue(tInt, 6));
        
        piv.put(
                new SDTVariableJSON(SDTVariableJSON.TYPE_SDT_REGISTER, 1), 
                new DataValuePrefixJSON(DataValuePrefixJSON.TYPE_CONCRETE, 2));

        piv.put(
                new SDTVariableJSON(SDTVariableJSON.TYPE_SDT_REGISTER, 2), 
                new DataValuePrefixJSON(DataValuePrefixJSON.TYPE_CONCRETE, 2));
        
        VariableRepository repo = new VariableRepository(vmap, piv, suffix, consts);
        
        SDT sdt = JSONUtils.fromJSON(tqrJSON.getSdt(), repo);
        System.out.println(sdt);
        System.out.println("PIV: " + repo.computePIV(pword, tqrJSON.getPiv()));
    }
*/
    
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

        ConcreteSymbolJSON[] prefix = JSONUtils.toJSON(pword, new Constants(), new HashMap<>());
        SymbolicSymbolJSON[] suffix = JSONUtils.toJSON(symSuffix);

        TreeQueryJSON tq = new TreeQueryJSON(prefix, suffix);

        Gson gson = new Gson();
        System.out.println(gson.toJson(tq));

    }

}
