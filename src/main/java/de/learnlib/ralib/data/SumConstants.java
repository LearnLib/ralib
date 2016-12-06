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

import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;

public class SumConstants extends Mapping<SymbolicDataValue.SumConstant, DataValue<?>>  {
	private static final long serialVersionUID = 1L;

	public SumConstants() {
	super();	
	}
	
	public SumConstants(DataValue<?> ... dvs) {
		this();
		SymbolicDataValueGenerator.SumConstantGenerator cgen = new SymbolicDataValueGenerator.SumConstantGenerator();
		for (DataValue<?> dv : dvs) 
			this.put(cgen.next(dv.getType()), dv);	
	}
}
