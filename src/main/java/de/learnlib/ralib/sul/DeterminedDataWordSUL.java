package de.learnlib.ralib.sul;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;

public class DeterminedDataWordSUL extends DataWordSUL{
	
	private final Map<DataType, Theory> theories;
	
    private final Map<DataType, LinkedHashMap<Object, Object>> buckets = new HashMap<>();

	private final DataWordSUL sul;
	
	public DeterminedDataWordSUL(Map<DataType, Theory> theories, DataWordSUL sul) {
		this.theories = theories;
		this.sul = sul;
		
	}

	@Override
	public void pre() {
		 countResets(1);
		 this.sul.pre();
		 this.buckets.clear();
	}

	@Override
	public void post() {
		this.sul.post();
	}

	@Override
	public PSymbolInstance step(PSymbolInstance input) throws SULException {
        countInputs(1);
        updateSeenInput(input);

        input = decanonizeInput(input);
        
        PSymbolInstance output = this.sul.step(input);

        Map<DataValue, FreshValue> freshValueMap = updateSeenOutput(output);
        output = canonizeOutput(output, freshValueMap);
        

        return output;
	}
	
	private PSymbolInstance decanonizeInput(PSymbolInstance input) {
		 DataValue[] dvs = input.getParameterValues();
	     DataValue[] undetInVal = new DataValue[dvs.length];
	     int i = 0;
	     for (DataValue detVal : dvs) {
	         undetInVal[i] = new DataValue( detVal.getType(), resolveInputValue(dvs[i]));
	         i++;
	     }
	     return new PSymbolInstance(input.getBaseSymbol(), undetInVal);
	     
	}
	
	private PSymbolInstance canonizeOutput(PSymbolInstance output, Map<DataValue, FreshValue> freshValueMap) {
        DataValue [] detOutVals = new DataValue [output.getParameterValues().length];
        int i = 0;
        for (DataValue outVal : output.getParameterValues()) {
        	if (freshValueMap.containsKey(outVal))
        		detOutVals[i++] = freshValueMap.get(outVal);
        	else 
        		detOutVals[i++] = new DataValue(outVal.getType(), resolveOutputValue(outVal));
        }
        return new PSymbolInstance(output.getBaseSymbol(), detOutVals);
	}
	
    private void updateSeenInput(PSymbolInstance input) {
    	DataValue[] vals = input.getParameterValues();
        for (DataValue v : vals) {
        	LinkedHashMap<Object, Object> map = this.buckets.get(v.getType());
            if (map == null) {
                map = new LinkedHashMap<>();
                this.buckets.put(v.getType(), map);
            }
            
            final Map<Object, Object> fmap = map;
            
            if (!map.containsKey(v.getId())) {
            	Origin origin = getOriginOfValueFromCollection(v.getType(), v.getId(), () -> fmap.keySet());
            	if (origin != null) {
	            	List<DataValue> potsOfUndetermined = getPotsOfCollection(v.getType(), () -> fmap.values());
	            	DataValue undeterminedDv = potsOfUndetermined.get(origin.indexInPots);
	            	map.put(v.getId(), undeterminedDv.getId());
            	} else {
            		map.put(v.getId(), v.getId());
            	}
            }
        }
    }
    
    private Map<DataValue, FreshValue> updateSeenOutput(PSymbolInstance output) {
    	Map<DataValue, FreshValue> freshValueMap = new HashMap<>();
    	DataValue[] vals = output.getParameterValues();
        for (DataValue v : vals) {
        	LinkedHashMap<Object, Object> map = this.buckets.get(v.getType());
            if (map == null) {
                map = new LinkedHashMap<>();
                this.buckets.put(v.getType(), map);
            }
            
            final Map<Object, Object> fmap = map;
            
            if (!map.containsKey(v.getId()) && !map.containsValue(v.getId())) {
            	Origin origin = getOriginOfValueFromCollection(v.getType(), v.getId(), () -> fmap.keySet());
            	if (origin != null) {
	            	List<DataValue> potsOfUndetermined = getPotsOfCollection(v.getType(), () -> fmap.values());
	            	DataValue undeterminedDv = potsOfUndetermined.get(origin.indexInPots);
	            	map.put(v.getId(), undeterminedDv.getId());
            	} else {
            		origin = getOriginOfValueFromCollection(v.getType(), v.getId(), () -> fmap.values());
            		if (origin != null) {
            			List<DataValue> potsOfDetermined = getPotsOfCollection(v.getType(), () -> fmap.values());
    	            	DataValue determinedDv = potsOfDetermined.get(origin.indexInPots);
    	            	map.put(determinedDv.getId(), v.getId());
            		} else {
            			// truly fresh
            			FreshValue fv = registerFreshValue(v.getType(), v.getId());
            			freshValueMap.putIfAbsent(v,  fv);
            		} 
            	}
            }
        }
        
        return freshValueMap;
    }
    
