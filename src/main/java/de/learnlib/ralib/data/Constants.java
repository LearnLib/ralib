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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.SymbolicDataValue.Constant;

/**
 * Named constants.
 * 
 * @author falk
 */
public class Constants extends Mapping<SymbolicDataValue.Constant, DataValue<?>> {
	public Constants() {
		this(new SumConstants());
	}
	public Constants(SumConstants sumConstants) {
		super();
		this.setSumC(sumConstants);
	}
 
	@SuppressWarnings("unchecked")
	public <T>  Collection<DataValue<T>> getValues(DataType type) {
		Collection<DataValue<T>> collection = (Collection<DataValue<T>>)  
				this.values().stream().filter(c -> c.type.equals(type)).
				map(dv -> (DataValue<T>) dv).collect(Collectors.toList());
		return collection;
	}
	
	public <T> Map<SymbolicDataValue.Constant, DataValue<T>> ofType(DataType type) {
		final LinkedHashMap<SymbolicDataValue.Constant, DataValue<T>> cMap = new LinkedHashMap<>();
		this.entrySet().stream().filter(e -> e.getValue().type.equals(type))
		.forEach(e -> cMap.put(e.getKey(), (DataValue<T>) e.getValue()) );
		return cMap;
	}
	
	public SymbolicDataValue.Constant getConstantWithValue(DataValue<?> dv) {
		Optional<Constant> cst = this.keySet().stream().filter(c -> this.get(c).equals(dv)).findFirst();
		cst.orElse(null);
		return cst.get();
	}
	
	private SumConstants sumConstants = null;
	
	public void setSumC(SumConstants sumConstants) {
		this.sumConstants = sumConstants;
	} 
	
	public SumConstants getSumCs() {
		return sumConstants;
	}
	
	
	public <T> List<DataValue<T>> getSumCs(DataType type) {
		@SuppressWarnings("unchecked")
		List<DataValue<T>> list = (List<DataValue<T>>)  
				sumConstants.values().stream().filter(c -> c.type.equals(type)).
				map(dv -> (DataValue<T>) dv).collect(Collectors.toList());
		return list;
	}
	
	public <T> DataValue<T> getSumC(DataType type, int index) {
		List<DataValue<T>> sumConst = getSumCs(type);
		return sumConst.get(index);
		
	}
}
