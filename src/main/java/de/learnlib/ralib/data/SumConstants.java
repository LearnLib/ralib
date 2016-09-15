/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.SymbolicDataValue.Constant;

/**
 * Contains regular constants plus constants which can are added to registers in 
 * inequality relations.  
 * 
 * @author falk
 */
public class SumConstants extends Constants {
	Mapping<SymbolicDataValue.Constant, DataValue<?>> sumConstants = new Mapping<SymbolicDataValue.Constant, DataValue<?>>();
	
	public void addSumConstants(Mapping<SymbolicDataValue.Constant, DataValue<?>> sumConstants) {
		this.sumConstants.putAll(sumConstants);
	}
	
	public Mapping<SymbolicDataValue.Constant, DataValue<?>> getSumConstants() {
		return this.sumConstants;
	}
	
	public <T> Collection<DataValue<T>> applySumCToPotSet(Collection<DataValue<T>> potSet) {
		Collection<DataValue<T>> potSetAdditions = Collections.emptyList(); 
		Mapping<Constant, DataValue<?>> sumC = this.getSumConstants();
    	sumC.values().stream().map(c -> c).collect(Collectors.toSet());
    	if (!sumC.isEmpty()) {
    		potSetAdditions = potSet.stream().filter(potVal -> !this.containsValue(potVal)).   //. // we filter the actual constants (we don't want sums over constants)
    		flatMap(value -> sumC.values().stream().map(c -> addNumbers(value, c))).collect(Collectors.toList());
    	}
    	
		return potSetAdditions;
	}

	private <T> DataValue<T>  addNumbers(DataValue<T> value, DataValue<?> c) {
		BigInteger sum = new BigInteger(value.id.toString()).add(new BigInteger(c.id.toString()));
		DataValue<T> sumDv = new DataValue<T>(value.getType(), (T) new Integer(sum.intValue()));
		return sumDv;
	}
	
	
}
