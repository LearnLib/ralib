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

package de.learnlib.ralib.sul;

import de.learnlib.api.SUL;
import de.learnlib.ralib.words.PSymbolInstance;

/**
 *
 * @author falk
 */
public abstract class DataWordSUL implements SUL<PSymbolInstance, PSymbolInstance> {
    
    private long resets = 0;
    
    private long inputs = 0;
    
    protected void countResets(int n) {
        resets += n;
    }
    
    protected void countInputs(int n) {
        inputs += n;
    }

    /**
     * @return the resets
     */
    public long getResets() {
        return resets;
    }

    /**
     * @return the inputs
     */
    public long getInputs() {
        return inputs;
    }
    
    
}
