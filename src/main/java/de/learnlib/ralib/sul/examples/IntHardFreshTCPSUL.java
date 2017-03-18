package de.learnlib.ralib.sul.examples;
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

import de.learnlib.api.SULException;
import de.learnlib.ralib.sul.examples.IntHardFreshTCPExample.Packet;
import de.learnlib.ralib.sul.examples.IntHardFreshTCPExample.Timeout;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteInput;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteOutput;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteSUL;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class IntHardFreshTCPSUL extends ConcreteSUL {

	private Integer window;
	private IntHardFreshTCPExample tcpSut;

	public IntHardFreshTCPSUL() {
		window = 1000;
	}

	@Override
	public void pre() {
		this.tcpSut = new IntHardFreshTCPExample(window);
	}

	@Override
	public void post() {
		this.tcpSut = null;
	}

	private ConcreteOutput createOutputSymbol(Object x) {
		ConcreteOutput ret = null;
		if (x instanceof Timeout) {
			ret = new ConcreteOutput("OTIMEOUT");
		} else {
			Packet pkt = ((Packet) x);
			switch (pkt.flags) {
			case ACK:
				ret = new ConcreteOutput("OACK", new Object[] { pkt.seqNum, pkt.ackNum });
				break;
			case SYNACK:
				ret = new ConcreteOutput("OSYNACK", new Object[] { pkt.seqNum, pkt.ackNum });
				break;
			default:
				throw new NotImplementedException();
			}
		}
		return ret;
	}

	@Override
	public ConcreteOutput step(ConcreteInput in) throws SULException {
		if (in.getMethodName().equals("ISYN")) {
			Object x = tcpSut.ISYN((Integer) in.getParameterValues()[0],
					(Integer) in.getParameterValues()[1]);
			return createOutputSymbol(x);
		} else if (in.getMethodName().equals("IACK")) {
			Object x = tcpSut.IACK((Integer) in.getParameterValues()[0],
					(Integer) in.getParameterValues()[1]);
			return createOutputSymbol(x);
		} else if (in.getMethodName().equals("IACKPSH")) {
			Object x = tcpSut.IPSHACK((Integer) in.getParameterValues()[0],
					(Integer) in.getParameterValues()[1]);
			return createOutputSymbol(x);
		} else if (in.getMethodName().equals("IFINACK")) {
			Object x = tcpSut.IFINACK((Integer) in.getParameterValues()[0],
					(Integer) in.getParameterValues()[1]);
			return createOutputSymbol(x);
		}
		return null;
	}

}
