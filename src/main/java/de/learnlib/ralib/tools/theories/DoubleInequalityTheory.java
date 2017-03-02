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

import java.util.Comparator;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.inequality.ConcreteInequalityMerger;
import de.learnlib.ralib.theory.inequality.InequalityGuardLogic;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;

/**
 *
 * @author falk
 */
public class DoubleInequalityTheory extends NumberInequalityTheory<Double> implements TypedTheory<Double> {

    protected static final class Cpr implements Comparator<DataValue<Double>> {

        @Override
        public int compare(DataValue<Double> one, DataValue<Double> other) {
            return one.getId().compareTo(other.getId());
        }
    }

    public DoubleInequalityTheory() {
    	super(new ConcreteInequalityMerger(new InequalityGuardLogic()));
    }

    public DoubleInequalityTheory(DataType<Double> t) {
    	this();
        super.setType(t);
    }
}
