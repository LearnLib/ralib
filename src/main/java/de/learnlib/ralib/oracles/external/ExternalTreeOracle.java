/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.oracles.external;

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
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTLeaf;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class ExternalTreeOracle extends MultiTheoryTreeOracle {

    private final String cmd;

    private final String qfile;

    private final String sfile;
    
    private long tqTest = 0;
    
    private long tqLearn = 0;
    
    private boolean isLearning = true;

    private final Constants constants;
    
    private final Map<DataValuePrefixJSON,DataValue> vmapPrefix = new HashMap<>();

    public ExternalTreeOracle(Map<DataType, Theory> teachers, Constants constants, ConstraintSolver solver,
            String cmd, String qfile, String sfile) {

        super(null, teachers, constants, solver);
        this.cmd = cmd;
        this.qfile = qfile;
        this.sfile = sfile;
        this.constants = constants;
    }

    @Override
    public TreeQueryResult treeQuery(
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {

        if (isLearning) {
            tqLearn++;
        } else {
            tqTest++;
        }
            

        System.out.println("prefix = " + prefix);
        System.out.println("suffix = " + suffix);

        // special case: empty word
        if (prefix.isEmpty() && suffix.getActions().isEmpty()) {
            System.out.println("Special Case: empty query is accepted by default.");
            return new TreeQueryResult(new PIV(), SDTLeaf.ACCEPTING);
        }

        // special case: invalid sequence of inputs / outputs
        if (invalidIO(prefix, suffix)) {
            System.out.println("Special Case: invalid sequence of i/o is rejected by default.");
            return new TreeQueryResult(new PIV(), SDTLeaf.REJECTING);
        }

        writeQuery(prefix, suffix);
        executeCmd();
        TreeQueryResult tqr = readSDT(prefix, suffix.getActions());
        System.out.println("PIV = " + tqr.getPiv());
        System.out.println("SDT = " + tqr.getSdt());        
        return tqr;
    }

    private void writeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {

        vmapPrefix.clear();
        ConcreteSymbolJSON[] pj = JSONUtils.toJSON(prefix, constants, vmapPrefix);
        SymbolicSymbolJSON[] sj = JSONUtils.toJSON(suffix);
        TreeQueryJSON tqjson = new TreeQueryJSON(pj, sj);
        System.out.println("TQ: " +  tqjson);
        try (FileWriter fw = new FileWriter(qfile)) {
            Gson gson = new Gson();
            gson.toJson(tqjson, fw);

            fw.flush();
        } catch (IOException ex) {
            throw new RuntimeException("Could not write query file");
        }
    }

    private void executeCmd() {
        try {
            String cmdString = cmd + " " + qfile + " " + sfile;
            Process p = Runtime.getRuntime().exec(cmdString);
            p.waitFor();
            if(p.exitValue() != 0){
                System.err.println("Error in \n" + cmdString + "\n exit code " + p.exitValue());
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        System.out.println(line);
                    }
                }
                System.exit(1);
            }
        } catch (IOException e) {
            throw new RuntimeException("could not start python oracle");
        } catch (InterruptedException e) {
            throw  new RuntimeException("treeOracle interrupted ");
        }
    }

    private TreeQueryResult readSDT(
            Word<PSymbolInstance> prefix, 
            Word<ParameterizedSymbol> suffix) {
        
        try (FileReader fr = new FileReader(sfile)) {
            RuntimeTypeAdapterFactory<SdtJSON> runtimeTypeAdapterFactory = RuntimeTypeAdapterFactory
                .of(SdtJSON.class, "type")
                .registerSubtype(SdtInnerNodeJSON.class, SdtJSON.TYPE_INNER)
                .registerSubtype(SdtLeafJSON.class, SdtJSON.TYPE_LEAF);

            Gson gson = new GsonBuilder().registerTypeAdapterFactory(runtimeTypeAdapterFactory).create();

            TreeQueryResultJSON tqr = gson.fromJson(fr, TreeQueryResultJSON.class);
            fr.close();
            System.out.println("TQR: " +  tqr);          
                        
            VariableRepository repo = new VariableRepository(
                    vmapPrefix, tqr.getPivasMap(), suffix, constants);
            
            SDT sdt = JSONUtils.fromJSON(tqr.getSdt(), repo);
            PIV piv = repo.computePIV(prefix, tqr.getPiv());
            return new TreeQueryResult(piv, sdt);
            
        } catch (IOException ex) {
            throw new RuntimeException("Could not read sdt file");
        }
    }

    private boolean invalidIO(Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {
        Word<ParameterizedSymbol> query = DataWords.actsOf(prefix).concat(suffix.getActions());
        boolean input = true;
        for (ParameterizedSymbol p : query) {
            if ((input && (p instanceof OutputSymbol))
                    || (!input && (p instanceof InputSymbol))) {

                return true;
            }
            input = !input;
        }
        return false;
    }

    public long getTqLearn() {
        return tqLearn;
    }

    public long getTqTest() {
        return tqTest;
    }


    public void setIsLearning(boolean isLearning) {
        this.isLearning = isLearning;
    }
    
}
