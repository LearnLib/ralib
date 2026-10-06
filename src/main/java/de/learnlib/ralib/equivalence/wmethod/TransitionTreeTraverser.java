package de.learnlib.ralib.equivalence.wmethod;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.ParameterValuation;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.smt.VarsValuationVisitor;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import net.automatalib.word.Word;

public class TransitionTreeTraverser {

    private record State (RALocation loc, Word<PSymbolInstance> prefix, RegisterValuation val) {}

    private final TransitionTree tt;

    private final Map<DataType, Theory> teachers;

    private final Constants consts;

    private final ConstraintSolver solver;

    private Set<Word<PSymbolInstance>> locCover;
    private Set<Word<PSymbolInstance>> transCover;
    private Set<RALocation> traversedLocs;

    public TransitionTreeTraverser(TransitionTree transitionTree, Map<DataType, Theory> teachers, Constants consts, ConstraintSolver solver) {
        this.tt = transitionTree;
        this.teachers = teachers;
        this.consts = consts;
        this.solver = solver;
        this.locCover = new LinkedHashSet<>();
        this.transCover = new LinkedHashSet<>();
        this.traversedLocs = new LinkedHashSet<>();
    }

    public void traverse() {
        reset();
        locCover.add(Word.epsilon());
        transCover.add(Word.epsilon());
        traversedLocs.add(tt.getRaLocation(tt.getRoot()));
        State initial = new State(tt.getRoot(), Word.epsilon(), new RegisterValuation());
        traverse(Set.of(initial));
    }

    public Set<Word<PSymbolInstance>> getLocationCover() {
        return locCover;
    }

    public Set<Word<PSymbolInstance>> getTransitionCover() {
        return transCover;
    }

    public void reset() {
        locCover.clear();
        transCover.clear();
        traversedLocs.clear();
    }

    private void traverse(Set<State> front) {
        Set<State> nextFront = new LinkedHashSet<>();
        for (State state : front) {
            for (Transition t : tt.getTransitons(state.loc)) {
                ParameterizedSymbol ps = t.getLabel();
                RALocation dest = t.getDestination();

                PSymbolInstance psi = instantiate(state, ps, t.getGuard());

                Word<PSymbolInstance> word = state.prefix.append(psi);
                transCover.add(word);
                RALocation loc = tt.getRaLocation(dest);
                if (!traversedLocs.contains(loc)) {
                    locCover.add(word);
                    traversedLocs.add(loc);
                }
                RegisterValuation nextVal = t.valuation(state.val, ParameterValuation.fromPSymbolInstance(psi), consts);
                nextFront.add(new State(dest, word, nextVal));
            }
        }

        if (!nextFront.isEmpty()) {
            traverse(nextFront);
        }
    }

    private PSymbolInstance instantiate(State state, ParameterizedSymbol ps, Expression<Boolean> guard) {
        VarsValuationVisitor vvv = new VarsValuationVisitor();
        Expression<Boolean> g = vvv.apply(guard, state.val());

        List<DataValue> pvals = new ArrayList<>();
        DataValue[] dvals = new DataValue[ps.getArity()];
        for (int i = 0; i < ps.getArity(); i++) {
            Theory theory = teachers.get(ps.getPtypes()[i]);
            assert theory != null : "Undefined theory";
            Optional<DataValue> dvalOpt = theory.instantiate(state.prefix, ps, g, i+1, pvals, consts, solver);
            assert dvalOpt.isPresent() : "Failed to instantiate data value";
            dvals[i] = dvalOpt.get();
            pvals.add(dvals[i]);
        }

        return new PSymbolInstance(ps, dvals);
    }
}
