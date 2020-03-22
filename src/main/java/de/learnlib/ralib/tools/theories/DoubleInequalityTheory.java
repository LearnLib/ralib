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

import java.util.Collections;
import java.util.List;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.inequality.ContinuousDomainInequalityMerger;
import de.learnlib.ralib.theory.inequality.InequalityGuardLogic;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;

/**
 *
 * @author falk
 */
public class DoubleInequalityTheory extends NumberInequalityTheory<Double> implements TypedTheory<Double> {

    public DoubleInequalityTheory() {
    	super(new ContinuousDomainInequalityMerger(new InequalityGuardLogic()));
    }

    public DoubleInequalityTheory(DataType<Double> t) {
    	this();
        super.setType(t);
    }
    
    public DataValue<Double> getFreshValue(List<DataValue<Double>> vals) {
		if (vals.isEmpty()) {
			return new DataValue<Double>(super.getType(), 1.0);
		}
		
		DataValue<Double> biggestDv = Collections.max(vals, new Cpr());
		return new DataValue<Double>(biggestDv.getType(), biggestDv.getId() + 1);
	}
    
    public IntervalDataValue<Double> pickIntervalDataValue(DataValue<Double> left, DataValue<Double> right) {
    	double intVal;
    	assert(left != null || right != null);
    	
    	if (left == null) {
    		intVal = right.getId()/2;
    	} else if (right == null ) {
    		intVal = left.getId() + (Math.ceil(left.getId()) + 1.0 - left.getId())/2;
    	} else {
    		intVal = (left.getId() + right.getId()) / 2; 
    	}
    	
    	return new IntervalDataValue<Double>(new DataValue<Double>(super.getType(), intVal), left, right);
	}
}
