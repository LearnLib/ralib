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
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.learning.SymbolicSuffix;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
    public SDT treeQuery(
            Word<PSymbolInstance> prefix, SymbolicSuffix suffix,
            WordValuation values, PIV pir,
            Constants constants,
            SuffixValuation suffixValues) {

        System.out.println("prefix = " + prefix);
        System.out.println("suffix = " + suffix);
        System.out.println("values = " + values);
        System.out.println("pir = " + pir);
        System.out.println("constants = " + constants);
        System.out.println("suffixValues = " + suffixValues);

        // special case: empty word
        if (prefix.isEmpty() && suffix.getActions().isEmpty()) {
            System.out.println("Special Case: empty query is accepted by default.");
            return SDTLeaf.ACCEPTING;
        }

        // special case: invalid sequence of inputs / outputs
        if (invalidIO(prefix, suffix)) {
            System.out.println("Special Case: invalid sequence of i/o is rejected by default.");
            return SDTLeaf.REJECTING;
        }

        writeQuery(prefix, suffix);
        executeCmd();
        return readSDT();
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

    }

    private SDT readSDT() {

        try (FileReader fr = new FileReader(sfile)) {
            Gson gson = new Gson();
            TreeQueryResultJSON tqr = gson.fromJson(fr, TreeQueryResultJSON.class);
            fr.close();

            return JSONUtils.fromJSON(tqr.getSdt(), null, null);

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
