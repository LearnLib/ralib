/*
 * Copyright (C) 2014 falk.
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

package de.learnlib.ralib.theory.equality;

import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.theory.Guard;
/**
 *
 * @author falk
 */
public class ElseGuard extends Guard {
    
    public ElseGuard(SymbolicDataValue param) {
        super(param, null);     
    }
    
    
    @Override
    public String toString() {
        return "ELSE[" + this.getParameter().toString() + "]";
    }
    
    public ElseGuard join(Equality e) {
        //System.out.println("e.param = " + e.getParameter().toString() + ", this.param = " + this.getParameter().toString());
        assert e.getParameter().equals(this.getParameter());
        return this;
    }
}