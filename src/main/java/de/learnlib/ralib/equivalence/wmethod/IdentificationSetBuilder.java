package de.learnlib.ralib.equivalence.wmethod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RARun;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.RegisterValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.QuantifiedSDV;
import de.learnlib.ralib.data.SymbolicDataVariable;
import de.learnlib.ralib.equivalence.wmethod.partref.DSymbolInstance;
import de.learnlib.ralib.equivalence.wmethod.partref.Partition;
import de.learnlib.ralib.equivalence.wmethod.partref.PartitionRefinement;
import de.learnlib.ralib.equivalence.wmethod.partref.automata.RaModel;
import de.learnlib.ralib.equivalence.wmethod.partref.constraints.Constraint;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.smt.VarsValuationVisitor;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.Word;

public class IdentificationSetBuilder {

    private final PartitionRefinement partref;

    private final RaModel model;

    private final Constants consts;

    private final ConstraintSolver solver;

    private final boolean ioMode;

    private Partition partition;

    public IdentificationSetBuilder(RegisterAutomaton model, Constants consts, ConstraintSolver solver, boolean ioMode, ParameterizedSymbol ... inputs) {
        this.consts = consts;
        this.solver = solver;
        this.ioMode = ioMode;
        ParameterizedSymbol[] inacts;
        if (ioMode) {
            inacts = inputs;
        } else {
            inacts = new ParameterizedSymbol[inputs.length + 1];
            System.arraycopy(inputs, 0, inacts, 0, inputs.length);
            inacts[inputs.length] = RaModel.EMPTY_INPUT;
        }
        this.model = new RaModel(model, ioMode);
        partref = new PartitionRefinement(this.model, ioMode, inacts);
        while (partref.hasNext()) {
            partition = partref.next();
        }
    }

    public List<Word<PSymbolInstance>> getIdentifyingSuffixes(Word<PSymbolInstance> prefix) {
        Word<PSymbolInstance> prefixOutputs = SequenceBuilder.getOutputSequence(prefix, model, consts);
        Word<PSymbolInstance> prefixWord = ioMode ? SequenceBuilder.getDataWord(prefix, prefixOutputs) : prefix;
        RARun run = model.getRun(prefixWord, consts);
        RALocation loc = run.getLocation(prefixWord.length());
        RegisterValuation regs = run.getValuation(prefixWord.length());

        Constraint constr = partition.getConstraint(loc);
        Expression<Boolean> expr = toExpression(constr, prefix);
        VarsValuationVisitor vvv = new VarsValuationVisitor();
        Expression<Boolean> evaluated = vvv.apply(expr, regs);

        Optional<Valuation> valuation = solver.solve(evaluated);
        assert valuation.isPresent() : "No valuation found";

        Map<SymbolicDataVariable, DataValue> valMapping = new LinkedHashMap<>();
        for (SymbolicDataValue var : constr.getVariables()) {
            if (var instanceof SymbolicDataVariable sdv) {
                BigDecimal d = valuation.get().getValue(var);
                assert d != null;
                valMapping.put(sdv, new DataValue(var.getDataType(), d));
            }
        }

        List<Word<PSymbolInstance>> suffixes = new ArrayList<>();
        List<Word<DSymbolInstance>> symbolicSuffixes = partref.getSuffixes(loc, partition);
        for (Word<DSymbolInstance> symbolicSuffix : symbolicSuffixes) {
            Set<SymbolicDataVariable> vars = DSymbolInstance.valSet(symbolicSuffix);
            for (SymbolicDataVariable var : vars) {
                if (!valMapping.containsKey(var)) {
                    valMapping.put(var, fresh(valMapping.values(), var.getDataType()));
                }
            }
            Word<PSymbolInstance> suffix = DSymbolInstance.toDataWord(symbolicSuffix, valMapping);
            suffixes.add(RaModel.removeEpsilon(suffix));
        }

        if (suffixes.isEmpty()) {
            suffixes.add(Word.epsilon());
        }
        return suffixes;
    }

    private Expression<Boolean> toExpression(Constraint constr, Word<PSymbolInstance> prefix) {
        Expression<Boolean> expr = constr.toExpression();
        for (Map.Entry<SymbolicDataValue, SymbolicDataValue> e : constr.getEqualities()) {
            if (e.getKey() instanceof QuantifiedSDV qsdv) {
                expr = addEqualities(expr, qsdv, prefix);
            }
        }
        return expr;
    }

    private Expression<Boolean> addEqualities(Expression<Boolean> expr, QuantifiedSDV qsdv, Word<PSymbolInstance> prefix) {
        Set<DataValue> prefixVals = DataWords.valSet(prefix);
        Expression[] disjuncts = new Expression[prefixVals.size()];
        int i = 0;
        for (DataValue d : prefixVals) {
            disjuncts[i++] = new NumericBooleanExpression(qsdv, NumericComparator.EQ, d);
        }
        Expression<Boolean> disjunction = ExpressionUtil.or(disjuncts);
        return ExpressionUtil.and(expr, disjunction);
    }

    private DataValue fresh(Collection<DataValue> vals, DataType t) {
        DataValue max = null;
        for (DataValue val : vals) {
            if (val.getDataType().equals(t)) {
                if (max == null || val.compareTo(max) > 0) {
                    max = val;
                }
            }
        }
        if (max == null) {
            return new DataValue(t, BigDecimal.ZERO);
        }
        return new DataValue(t, max.getValue().add(BigDecimal.ONE));
    }
}
