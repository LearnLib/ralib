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

/**
 *
 * @author Sofia Cassel
 */

package de.learnlib.ralib.theory;

public enum Relation {
    EQUALS("=="), 
    SMALLER("<"), 
    PLUS_ONE("+1=="), 
    MEMBER_OF(" in "),
    BIGGER(">"),
    ELSE("else"),
    TRUE("true"),
    NOT_EQUALS("!=");
    
    private final String name;       

    private Relation(String s) {
        name = s;
    }

    @Override
    public String toString(){
       return name;
    }    
}

