package de.learnlib.ralib.tools;

import org.apache.commons.lang3.NotImplementedException;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;

public class DataValueSerializer {
	public static final String SIMPLE_DV = "[0-9\\.]+";
	public static final String DV = "[0-9\\.]+";
	public static final String INT_DV = DV + "\\(("+DV +")?\\:("+DV+")?\\)";
	public static final String PLUS = "\\s*\\+\\*s";
	public static final String SUMC_DV = "(" + DV + ")|(" + INT_DV +")" + "(" + PLUS + DV + ")+";
	
	public static DataValue deserialize(String dvString, DataType type) {
		DataValue dv;
		dvString = dvString.trim();
		dvString = dvString.replaceAll("\\[" +type.getName() + "\\]", "");
		if (dvString.matches(SUMC_DV)) {
			String[] terms = dvString.split("\\+");
			dv = deserialize(terms[0].trim(), type);
			for (int j=1; j < terms.length; j++) 
				dv = new SumCDataValue(dv, DataValue.valueOf(terms[j], type));
		} else {
			if (dvString.matches(INT_DV)) {
				String val = dvString.substring(0, dvString.indexOf('('));
				dv = deserialize(val, type);
				String range = dvString.substring(dvString.indexOf('(')+1, dvString.lastIndexOf(')')-1);
				String[] ends = range.split(":");
				String left = ends[0];
				String right = ends[1];
				DataValue dvLeft = left.isEmpty()? null : deserialize(left, type);
				DataValue dvRight = right.isEmpty()? null : deserialize(right, type);
			} else {
				if (dvString.matches(DV)) {
					dv = DataValue.valueOf(dvString, type);
				}
				else {
					throw new NotImplementedException("Unfortunately, deserializing complex structures is not supported");
				}
			}
		}
		
		return dv;
	}

}
