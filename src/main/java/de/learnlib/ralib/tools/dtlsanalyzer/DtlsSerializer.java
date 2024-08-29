package de.learnlib.ralib.tools.dtlsanalyzer;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class DtlsSerializer {

	private Map<String, ParameterizedSymbol> inputSymbolLookup;
	private Map<String, ParameterizedSymbol> outputSymbolLookup;
	private Map<String, DataType> typeLookup;

	public DtlsSerializer(ParameterizedSymbol [] inputSymbols, ParameterizedSymbol[] outputSymbols) {
		inputSymbolLookup = new LinkedHashMap<>();
		typeLookup = new LinkedHashMap<>();
		for (ParameterizedSymbol inputSymbol : inputSymbols) {
			inputSymbolLookup.put(inputSymbol.getName(), inputSymbol);
			for (DataType type : inputSymbol.getPtypes()) {
				typeLookup.put(type.getName(), type);
			}
		}

		outputSymbolLookup = new LinkedHashMap<>();
		for (ParameterizedSymbol outputSymbol : outputSymbols) {
			outputSymbolLookup.put(outputSymbol.getName(), outputSymbol);
			for (DataType type : outputSymbol.getPtypes()) {
				typeLookup.put(type.getName(), type);
			}
		}
	}

	public String serializeSymbolInstance(PSymbolInstance symbol) {
		StringBuilder sb = new StringBuilder();
		String methodName = symbol.getBaseSymbol().getName();
		sb.append(methodName.substring(0));
		sb.append("{");

		for (DataValue dv : symbol.getParameterValues()) {
			sb.append(dv.getType().getName()).append("=").append(dv.getId());
			sb.append(",");
		}

		if (symbol.getParameterValues().length > 0) {
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append("}");

		return sb.toString();
	}

	public String serializeSymbolQuery(ParameterizedSymbol symbol) {
		StringBuilder sb = new StringBuilder();
		String methodName = symbol.getName();
		sb.append(methodName.substring(0));
		sb.append("{");

		for (DataType dt : symbol.getPtypes()) {
			sb.append(dt.getName()).append("=").append("?");
			sb.append(",");
		}

		if (symbol.getPtypes().length > 0) {
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append("}");

		return sb.toString();
	}

	private ParameterizedSymbol getParameterizedSymbol(String symbolString, boolean output) {
		// transform Alerts to parser-friendly strings, e.g., Alert(FATAL,DECRYPT_ERROR) to Alert_FATAL_DECRYPT_ERROR
		symbolString = symbolString.replaceAll("\\,|\\(", "_").replaceAll("\\)", "");
		ParameterizedSymbol symbol = output ? outputSymbolLookup.get(symbolString) : inputSymbolLookup.get(symbolString);
		if (symbol == null) {
			throw new RuntimeException("Undefined symbol: " + symbolString);
		}
		return symbol;
	}

	public PSymbolInstance deserializeSymbolInstance(String symbolString, boolean output) {
		symbolString = symbolString.replace("+", "_MULTIPLE");
		ParameterizedSymbol symbol = null;
		DataValue [] values;
		int startParam = symbolString.indexOf("[");
		if (startParam == -1) {
			symbol = getParameterizedSymbol(symbolString, output);
			values = new DataValue [] {};
		} else {
			String actionName = symbolString.substring(0, startParam);
			symbol = getParameterizedSymbol(actionName, output);
			if (!symbolString.endsWith("]")) {
				throw new RuntimeException("Symbol instance: " + symbolString);
			}
			String paramString = symbolString.substring(startParam+1, symbolString.length() - 1);
			values = parseParamValueStrings(symbolString, paramString);
		}

		if (values.length != symbol.getArity()) {
			throw new RuntimeException("Arrity mismatch in symbol instance " + symbolString);
		}

		PSymbolInstance symbolInstance = new PSymbolInstance(symbol, values);
		return symbolInstance;
	}

	private DataValue [] parseParamValueStrings(String symbol, String paramString) {
		if (paramString.isEmpty()) {
			return new DataValue [] {};
		}
		paramString = paramString.replaceAll("\\}\\,\\{", ",");
		if (!paramString.startsWith("{") || !paramString.endsWith("}")) {
			throw new RuntimeException("Missing begin and end brackets in parameterized symbol " + symbol);
		}

		if (paramString.substring(1, paramString.length()-1).isEmpty()) {
			return new DataValue [] {};
		}
		String[] paramValues = paramString.substring(1, paramString.length()-1).split("\\,");

		List<DataValue> valList = new LinkedList<>();
		for (String paramValue : paramValues) {
			if (paramValue.isEmpty()) {
				continue;
			}
			String[] parts = paramValue.split("\\=");
			if (parts.length != 2) {
				throw new RuntimeException("Invalid parameter encoding in output " + symbol);
			}
			String typeStr = parts[0];
			DataType type = typeLookup.get(typeStr);
			if (type == null) {
				throw new RuntimeException(String.format("Instance %s refers to unknown type %s", symbol, typeStr));
			}
			Object concreteValue = valueOf(parts[1], type);
			valList.add(new DataValue(type, concreteValue));
		}

		return valList.toArray(new DataValue[valList.size()]);
	}



	protected static int uLongString2Int(String num){
		return Long.valueOf(num).intValue();
	}

	   /**
     * Reads an int (which is always signed in java) as unsigned,
     * stored in a long
     * @param x
     * @return
     */
    protected static long int2ULong(int x) {
        return Integer.toUnsignedLong(x);
    }

    public static <T> T valueOf(String serializedParameter, DataType parameterType) {
        // TODO we should use the domain and not the base type here
        Class<T> cls = parameterType.getBase();
        if (cls == Integer.class)
            return cls.cast(Integer.valueOf(serializedParameter));
        else if (cls == Double.class)
            return cls.cast(Double.valueOf(serializedParameter));
        else if (cls == Long.class)
            return cls.cast(Long.valueOf(serializedParameter));

        throw new RuntimeException("Deserialization not supported for " + cls);
    }
}
