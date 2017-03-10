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

import java.util.ArrayList;
import java.util.List;

import de.learnlib.ralib.theory.SDTGuard;

/**
 *
 * @author falk
 */
//TODO: check is necessary
public class SuffixValuation extends Mapping<SymbolicDataValue.SuffixValue, DataValue<?>> {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public List<SDTGuard> suffGuards = new ArrayList<>();
    
    public SuffixValuation() {
    	super();
    }
    
    public SuffixValuation(SuffixValuation suff) {
    	this();
    	this.putAll(suff);
    }
}
