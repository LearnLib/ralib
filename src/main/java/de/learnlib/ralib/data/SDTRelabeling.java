package de.learnlib.ralib.data;

public class SDTRelabeling extends Mapping<SDTGuardElement, SDTGuardElement> {

    public static SDTRelabeling fromBijection(Bijection<DataValue> in) {
        SDTRelabeling ret = new SDTRelabeling();
        ret.putAll(in);
        return ret;
    }

    public static SDTRelabeling fromMapping(Mapping<SDTGuardElement, SDTGuardElement> mapping) {
    	SDTRelabeling ret = new SDTRelabeling();
    	ret.putAll(mapping);
    	return ret;
    }
}
