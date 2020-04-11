package de.learnlib.ralib.sul;

public class InputCounter {
    private int resets = 0;
    
    private int  inputs = 0;
    
    public InputCounter() {
    	
    }
    
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
