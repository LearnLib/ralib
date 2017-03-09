package de.learnlib.ralib.example.succ;
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

import java.util.function.Supplier;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.examples.IntAbstractTCPExample;
import de.learnlib.ralib.sul.examples.IntHardFreshTCPExample;
import de.learnlib.ralib.sul.examples.IntAbstractTCPExample.Option;
import de.learnlib.ralib.sul.examples.IntHardFreshTCPExample.Packet;
import de.learnlib.ralib.sul.examples.IntHardFreshTCPExample.Timeout;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class IntHardFreshTCPSUL extends DataWordSUL {

	public static final DataType<Integer> INT_TYPE = new DataType<Integer>("INTEGER", Integer.class);

	public static final ParameterizedSymbol ISYN = new InputSymbol("ISYN", new DataType[] { INT_TYPE, INT_TYPE });
	public static final ParameterizedSymbol ISYNACK = new InputSymbol("ISYNACK", new DataType[] { INT_TYPE, INT_TYPE });
	public static final ParameterizedSymbol IACK = new InputSymbol("IACK", new DataType[] { INT_TYPE, INT_TYPE });

	public static final ParameterizedSymbol IFINACK = new InputSymbol("IFINACK", new DataType[] { INT_TYPE, INT_TYPE });
	public static final ParameterizedSymbol IACKPSH = new InputSymbol("IACKPSH", new DataType[] { INT_TYPE, INT_TYPE });

	public static final ParameterizedSymbol ERROR = new OutputSymbol("_io_err", new DataType[] {});

	public final ParameterizedSymbol[] getInputSymbols() {
		return new ParameterizedSymbol[] { ISYN, ISYNACK, IACK, IFINACK, IACKPSH };
	}

	public static final ParameterizedSymbol OTIMEOUT = new OutputSymbol("OTIMEOUT", new DataType[] {});
	public static final ParameterizedSymbol OSYNACK = new OutputSymbol("OSYNACK",
			new DataType[] { INT_TYPE, INT_TYPE });
	public static final ParameterizedSymbol OACK = new OutputSymbol("OACK", new DataType[] { INT_TYPE, INT_TYPE });

	public final ParameterizedSymbol[] getOutputSymbols() {
		return new ParameterizedSymbol[] { OSYNACK, OACK, OTIMEOUT, ERROR };
	}

	private IntHardFreshTCPExample tcpSut;
	private Supplier<IntHardFreshTCPExample> supplier;

	private Option[] options;

	public IntHardFreshTCPSUL() {
		supplier = () -> new IntHardFreshTCPExample();
	}

	public IntHardFreshTCPSUL(Integer window) {
		supplier = () -> new IntHardFreshTCPExample(window);
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

	public void configure(Option... options) {
		this.options = options;
	}

	private PSymbolInstance createOutputSymbol(Object x) {
		PSymbolInstance ret = null;
		if (x instanceof Timeout) {
			ret = new PSymbolInstance(OTIMEOUT);
		} else {
			Packet pkt = ((Packet) x);
			switch (pkt.flags) {
			case ACK:
				ret = new PSymbolInstance(OACK, new DataValue[] { dv(pkt.seqNum), dv(pkt.ackNum) });
				break;
			case SYNACK:
				ret = new PSymbolInstance(OSYNACK, new DataValue[] { dv(pkt.seqNum), dv(pkt.ackNum) });
				break;
			default:
				throw new NotImplementedException();
			}
		}
		return ret;
	}

	public DataValue<Integer> dv(Object val) {
		return new DataValue<Integer>(INT_TYPE, (Integer) val);
	}

	@Override
	public PSymbolInstance step(PSymbolInstance i) throws SULException {
		countInputs(1);
		if (i.getBaseSymbol().equals(ISYN)) {
			Object x = tcpSut.ISYN((Integer) i.getParameterValues()[0].getId(),
					(Integer) i.getParameterValues()[1].getId());
			return createOutputSymbol(x);
		} else if (i.getBaseSymbol().equals(IACK)) {
			Object x = tcpSut.IACK((Integer) i.getParameterValues()[0].getId(),
					(Integer) i.getParameterValues()[1].getId());
			return createOutputSymbol(x);
		} else if (i.getBaseSymbol().equals(IACKPSH)) {
			Object x = tcpSut.IPSHACK((Integer) i.getParameterValues()[0].getId(),
					(Integer) i.getParameterValues()[1].getId());
			return createOutputSymbol(x);
		} else if (i.getBaseSymbol().equals(IFINACK)) {
			Object x = tcpSut.IFINACK((Integer) i.getParameterValues()[0].getId(),
					(Integer) i.getParameterValues()[1].getId());
			return createOutputSymbol(x);
		}

		else {
			throw new IllegalStateException("i must be instance of connect or flag config");
		}
	}

}
