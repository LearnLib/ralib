package de.learnlib.ralib.dt;


import java.util.*;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 * this is a copy of the functionality of row
 * with additions / modifications for use in the DT
 *
 * @author falk
 */
public class PathResult {

    private final Map<SymbolicSuffix, TreeQueryResult> results;

    private final PIV memorable = new PIV();

    private final SymbolicDataValueGenerator.RegisterGenerator regGen = new SymbolicDataValueGenerator.RegisterGenerator();

    private final boolean ioMode;

    private PathResult(boolean ioMode) {
        this.results = new LinkedHashMap<>();
        this.ioMode = ioMode;
    }

    private void addResult(SymbolicSuffix s, TreeQueryResult tqr) {

        // make sure that pars-in-vars is consistent with
        // existing cells in his row
        PIV cpv = tqr.getPiv();
        VarMapping relabelling = new VarMapping();
        for (Map.Entry<SymbolicDataValue.Parameter, SymbolicDataValue.Register> e : cpv.entrySet()) {
            SymbolicDataValue.Register r = this.memorable.get(e.getKey());
            if (r == null) {
                r = regGen.next(e.getKey().getType());
                memorable.put(e.getKey(), r);
            }
            relabelling.put(e.getValue(), r);
        }

        this.results.put(s, new TreeQueryResult(tqr.getPiv().relabel(relabelling), tqr.getSdt().relabel(relabelling)));
    }

    public PIV getParsInVars() {
        return this.memorable;
    }

    public boolean isEquivalentTo(PathResult other, VarMapping renaming) {
        if (!couldBeEquivalentTo(other)) {
            return false;
        }

        if (!this.memorable.relabel(renaming).equals(other.memorable)) {
            return false;
        }

        for (SymbolicSuffix s : this.results.keySet()) {
            TreeQueryResult c1 = this.results.get(s);
            TreeQueryResult c2 = other.results.get(s);

            if (ioMode) {
                if (c1 == null && c2 == null) {
                    continue;
                }

                if (c1 == null || c2 == null) {
                    return false;
                }
            }

            if (!isEquivalentTo(c1, c2, renaming)) {
                return false;
            }
        }
        return true;
    }

    boolean isEquivalentTo(TreeQueryResult c1, TreeQueryResult c2, VarMapping renaming) {
        if (!couldBeEquivalentTo(c1, c2)) {
            return false;
        }

        return c1.getPiv().relabel(renaming).equals(c2.getPiv()) &&
                c1.getSdt().isEquivalent(c2.getSdt(), renaming);

    }

    boolean couldBeEquivalentTo(PathResult other) {
        if (!this.memorable.typedSize().equals(other.memorable.typedSize())) {
            return false;
        }

        for (SymbolicSuffix s : this.results.keySet()) {
            TreeQueryResult c1 = this.results.get(s);
            TreeQueryResult c2 = other.results.get(s);

            if (ioMode) {
                if (c1 == null && c2 == null) {
                    continue;
                }

                if (c1 == null || c2 == null) {
                    return false;
                }
            }

            if (!couldBeEquivalentTo(c1, c2)) {
                return false;
            }
        }
        return true;
    }

    private boolean couldBeEquivalentTo(TreeQueryResult c1, TreeQueryResult c2) {
        return c1.getPiv().typedSize().equals(c2.getPiv().typedSize());
    }

    /**
     * computes a new row object from a prefix and a set of symbolic suffixes.
     *
     * @param oracle
     * @param prefix
     * @param suffixes
     * @return
     */
    public static PathResult computePathResult(TreeOracle oracle,
                                 MappedPrefix prefix, List<SymbolicSuffix> suffixes, boolean ioMode) {

        PathResult r = new PathResult(ioMode);
        for (SymbolicSuffix s : suffixes) {
            //todo: potential for optimization
            if (ioMode && s.getActions().length() > 0) {
                // error row
                if (prefix.getPrefix().length() > 0 && !r.isAccepting()) {
                    //log.log(Level.INFO, "Not adding suffix " + s + " to error row " + r.getPrefix());
                    continue;
                }
                // unmatching suffix
                if ((prefix.getPrefix().length() < 1 && (s.getActions().firstSymbol() instanceof OutputSymbol))
                        || (prefix.getPrefix().length() > 0 && !(prefix.getPrefix().lastSymbol().getBaseSymbol() instanceof InputSymbol
                        ^ s.getActions().firstSymbol() instanceof InputSymbol))) {
                    //log.log(Level.INFO, "Not adding suffix " + s + " to unmatching row " + r.getPrefix());
                    continue;
                }
            }
            TreeQueryResult tqr = prefix.getTQRs().get(s);
            if (tqr == null) {
                tqr = oracle.treeQuery(prefix.getPrefix(), s);
            }
            //System.out.println("TQ: " + prefix + " : " + s + " : " + tqr);
            r.addResult(s, tqr);
        }
        return r;
    }

    public boolean isAccepting() {
        TreeQueryResult c = this.results.get(RaStar.EMPTY_SUFFIX);
        return c.getSdt().isAccepting();
    }

    public TreeQueryResult getTQRforSuffix(SymbolicSuffix suffix) {
        return results.get(suffix);
    }

    public PathResult copy() {
        PathResult r = new PathResult(ioMode);
        for (Map.Entry<SymbolicSuffix, TreeQueryResult> e : this.results.entrySet()) {
            r.results.put(e.getKey(), new TreeQueryResult(e.getValue().getPiv(), e.getValue().getSdt().copy()));
        }
        r.memorable.putAll(this.memorable);
        return  r;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<SymbolicSuffix, TreeQueryResult> e : this.results.entrySet()) {
            sb.append("[").append(e.getKey()).append("->").append(e.getValue().getSdt().toString().replaceAll("\\s+", " ")).append("] ");
        }
        return sb.toString();
    }
}
