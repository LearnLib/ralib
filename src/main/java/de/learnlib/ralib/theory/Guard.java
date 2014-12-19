/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.theory;

import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;

/**
 *
 * @author falk
 */
public class Guard {
    
    private final Parameter parameter;
    private final Register register;
    private final Relation relation;
    
    
    public Parameter getParameter() {
        return this.parameter;
    }
    
    public Register getRegister() {
        return this.register;        
    }
    
    public Relation getRelation() {
        return this.relation;
    }
    
    public Guard(Parameter param, Register reg, Relation rel) {
        
        this.parameter = param;
        this.register = reg;
        this.relation = rel;
    }
    
//    public Guard createCopy(VarMapping renaming) {
//        return this;
//    }
    
    public Guard[] or(Guard... guards) {
    //        Guard[] guardList = new Guard[guards.length];
    //        for (int i=0; i<guards.length; i++) {
    //            guardList[i] = guards[i];
    //        }
        
        // check feasibility here...
        
            return guards;
        }
    
    public boolean equals(Guard other) {
        return (this.parameter == other.getParameter() &&
                this.register == other.getRegister() &&
                this.relation == other.getRelation());
    }
    
    public boolean contradicts(Guard other) {
        boolean samePR = (this.parameter.getId() == other.getParameter().getId() && 
     //           this.register.getVC() == other.getRegister().getVC() &&
                this.register.getId() == other.getRegister().getId());
        //System.out.println("params " + this.parameter.toString() + " and " + other.getParameter().toString() + "\nregs " + this.register.toString() + " and " + other.getRegister().toString() + "? : " + samePR);
        boolean contRels = ((this.relation.equals(Relation.SMALLER) && 
                (other.getRelation().equals(Relation.BIGGER))) ||
                (this.relation.equals(Relation.BIGGER) &&
                (other.getRelation().equals(Relation.SMALLER))));
        //System.out.println("rels contradict: " + contRels);
        return samePR && contRels;
    }
    
    @Override
    public String toString() {
        return this.parameter + " " + this.relation + " " + this.register;
    }
            
    
    }
