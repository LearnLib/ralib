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
package de.learnlib.ralib.tools;

import de.learnlib.ralib.tools.classanalyzer.ClassAnalyzerParser;
import de.learnlib.ralib.tools.config.ConfigurationException;

/**
 *
 * @author falk
 */
public class ClassAnalyzer extends GeneralToolTemplate {
	ClassAnalyzer() throws ConfigurationException {
		super(new ClassAnalyzerParser());
	}

	public String description() {
		return "analyzes Java classes";
	}
}
