/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.theory;

import de.learnlib.ralib.data.DataType;

/**
 *
 * @author Sofia Cassel
 */

public final class DoubType extends DataType implements Comparable {
        
    
        public DoubType() {
            super("doubType", Double.class);
        }
        
   //     public class Comp implements Comparator<DataValue<Integer>> {
//        
    @Override
    public int compareTo(Object dv_b) {
        assert dv_b instanceof DoubType;
        //System.out.println("comparing");
//            //System.out.println("id, dv_a: " + dv_a.getId().toString());
//            //System.out.println("id, dv_b: " + (dv_b.getId()).getClass() + " " + dv_b.getId());
        DoubType dvd_a = this;
        DoubType dvd_b = (DoubType) dv_b;
        Double db_a = Double.class.cast(dvd_a);
        Double db_b = Double.class.cast(dvd_b);
        return Double.compare(db_a, db_b);
    }

        
    }
