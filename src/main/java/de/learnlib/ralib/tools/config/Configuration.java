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

package de.learnlib.ralib.tools.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.Random;

/**
 *
 * @author falk
 */
public class Configuration extends Properties {

    public Configuration(File f) throws IOException {    
        super.load(new FileInputStream(f));
    }

    public Configuration(String args) throws IOException {
        super.load(new StringReader(args));
    }
    
    public Random getRandom() {
        throw new UnsupportedOperationException("not implemented yet.");
    }
    
    public void setRandom(Random r) {
        
    }
    
}
