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
package de.learnlib.ralib;

import java.io.IOException;
import java.util.logging.Logger;

import org.testng.annotations.BeforeSuite;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.ParameterizedSymbol;

/**
 *
 * @author falk
 */
public abstract class RaLibTestSuite {

    protected final ParameterizedSymbol ERROR = 
            new OutputSymbol("_io_err", new DataType[]{});    
        
    protected static final Logger logger = Logger.getLogger("UnitTest");
    protected static TestConfig TEST_CONFIG;
    
    @BeforeSuite
    public void beforeSuite() throws IOException, ConfigurationException{
    	TEST_CONFIG = TestConfig.parseTestConfig();
        TestUtil.configureLogging(TEST_CONFIG.getLoggingLevel());
    }
    
    public TestConfig getTestConfig() {
    	return TEST_CONFIG;
    }
    
}
