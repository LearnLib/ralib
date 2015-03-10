/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.theory;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import java.util.Comparator;

/**
 *
 * @author Sofia Cassel
 */
public class CompTypes implements Comparator<DataValue<? extends DataType>> {
    
    @Override
        public int compare(DataValue<? extends DataType> dv_a, DataValue<? extends DataType> dv_b) {
            Class ic = dv_a.getType().getBase();
            // log.log(Level.FINEST,"dv_a class is " + ic.toString());
            if (ic == Integer.class) {
                return intcompare((DataValue<IntType>) dv_a, (DataValue<IntType>) dv_b);
            }
            else if (ic == Double.class) {
                return doubcompare((DataValue<DoubType>) dv_a, (DataValue<DoubType>) dv_b);
            }
            else {
                return 0;
            }
        }
        
    public int intcompare(DataValue<IntType> dv_a, DataValue<IntType> dv_b) {
        Integer int_a = Integer.class.cast(dv_a.getId());
        Integer int_b = Integer.class.cast(dv_b.getId());
        return int_a.compareTo(int_b); }
    
    public int doubcompare(DataValue<DoubType> dv_a, DataValue<DoubType> dv_b) {
            Double db_a = Double.class.cast(dv_a.getId());
            Double db_b = Double.class.cast(dv_b.getId());
            return db_a.compareTo(db_b);
        
    }
       
}