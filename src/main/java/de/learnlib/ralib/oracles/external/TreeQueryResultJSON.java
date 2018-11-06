/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.oracles.external;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author falk
 */
public class TreeQueryResultJSON {
 
  private final SdtJSON sdt;
   private final PIVElementJSON[] piv;

    public TreeQueryResultJSON(SdtJSON sdt, PIVElementJSON ... piv) {
        this.sdt = sdt;
        this.piv = piv;
    }

    public SdtJSON getSdt() {
        return sdt;
    }

    public Map<SDTVariableJSON, DataValuePrefixJSON> getPivasMap() {
        Map<SDTVariableJSON, DataValuePrefixJSON> pivMap = new HashMap<>();
        for (PIVElementJSON e : piv) {
            pivMap.put(e.getKey(), e.getValue());
        }        
        return pivMap;
    }

    public PIVElementJSON[] getPiv() {
        return piv;
    }
    
    @Override
    public String toString() {
        return "TQR[" + sdt + ", " + piv + "]";
    }

    
    
    
}
