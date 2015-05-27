/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.example.priority;

/**
 *
 * @author Stealth
 */
public class PQWrapper extends java.util.PriorityQueue {
    
    private final int capacity = 3;

    @Override
    public boolean offer(Object e) {
        if (this.size() >= capacity) {
            return false;
        }
        
        return super.offer(e); 
    }
    
}
