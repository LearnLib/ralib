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

import java.util.Collection;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.xml.RegisterAutomatonImporter;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.SimulatorOracle;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.oracles.io.IOFilter;
import de.learnlib.ralib.oracles.io.IOOracle;
import de.learnlib.ralib.oracles.mto.MultiTheoryTreeOracle;
import de.learnlib.ralib.solver.ConstraintSolver;
import de.learnlib.ralib.solver.jconstraints.JConstraintsConstraintSolver;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.sul.SULOracle;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;

/**
 *
 * @author falk
 */
public class TestUtil {

    /**
     * Configures the logging system.
     *
     * @param lvl  the logging level
     */
    public static void configureLogging(Level lvl) {
        Logger root = Logger.getLogger("");
        root.setLevel(lvl);
        for (Handler h : root.getHandlers()) {
            h.setLevel(lvl);
        }
    }

    public static JConstraintsConstraintSolver getZ3Solver() {
        gov.nasa.jpf.constraints.api.ConstraintSolver solver =
                ConstraintSolverFactory.createSolver("z3");

        return new JConstraintsConstraintSolver(solver);
    }

    public static MultiTheoryTreeOracle createMTO(
            DataWordSUL sul, ParameterizedSymbol error,
            Map<DataType, Theory> teachers, Constants consts,
            ConstraintSolver solver, ParameterizedSymbol ... inputs) {

        IOOracle ioOracle = new SULOracle(sul, error);
        return createMTO(ioOracle, teachers, consts, solver, inputs);
    }

    public static MultiTheoryTreeOracle createMTO(
            IOOracle ioOracle,
            Map<DataType, Theory> teachers, Constants consts,
            ConstraintSolver solver, ParameterizedSymbol ... inputs) {

        IOCache ioCache = new IOCache(ioOracle);
        IOFilter ioFilter = new IOFilter(ioCache, inputs);

        MultiTheoryTreeOracle mto = new MultiTheoryTreeOracle(
                ioFilter, teachers, consts, solver);

        return mto;
    }


    /**
     * Creates an MTO from an RA by simulating it.
     */
    public static MultiTheoryTreeOracle createSimulatorMTO(RegisterAutomaton regAutomaton, Map<DataType, Theory> teachers, Constants consts, ConstraintSolver solver) {
        DataWordOracle hypOracle = new SimulatorOracle(regAutomaton);
        return new MultiTheoryTreeOracle(hypOracle, teachers, consts, solver);
    }

    public static RegisterAutomatonImporter getLoader(String resName) {
        return new RegisterAutomatonImporter(
                TestUtil.class.getResourceAsStream(resName));
    }

    public static DataType getType(String name, Collection<DataType> dataTypes) {
        for (DataType t : dataTypes) {
            if (t.getName().equals(name)) {
                return t;
            }
        }
        return null;
    }

}
