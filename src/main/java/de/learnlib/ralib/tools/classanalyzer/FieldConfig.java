package de.learnlib.ralib.tools.classanalyzer;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;

public class FieldConfig {
	private Map<Field, Object> fieldConfig;
	private Class<?> cls;
	public FieldConfig(Class <?> cls, String [] fieldConfigStrings) {
		this.cls = cls;
		this.fieldConfig = parseFieldConfiguration(cls, fieldConfigStrings);
	}
	
	private Map<Field, Object> parseFieldConfiguration(Class<?> cls, String[] fieldConfigStrings) {
		Map<Field, Object> fieldValMap = new LinkedHashMap<>();
		for (String configString : fieldConfigStrings) {
			String[] fieldVal = configString.split("\\:");
			assert fieldVal.length == 2;
			Field field = getField(cls, fieldVal[0]);
			if (field == null) 
				throw new DecoratedRuntimeException("FIeld with name "+ fieldVal[0] + " could not be found");
			field.setAccessible(true);
			Object val = DataValue.valueOf(fieldVal[1], field.getType());
			fieldValMap.put(field, val);
		}
		
		return fieldValMap;
	}
	
	public Field getField(Class<?> cls, final String fieldName) {
		Optional<Field> foundField = Arrays.stream(cls.getDeclaredFields()).
		filter(field -> field.getName().equals(fieldName)).findAny();
		if (foundField.isPresent())
			return foundField.get();
		if (cls.getSuperclass() != null)
			return getField (cls.getSuperclass(), fieldName);
		return null;
	}

	void setFields(Object clsInstance) {
		assert cls.isInstance(clsInstance);
		fieldConfig.forEach((f,v) -> {
			try {
				f.set(clsInstance, v);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			} 
		});
	}
}
