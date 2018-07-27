/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.oracles.external;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SuffixValuation;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import net.automatalib.words.Word;

/**
 *
 * @author falk
 */
public class ExternalTreeOracle extends MultiTheoryTreeOracle {

    private final String cmd;

    private final String qfile;

    private final String sfile;

    public ExternalTreeOracle(Map<DataType, Theory> teachers, Constants constants, ConstraintSolver solver,
            String cmd, String qfile, String sfile) {

        super(null, teachers, constants, solver);
        this.cmd = cmd;
        this.qfile = qfile;
        this.sfile = sfile;
    }

    @Override
    public TreeQueryResult treeQuery(
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {


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
        return readSDT(DataWords.actsOf(prefix), suffix.getActions());
    }

    private void writeQuery(Word<PSymbolInstance> prefix, SymbolicSuffix suffix) {

        ConcreteSymbolJSON[] pj = JSONUtils.toJSON(prefix);
        SymbolicSymbolJSON[] sj = JSONUtils.toJSON(suffix);
        TreeQueryJSON tqjson = new TreeQueryJSON(pj, sj);

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
            Word<ParameterizedSymbol> prefix, 
            Word<ParameterizedSymbol> suffix) {
        
        Map<Integer, Parameter> pmap = new HashMap<>();
        SymbolicDataValueGenerator.ParameterGenerator pgen = 
                new SymbolicDataValueGenerator.ParameterGenerator();
        int i=1;
        for (ParameterizedSymbol ps : prefix) {
            for (DataType t : ps.getPtypes()) {
                pmap.put(i++, pgen.next(t));
            }
        }

        Map<Integer, SuffixValue> smap = new HashMap<>();
        SymbolicDataValueGenerator.SuffixValueGenerator sgen = 
                new SymbolicDataValueGenerator.SuffixValueGenerator();
        i=1;
        for (ParameterizedSymbol ps : suffix) {
            for (DataType t : ps.getPtypes()) {
                smap.put(i++, sgen.next(t));
            }
        }
        
        try (FileReader fr = new FileReader(sfile)) {
            Gson gson = new Gson();
            TreeQueryResultJSON tqr = gson.fromJson(fr, TreeQueryResultJSON.class);
            fr.close();
            
            PIV piv = new PIV();            
            SymbolicDataValueGenerator.RegisterGenerator rgen = 
                    new SymbolicDataValueGenerator.RegisterGenerator();
            
            SDT sdt = JSONUtils.fromJSON(tqr.getSdt(), pmap, smap, 1, piv, rgen);

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
}
