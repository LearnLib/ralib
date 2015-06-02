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

package de.learnlib.ralib.solver;

import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.solver.simple.SimpleSolver;

/**
 *
 * @author falk
 */
public class ConstraintSolver {
    
//    private final gov.nasa.jpf.constraints.api.ConstraintSolver solver;
	private final SimpleSolver solver;
    
    public ConstraintSolver() {
//        ConstraintSolverFactory fact = new ConstraintSolverFactory();
//        this.solver = fact.createSolver("z3");  
    	this.solver = new SimpleSolver();
    }
    
    public boolean isSatisfiable(GuardExpression expr) {
//        Expression<Boolean> jexpr = expr.toExpression();
//        Result r = solver.isSatisfiable(jexpr);
//        return r == Result.SAT;
    	return solver.isSatisfiable(expr);
    }
    
}
