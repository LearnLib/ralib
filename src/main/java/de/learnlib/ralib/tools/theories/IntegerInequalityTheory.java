package de.learnlib.ralib.tools.theories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.theory.SDTGuardLogic;
import de.learnlib.ralib.theory.inequality.DiscreteInequalityMerger;
import de.learnlib.ralib.theory.inequality.InequalityGuardLogic;
import de.learnlib.ralib.theory.inequality.InequalityTheoryWithEq;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;

public class IntegerInequalityTheory  extends InequalityTheoryWithEq<Integer> implements TypedTheory<Integer>{
	protected static final class Cpr implements Comparator<DataValue<Integer>> {

        @Override
        public int compare(DataValue<Integer> one, DataValue<Integer> other) {
            return one.getId().compareTo(other.getId());
        }
    }

    public IntegerInequalityTheory() {
    	super(new DiscreteInequalityMerger(new InequalityGuardLogic()));
    }

    public IntegerInequalityTheory(DataType<Integer> t) {
    	this();
    	super.setType(t);
    }
    
    @Override
    public List<DataValue<Integer>> getPotential(List<DataValue<Integer>> dvs) {
        //assume we can just sort the list and get the values
        List<DataValue<Integer>> sortedList = new ArrayList<>();
        for (DataValue<Integer> d : dvs) {
//                    if (d.getId() instanceof Integer) {
//                        sortedList.add(new DataValue(d.getType(), ((Integer) d.getId()).IntegerValue()));
//                    } else if (d.getId() instanceof Integer) {
            sortedList.add(d);
//                    } else {
//                        throw new IllegalStateException("not supposed to happen");
//                    }
        }

        //sortedList.addAll(dvs);
        Collections.sort(sortedList, new Cpr());

        //System.out.println("I'm sorted!  " + sortedList.toString());
        return sortedList;
    }

    @Override
    public DataValue<Integer> getFreshValue(List<DataValue<Integer>> vals) {
        if (vals.isEmpty()) {
            return new DataValue<Integer>(type, 1);
        }
        List<DataValue<Integer>> potential = getPotential(vals);
        if (potential.isEmpty()) {
            return new DataValue<Integer>(type, 1);
        }
        //log.log(Level.FINEST, "smallest index of " + newDv.toString() + " in " + ifValues.toString() + " is " + smallest);
        DataValue<Integer> biggestDv = Collections.max(potential, new Cpr());
        return new DataValue<Integer>(type, biggestDv.getId() + 1);
    }

    

    @Override
    public Collection<DataValue<Integer>> getAllNextValues(
            List<DataValue<Integer>> vals) {
        Set<DataValue<Integer>> nextValues = new LinkedHashSet<>();
        nextValues.addAll(vals);
        List<DataValue<Integer>> distinctValList = new ArrayList<>(nextValues);
        
        if (distinctValList .isEmpty()) {
            nextValues.add(new FreshValue<Integer>(getType(), 1));
        } else {
            Collections.sort(distinctValList , new Cpr());
            if (distinctValList.size() > 1) {
                for (int i = 0; i < (distinctValList.size() - 1); i++) {
                    IntervalDataValue<Integer> intVal = IntervalDataValue.instantiateNew(distinctValList.get(i), distinctValList .get(i + 1));
                    nextValues.add(intVal);
                }
            }
            DataValue<Integer> min = Collections.min(distinctValList, new Cpr());
            nextValues.add(IntervalDataValue.instantiateNew(null, min));
            DataValue<Integer> max = Collections.max(distinctValList, new Cpr());
            nextValues.add(IntervalDataValue.instantiateNew(max, null));
        }
        return nextValues;
    }
    
    public SDTGuardLogic getGuardLogic() {
    	return new InequalityGuardLogic();
    }

}
