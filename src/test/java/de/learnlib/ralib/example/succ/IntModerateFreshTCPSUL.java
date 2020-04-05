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
package de.learnlib.ralib.example.succ;


import java.util.function.Supplier;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.example.succ.IntAbstractTCPExample.Option;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class IntModerateFreshTCPSUL extends DataWordSUL {

    public static final DataType<Integer> INT_TYPE = 
            new DataType<Integer>("Integer", Integer.class);    
    
    public static final ParameterizedSymbol ICONNECT = 
            new InputSymbol("IConnect");
    public static final ParameterizedSymbol ISYN = 
            new InputSymbol("ISYN", new DataType[]{INT_TYPE, INT_TYPE});
    public static final ParameterizedSymbol ISYNACK = 
            new InputSymbol("ISYNACK", new DataType[]{INT_TYPE, INT_TYPE});
    public static final ParameterizedSymbol IACK = 
            new InputSymbol("IACK", new DataType[]{INT_TYPE, INT_TYPE});
    public static final ParameterizedSymbol IFINACK = 
            new InputSymbol("IFINACK", new DataType[]{INT_TYPE, INT_TYPE});
    
    public static final ParameterizedSymbol ERROR = 
            new OutputSymbol("_io_err", new DataType[]{});

    
    public final ParameterizedSymbol[] getInputSymbols() {
        return new ParameterizedSymbol[] { ICONNECT, ISYN, ISYNACK, IACK, IFINACK};
    }
        
    public static final ParameterizedSymbol OK = 
            new OutputSymbol("_ok", new DataType[]{});
        
    public static final ParameterizedSymbol NOK = 
            new OutputSymbol("_not_ok", new DataType[]{});
    
    public static final ParameterizedSymbol OCONNECT = 
            new OutputSymbol("OCONNECT", new DataType[]{INT_TYPE});
    

    public final ParameterizedSymbol[] getActionSymbols() {
        return new ParameterizedSymbol[] { OK, NOK, OCONNECT };
    }


    private IntModerateFreshTCPExample tcpSut;
    private Supplier<IntModerateFreshTCPExample> supplier;

	private Option[] options ;
    
    public IntModerateFreshTCPSUL() {
    	supplier = () -> new IntModerateFreshTCPExample();
    }
    
    public IntModerateFreshTCPSUL(Integer window) {
    	supplier = () -> new IntModerateFreshTCPExample(window);
    }

    @Override
    public void pre() {
        countResets(1);
        this.tcpSut = supplier.get();
        if (options != null) {
        	this.tcpSut.configure(options);
        }
    }

    @Override
    public void post() {
        this.tcpSut = null;
    }
    
    public void configure(Option ... options ) {
    	this.options = options;
    }

    private PSymbolInstance createOutputSymbol(Object x) {
        if (x instanceof Boolean) {
            return new PSymbolInstance( ((Boolean) x) ? OK : NOK);
        } else {
        	return new PSymbolInstance(OCONNECT, new DataValue<Integer>(INT_TYPE, (Integer) x));
        }
     }

    @Override
    public PSymbolInstance step(PSymbolInstance i) throws SULException {
        countInputs(1);
        if (i.getBaseSymbol().equals(ICONNECT)) {
            Object x = tcpSut.IConnect();
            return createOutputSymbol(x);
        } else if (i.getBaseSymbol().equals(ISYN)) {
            Object x = tcpSut.ISYN(
            		(Integer)i.getParameterValues()[0].getId(), 
            		(Integer)i.getParameterValues()[1].getId());
            return createOutputSymbol(x); 
        } else if (i.getBaseSymbol().equals(ISYNACK)) {
            Object x = tcpSut.ISYNACK(
            		(Integer)i.getParameterValues()[0].getId(), 
            		(Integer)i.getParameterValues()[1].getId());
            return createOutputSymbol(x); 
        } else if (i.getBaseSymbol().equals(IACK)) {
            Object x = tcpSut.IACK(
            		(Integer)i.getParameterValues()[0].getId(), 
            		(Integer)i.getParameterValues()[1].getId());
            return createOutputSymbol(x); 
        } else if (i.getBaseSymbol().equals(IFINACK)) {
            Object x = tcpSut.IFINACK(
            		(Integer)i.getParameterValues()[0].getId(), 
            		(Integer)i.getParameterValues()[1].getId());
            return createOutputSymbol(x); 
        } else {
            throw new IllegalStateException("i must be instance of connect or flag config. i: " + i);
        }
    }
}
