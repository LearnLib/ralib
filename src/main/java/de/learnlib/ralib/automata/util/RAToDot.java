/*
 * Copyright (C) 2015 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package de.learnlib.ralib.automata.util;

import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.automata.output.OutputTransition;

/**
 *
 * @author falk
 */
public class RAToDot {

    private final StringBuilder stringRA = new StringBuilder();

    private static final String NEWLINE = System.lineSeparator();

    private final boolean acceptingOnly;

    public RAToDot(RegisterAutomaton ra, boolean acceptingOnly) {
        this.acceptingOnly = acceptingOnly;
        intro();
        convert(ra);
        outro();
    }

    @Override
    public String toString() {
        return stringRA.toString();
    }

    private void intro() {
        stringRA.append("digraph RA {").append(NEWLINE);
    }

    private void outro() {
        stringRA.append("}").append(NEWLINE);
    }

    private void convert(RegisterAutomaton ra) {
        printInitialRegisetrs(ra);
        printLocations(ra);
        initialTransition(ra.getInitialState());
        printTransitions(ra);
    }

    private void printInitialRegisetrs(RegisterAutomaton ra) {
        stringRA.append("\"\" [shape=none,label=<");
        if (!ra.getInitialRegisters().isEmpty()) {
            stringRA.append(ra.getInitialRegisters());
        }
        stringRA.append(">]").append(NEWLINE);

    }

    private void printLocations(RegisterAutomaton ra) {
        for (RALocation loc : ra) {
            if (!acceptingOnly || loc.isAccepting()) {
                printLocation(loc);
                stringRA.append(" [shape=");
                stringRA.append( (loc.isAccepting()) ? "doublecircle" : "circle");
                stringRA.append("]");
                stringRA.append(NEWLINE);
            }
        }
    }

    private void printLocation(RALocation loc) {
        stringRA.append("\"");
        stringRA.append(loc.getName());
        stringRA.append("\"");
    }

    private void initialTransition(RALocation initialState) {
        stringRA.append("\"\" -> ");
        printLocation(initialState);
        stringRA.append(NEWLINE);
    }

    private void printTransitions(RegisterAutomaton ra) {
        for (Transition t : ra.getTransitions()) {
            if (!acceptingOnly || (t.getSource().isAccepting() &&
                    t.getDestination().isAccepting())) {

                printLocation(t.getSource());
                stringRA.append(" -> ");
                printLocation(t.getDestination());
                stringRA.append(" [label=<");

                if (t instanceof OutputTransition) {
                    printOutputLabel( (OutputTransition)t );
                } else {
                    printInputLabel( t );
                }

                stringRA.append(">]");
                stringRA.append(NEWLINE);
            }
        }
    }

    private void printInputLabel(Transition t) {
        stringRA.append(t.getLabel());
        if (!t.getGuard().getCondition().equals(TrueGuardExpression.TRUE)) {
            stringRA.append("|").append(escapeGuard(t.getGuard()));
        }
        if (!t.getAssignment().getAssignment().isEmpty()) {
            stringRA.append("<BR />");
            stringRA.append(t.getAssignment());
        }
    }

    private void printOutputLabel(OutputTransition t) {
        stringRA.append(t.getLabel());
        if (!t.getGuard().getCondition().equals(TrueGuardExpression.TRUE)) {
            stringRA.append("|").append(escapeGuard(t.getGuard()));
        }
        if (!t.getOutput().getFreshParameters().isEmpty() ||
                !t.getOutput().getOutput().isEmpty()) {
            stringRA.append("/").append(
                    t.getOutput().toString().replaceAll(">", ":"));
        }
        if (!t.getAssignment().getAssignment().isEmpty()) {
            stringRA.append("<BR />");
            stringRA.append(t.getAssignment());
        }
    }

    private String escapeGuard(TransitionGuard g) {
        return g.toString().replaceAll("&", "&amp;");
    }
}
