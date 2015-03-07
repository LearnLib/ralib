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

package de.learnlib.ralib.tools;

import de.learnlib.ralib.tools.config.Configuration;

/**
 *
 * @author falk
 */
public abstract class ConsoleClient {
    
    private final Configuration config;

    public abstract void run();

    public abstract String help();
    
    public ConsoleClient(String[] args) {
        this.config = parse(args);
    }
    
    private Configuration parse(String[] params) {
        if (params.length == 2 && params[0].equals("-f")) {
            return parseFile(params[1]);
        }
        else if (params.length == 1) {
            return new Configuration(params);
        }
        else {
            //System.err.println("")
            return null;
        }
    }
    
    private Configuration parseFile(String string) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