    private Object resolveInputValue(DataValue d) {
        Map<Object, Object> map = this.buckets.get(d.getType());
        if (map == null || !map.containsKey(d.getId())) {
            //System.out.println(d);
            assert false;
            return d.getId();
        }
        //System.out.println("Get: " + d + " : " + map.get(d));
        return map.get(d.getId());
    }
    
    private Object resolveOutputValue(DataValue d) {
        Map<Object, Object> map = this.buckets.get(d.getType());
        if ( !map.containsKey(d.getId())) {
            if (!map.containsValue(d.getId())) {
            	assert false;
            	return d.getId();
            } else {
            	Object key = map.entrySet().stream().
            			filter(entry -> entry.getValue().equals(d.getId())).findFirst().get().getKey();
            	assert key != null;
            	return key;
            }
        } else
        	return map.get(d.getId());
    }

    private boolean isFresh(DataType t, Object id) {
    	Map<Object, Object> map = this.buckets.get(t);
    	if (map == null)
    		return true;
    	
    	List<DataValue> potDet = getPotsOfCollection(t, () -> map.values());
    	List<DataValue> potUndet = getPotsOfCollection(t, () -> map.values());
    	boolean valueInPot =  potDet.stream().anyMatch(potVal -> id.equals(potVal.getId()));
    	valueInPot = valueInPot || potUndet.stream().anyMatch(potVal -> potVal.equals(potVal.getId())); 
        return ! valueInPot;
    }
    
    private Origin getOriginOfValueFromCollection(DataType t, Object val, Supplier<Collection<Object>> colProvider) {
    	List<DataValue> pots = getPotsOfCollection(t, colProvider);
    	List<DataValue> relatedDVs = pots.stream().filter(potVal -> val.equals(potVal.getId())).collect(Collectors.toList());
    	return relatedDVs.isEmpty() ? null : new Origin(relatedDVs.get(0), pots.indexOf(relatedDVs.get(0)));
    }
    
    static class Origin {
    	public final DataValue dv;
    	public final Integer indexInPots;
		public Origin(DataValue dv, Integer indexInPots) {
			super();
			this.dv = dv;
			this.indexInPots = indexInPots;
		}
    	
    }
    
    private List<DataValue> getPotsOfCollection(DataType t, Supplier<Collection<Object>> valProvider) {
    	List<DataValue> dataValues = valProvider.get().
    			stream().map(obj -> new DataValue<>(t, obj)).collect(Collectors.toList());
    	Theory teach = theories.get(t);
    	List<DataValue> pot = teach.getPotential(dataValues);
    	return pot;
    } 
    
    private FreshValue registerFreshValue(DataType retType, Object ret) {
    	LinkedHashMap<Object, Object> map = this.buckets.get(retType);
        if (map == null) {
            map = new LinkedHashMap<>();
            this.buckets.put(retType, map);
        }
        
        final LinkedHashMap<Object, Object> fmap = map;
        //Object detFreshValue = DataValue.cast(map.size(), retType);
        List<DataValue> dataValues = fmap.keySet().stream().
        		map(v -> new DataValue(retType, v)).collect(Collectors.toList());
        DataValue freshDv = this.theories.get(retType)
        		.getFreshValue(dataValues);
        map.put(freshDv.getId(), ret);
        return new FreshValue(retType, freshDv.getId());    
    }
}
