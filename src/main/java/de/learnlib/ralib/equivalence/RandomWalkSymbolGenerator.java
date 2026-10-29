package de.learnlib.ralib.equivalence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class RandomWalkSymbolGenerator {

    private final Map<DataType, Theory> teachers;

    private final Constants constants;

    private final Random rand;

    private final ParameterizedSymbol[] inputs;

    private final double newDataProbability;

    private final boolean uniform;

    public RandomWalkSymbolGenerator(Random rand,
            boolean uniform,
            double newDataProbability,
            Constants constants,
            Map<DataType, Theory> teachers,
            ParameterizedSymbol ... inputs) {
        this.rand = rand;
        this.uniform = uniform;
        this.newDataProbability = newDataProbability;
        this.constants = constants;
        this.teachers = teachers;
        this.inputs = inputs;
    }

    public PSymbolInstance nextInput(Word<PSymbolInstance> run) {
        ParameterizedSymbol ps = nextSymbol();
        PSymbolInstance psi = nextDataValues(run, ps);
        return psi;
    }

    private PSymbolInstance nextDataValues(
            Word<PSymbolInstance> run, ParameterizedSymbol ps) {

        DataValue[] vals = new DataValue[ps.getArity()];

        int i = 0;
        for (DataType t : ps.getPtypes()) {
            Theory teacher = teachers.get(t);
            // TODO: generics hack?
            Set<DataValue> oldSet = DataWords.valSet(run, t);
            for (int j = 0; j < i; j++) {
                if (vals[j].getDataType().equals(t)) {
                    oldSet.add(vals[j]);
                }
            }
            ArrayList<DataValue> old = new ArrayList<>(oldSet);

            Set<DataValue> newSet = new HashSet<>(
                teacher.getAllNextValues(old));

            // TODO: add constants in teacher?
            newSet.addAll(constants.values(t));

            newSet.removeAll(old);
            ArrayList<DataValue> newList = new ArrayList<>(newSet);

            double draw = rand.nextDouble();
            if (draw <= newDataProbability || old.isEmpty()) {
                int idx = rand.nextInt(newList.size());
                vals[i] = newList.get(idx);
            } else {
                int idx = rand.nextInt(old.size());
                vals[i] = old.get(idx);
            }

            i++;
        }
        return new PSymbolInstance(ps, vals);
    }

    private ParameterizedSymbol nextSymbol() {
        ParameterizedSymbol ps = null;
        Map<DataType, Integer> tCount = new LinkedHashMap<>();
        if (uniform) {
            ps = inputs[rand.nextInt(inputs.length)];
        } else {
            int MAX_WEIGHT = 0;
            int[] weights = new int[inputs.length];
            for (int i = 0; i < weights.length; i++) {
                weights[i] = 1;
                for (DataType t : inputs[i].getPtypes()) {
                    Integer old = tCount.get(t);
                    if (old == null) {
                        // TODO: what about constants?
                        old = 0;
                    }
                    weights[i] *= (old + 1);
                    tCount.put(t, ++old);
                }
                MAX_WEIGHT += weights[i];
            }

            int idx = rand.nextInt(MAX_WEIGHT) + 1;
            int sum = 0;
            for (int i = 0; i < inputs.length; i++) {
                sum += weights[i];
                if (idx <= sum) {
                    ps = inputs[i];
                    break;
                }
            }
        }
        return ps;
    }

}
