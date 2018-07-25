/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.oracles.external;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

/**
 *
 * @author falk
 */
public class TreeQueryResultJSON {
 
  private final SdtJSON sdt;
  @SerializedName("PIV") private final Map<String,Integer> piv;

    public TreeQueryResultJSON(SdtJSON sdt, Map<String, Integer> piv) {
        this.sdt = sdt;
        this.piv = piv;
    }

    public SdtJSON getSdt() {
        return sdt;
    }

    public Map<String, Integer> getPiv() {
        return piv;
    }

    @Override
    public String toString() {
        return "TQR[" + sdt + ", " + piv + "]";
    }

    
    
    
}
