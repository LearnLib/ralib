/*
 * Copyright (C) 2015 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package de.learnlib.ralib.tools.theories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.sul.ValueMapper;
import de.learnlib.ralib.theory.DataRelation;
import de.learnlib.ralib.theory.inequality.InequalityTheoryWithEq;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;

/**
 *
 * @author falk
 */
public class DoubleInequalityTheory extends InequalityTheoryWithEq<Double> implements TypedTheory<Double> {

    protected static final class Cpr implements Comparator<DataValue<Double>> {

        @Override
        public int compare(DataValue<Double> one, DataValue<Double> other) {
            return one.getId().compareTo(other.getId());
        }
    }

    private final ConstraintSolverFactory fact = new ConstraintSolverFactory();
    private final ConstraintSolver solver = fact.createSolver("z3");

    private DataType<Double> type = null;

    public DoubleInequalityTheory() {
    }

    public DoubleInequalityTheory(DataType<Double> t) {
        this.setType(t);
    }

    @Override
    public List<DataValue<Double>> getPotential(List<DataValue<Double>> dvs) {
        //assume we can just sort the list and get the values
        List<DataValue<Double>> sortedList = new ArrayList<>();
        for (DataValue<Double> d : dvs) {
//                    if (d.getId() instanceof Integer) {
//                        sortedList.add(new DataValue(d.getType(), ((Integer) d.getId()).doubleValue()));
//                    } else if (d.getId() instanceof Double) {
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

    public ValueMapper<Double> getValueMapper() {
    	return new SumCInequalityValueMapper<Double>(this);
    }

    @Override
    public DataValue<Double> getFreshValue(List<DataValue<Double>> vals) {
        if (vals.isEmpty()) {
            return new DataValue<Double>(getType(), 1.0);
        }
        List<DataValue<Double>> potential = getPotential(vals);
        if (potential.isEmpty()) {
            return new DataValue(getType(), 1.0);
        }
        //log.log(Level.FINEST, "smallest index of " + newDv.toString() + " in " + ifValues.toString() + " is " + smallest);
        DataValue<Double> biggestDv = Collections.max(potential, new Cpr());
        return new DataValue(getType(), biggestDv.getId() + 1.0);
    }

    @Override
    public void setUseSuffixOpt(boolean useit) {
        System.err.println("Optimized suffixes are currently not supported for theory "
                + DoubleInequalityTheory.class.getName());
    }

    @Override
    public Collection<DataValue<Double>> getAllNextValues(
            List<DataValue<Double>> vals) {
        Set<DataValue<Double>> nextValues = new LinkedHashSet<>();
        nextValues.addAll(vals);
        List<DataValue<Double>> distinctValList = new ArrayList<>(nextValues);
        
        if (distinctValList .isEmpty()) {
            nextValues.add(new FreshValue<Double>(getType(), 1.0));
        } else {
            Collections.sort(distinctValList , new Cpr());
            if (distinctValList.size() > 1) {
                for (int i = 0; i < (distinctValList.size() - 1); i++) {
                    IntervalDataValue<Double> intVal = IntervalDataValue.instantiateNew(distinctValList.get(i), distinctValList .get(i + 1));
                    nextValues.add(intVal);
                }
            }
            DataValue<Double> min = Collections.min(distinctValList, new Cpr());
            nextValues.add(IntervalDataValue.instantiateNew(null, min));
            DataValue<Double> max = Collections.max(distinctValList, new Cpr());
            nextValues.add(IntervalDataValue.instantiateNew(max, null));
        }
        return nextValues;
    }
	
    @Override
    public List<EnumSet<DataRelation>> getRelations(
            List<DataValue<Double>> left, DataValue<Double> right) {
        
        List<EnumSet<DataRelation>> ret = new ArrayList<>();
        left.stream().forEach((dv) -> {
            final int c = dv.getId().compareTo(right.getId());
            switch (c) {
                case 0:
                    ret.add(EnumSet.of(DataRelation.EQ));
                    break;
                case 1:
                    ret.add(EnumSet.of(DataRelation.GT));
                    break;
                default: 
                    ret.add(EnumSet.of(DataRelation.DEFAULT));
                    break;
            }
        });
        
        return ret;
    }
    
}
