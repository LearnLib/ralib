package de.learnlib.ralib.data;

public class SDTRelabeling extends Mapping<SDTGuardElement, SDTGuardElement> {

    public static SDTRelabeling fromBijection(Bijection<DataValue> in) {
        SDTRelabeling ret = new SDTRelabeling();
        ret.putAll(in);
        return ret;
    }
}
