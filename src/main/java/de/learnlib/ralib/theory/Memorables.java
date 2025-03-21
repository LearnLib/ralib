package de.learnlib.ralib.theory;

import java.util.*;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.RegisterAssignment;
import de.learnlib.ralib.data.SDTRelabeling;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;

public class Memorables {

    public static Set<DataValue> relabel(Set<DataValue> values, SDTRelabeling relabeling) {
        Set<DataValue> result = values.stream()
                .map(relabeling::get)
                .map( d -> (DataValue) d)
                .collect(Collectors.toSet());

        assert result.size() == values.size();
        return result;
    }

    public static Map<DataType, Integer> typedSize(Set<DataValue> values) {
        Map<DataType, Integer> ret = new LinkedHashMap<>();
        values.forEach(v -> {
            Integer i = ret.get(v.getDataType());
            i = (i == null) ? 1 : i+1;
            ret.put(v.getDataType(), i);
        });
        return ret;
    }

    // TODO: duplicates code from MappedPrefix
    public static List<DataValue> memorableValues(SDT ... sdts) {
        return Arrays.stream(sdts)
                .flatMap(sdt -> sdt.getDataValues().stream())
                .distinct()
                .sorted()
                .toList();
    }

    // TODO: duplicates code from MappedPrefix
    public static RegisterAssignment getAssignment(SDT ... sdts) {
        RegisterAssignment ra = new RegisterAssignment();
        SymbolicDataValueGenerator.RegisterGenerator regGen =
                new SymbolicDataValueGenerator.RegisterGenerator();

        memorableValues(sdts).forEach(
                dv -> ra.put(dv, regGen.next(dv.getDataType()))
        );

        return ra;
    }

}
