package de.learnlib.ralib.oracles.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.FreshValue;
import de.learnlib.ralib.data.SumConstants;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.SumConstant;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.oracles.io.IOCache.CacheNode;
import de.learnlib.ralib.theory.inequality.IntervalDataValue;
import de.learnlib.ralib.theory.inequality.SumCDataValue;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;


public interface IOCacheManager {
	
	
	public IOCache loadCacheFromFile(String file, Constants consts)  throws IOException ;
	public void dumpCacheToFile(String file, IOCache cache, Constants consts)  throws IOException ;
	
	
	public static IOCacheManager getCacheManager(String caching) {
		switch(caching){
		case "serialize":
			return new JavaSerializeCacheManager();
		default: 
			return new MockCacheManager();
		}
	}
	
	
	static class MockCacheManager implements IOCacheManager {

		public IOCache loadCacheFromFile(String file, Constants consts) throws IOException {
			return new IOCache();
		}

		@Override
		public void dumpCacheToFile(String file, IOCache cache, Constants consts) throws IOException {
	//		nothing
		}
		
	}
	
	
	/**
	 * An ugly manager for implementing a serializable cache cloning the cache and all its constituent
	 * objects to Serializable objects.
	 * Hardly ideal, this should be implementable with a good serializing framework.
	 */
	static class JavaSerializeCacheManager implements IOCacheManager {

		public IOCache loadCacheFromFile(String fileName, Constants consts) throws IOException {
			InputStream file = new FileInputStream(fileName);
			InputStream buffer = new BufferedInputStream(file);
 			ObjectInput input = new ObjectInputStream (buffer);
			try {
				SerializableCacheNode cacheNode = (SerializableCacheNode) input.readObject();
				input.close();
				return new IOCache(cacheNode.toCacheNode(consts));
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				input.close();
				throw new RuntimeException(e);
			}
			
			
		}

		@Override
		public void dumpCacheToFile(String fileName, IOCache cache, Constants consts) throws IOException {
			SerializableCacheNode cacheNode = new SerializableCacheNode(cache.getRoot(), consts);
			OutputStream file = new FileOutputStream(fileName);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			output.writeObject(cacheNode);
			output.close();
		}
		
		static class SerializableCacheNode implements Serializable{
			private static final long serialVersionUID = 1L;
	        final Map<SerializablePSymbolInstance, SerializablePSymbolInstance> output;
	        final Map<SerializablePSymbolInstance, SerializableCacheNode> next;
	        
	        public SerializableCacheNode(IOCache.CacheNode cache, Constants consts) {
		        this.output = new LinkedHashMap<>();
		        this.next = new LinkedHashMap<>();
	        	cache.output.forEach((inp,out) 
	        			-> this.output.put(new SerializablePSymbolInstance(inp, consts), new SerializablePSymbolInstance(out, consts)));
	        	cache.next.forEach((inp,node) 
	        			-> this.next.put(new SerializablePSymbolInstance(inp, consts), new SerializableCacheNode(node, consts)));
	        	//System.out.println("Storing: "+ cache);
	        }
	        
	        public IOCache.CacheNode toCacheNode(Constants consts) {
	        	IOCache.CacheNode cache = new CacheNode();
	        	this.output.forEach((inp,out) 
	        			-> cache.output.put(inp.toPSymbolInstance(consts), out.toPSymbolInstance(consts)));
	        	this.next.forEach((inp,node) 
	        			-> cache.next.put(inp.toPSymbolInstance(consts), node.toCacheNode(consts)));
	        	//System.out.println("Loading: "+ cache);
	        	return cache;
	        }
		}
		
		static class SerializablePSymbolInstance implements Serializable {
			private static final long serialVersionUID = 1L;
			public SerializableParameterSymbol baseSymbol;
			private SerializableDataValue[] dvs;
			
			SerializablePSymbolInstance(PSymbolInstance inst, Constants consts) {
				baseSymbol = new SerializableParameterSymbol(inst.getBaseSymbol());
				dvs = (SerializableDataValue[]) Arrays.stream(inst.getParameterValues()).map(val -> visit(val, consts)).toArray(SerializableDataValue []::new);
			}
			
			public PSymbolInstance toPSymbolInstance(Constants consts) {
				DataValue[] dataValues = Arrays.stream(dvs).map(dvs -> dvs == null? null:dvs.toDataValue(consts)).toArray(DataValue []::new);
				return new PSymbolInstance(baseSymbol.toPSym(), dataValues);
			}
			
			<T> SerializableDataValue<T>   visit(DataValue<T> dv, Constants consts) {
				if (dv == null) {
					return null;
				}
				if (dv instanceof Constant) {
					return new ConstantSerializableDataValue<T>((Constant) dv, consts);
				} 
				
				if (dv instanceof SumCDataValue) {
					return new SumCSerializableDataValue<T>(visit(((SumCDataValue<T>) dv).getOperand(), consts), 
							new SumConstantSerializableDataValue<T>(((SumCDataValue<T>) dv).getConstant(), consts));
				}
				if (dv instanceof FreshValue) {
					return new FreshSerializableDataValue<T>(dv);
				}
				if (dv instanceof IntervalDataValue) {
					return new IntervalSerializableDataValue(
							visit(new DataValue( dv.getType(), dv.getId()), consts), 
							visit(((IntervalDataValue) dv).getLeft(), consts), 
							visit(((IntervalDataValue) dv).getRight(), consts));
				}
				if (dv.getClass() != DataValue.class) {
					throw new RuntimeException("Serialization not implemented for DataValue of subtype: " + dv.getClass());
				}
				return new BasicSerializableDataValue<T>(dv);
			}
		}
		
		static class SerializableParameterSymbol implements Serializable {
			private static final long serialVersionUID = 1L;
			private String name;
			private SerializableDataType[] pTypes;
			private boolean input;
			
			SerializableParameterSymbol(ParameterizedSymbol sym ) {
				this.pTypes = Arrays.stream(sym.getPtypes()).map(pType -> new SerializableDataType(pType)).toArray(SerializableDataType []::new);
				this.input = sym instanceof InputSymbol;
				this.name = sym.getName();
				
			} 
			SerializableParameterSymbol(String name, SerializableDataType [] pTypes, boolean input) {
				this.name = name;
				this.pTypes = pTypes;
				this.input = input;
			}
			
			public ParameterizedSymbol toPSym() {
				DataType [] dataTypes = Arrays.stream(pTypes).map(pType -> pType.toDataType()).toArray(DataType []::new);
				if (input)
					return new InputSymbol(name, dataTypes);
				else 
					return new OutputSymbol(name, dataTypes);
			}
			
		}
		
		
		static class FreshSerializableDataValue<T> extends BasicSerializableDataValue<T> {

			public FreshSerializableDataValue(DataValue<T> dataValue) {
				super(dataValue);
			}

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			
			public DataValue<T> toDataValue(Constants consts) {
				DataValue<T> dv = super.toDataValue(consts);
				return new FreshValue<T>(dv.getType(), dv.getId());
			}
			
		}
		
		static class SumCSerializableDataValue<T> implements SerializableDataValue<T>  {

			private SerializableDataValue<T>  opValue;
			private SerializableDataValue<T>  constValue;

			public SumCSerializableDataValue(SerializableDataValue<T>   opValue, SerializableDataValue<T>   constValue) {
				this.opValue = opValue;
				this.constValue = constValue;
				
			}

			private static final long serialVersionUID = 1L;
			
			public DataValue<T> toDataValue(Constants consts) {
				return new SumCDataValue<T>(opValue.toDataValue(consts), constValue.toDataValue(consts));
			}
		}
		
		static class IntervalSerializableDataValue<T extends Comparable<T>> implements SerializableDataValue<T>  {

			private SerializableDataValue<T>  val;
			private SerializableDataValue<T>  min;
			private SerializableDataValue<T>  max;

			public IntervalSerializableDataValue(SerializableDataValue<T>   val, SerializableDataValue<T>   min, SerializableDataValue<T>   max) {
				this.val = val;
				this.min = min;
				this.max = max;
			}

			private static final long serialVersionUID = 1L;
			
			public DataValue<T> toDataValue(Constants consts) {
				return new IntervalDataValue<T>(val.toDataValue(consts), min == null? null : 
					min.toDataValue(consts), max == null? null : max.toDataValue(consts));
			}
		}
		
		static class BasicSerializableDataValue<T> implements SerializableDataValue<T> {
			private static final long serialVersionUID = 1L;
			SerializableDataType<T> dataType;
			T object;
			Class<? extends DataValue> dvType;
			
			
			public BasicSerializableDataValue(DataValue<T> dataValue) {
				dataType = new SerializableDataType<T>(dataValue.getType());
				object = dataValue.getId();
				dvType = dataValue.getClass();
			}
			
			public DataValue<T> toDataValue(Constants consts) {
				return new DataValue<T>(dataType.toDataType(), object);
			}
		}
		
		static class ConstantSerializableDataValue<T> implements SerializableDataValue<T> {
			private static final long serialVersionUID = 1L;
			private Integer cIdx;

			public ConstantSerializableDataValue(Constant constant, Constants consts){
				this.cIdx = constant.getId();
			}

			@Override
			public DataValue<T> toDataValue(Constants consts) {
				Optional<Constant> symConst = consts.keySet()
						.stream().filter(c -> c.getId().equals(cIdx)).findFirst();
				if (!symConst.isPresent()) {
					throw new DecoratedRuntimeException("Constant with id " + cIdx + " not found in constants. " 
							+ "Ensure that the current configuration uses the same constant setup as the previous")
					.addDecoration("constants", consts);
				} 
				return (DataValue<T>) symConst.get();
			}
		}

		
		static class SumConstantSerializableDataValue<T> implements SerializableDataValue<T> {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			private Integer cIdx;

			public SumConstantSerializableDataValue(DataValue<T> constant, Constants consts){
				SumConstants sumCs = consts.getSumCs();
				Optional<SumConstant> symConst = sumCs.keySet()
				.stream().filter(c -> sumCs.get(c).equals(constant)).findFirst();
				assert symConst.isPresent();
				this.cIdx = symConst.get().getId();
			}

			@Override
			public DataValue<T> toDataValue(Constants consts) {
				SumConstants sumCs = consts.getSumCs();
				Optional<SumConstant> symConst = sumCs.keySet()
						.stream().filter(c -> c.getId().equals(cIdx)).findFirst();
				if (!symConst.isPresent()) {
					throw new DecoratedRuntimeException("Constant with id " + cIdx + " not found in constants. " 
							+ "Ensure that the current configuration uses the same constant setup as the previous")
					.addDecoration("constants", consts);
				} 
				return (DataValue<T>) sumCs.get(symConst.get());
			}
		}
		
		
		static interface SerializableDataValue<T> extends Serializable {
			public DataValue<T> toDataValue(Constants consts);
		}
		
		static class SerializableDataType<T> implements Serializable{

			private static final long serialVersionUID = 1L;
			private Class<T> base;
			private String name;
			public SerializableDataType(DataType<T> dataType) {
				this.base = dataType.getBase();
				this.name = dataType.getName();
			}
			
			public DataType<T> toDataType() {
				return new DataType<T>(name, base);
			}
		} 
		
		static <F,T> T [] arrayMap(F [] array, Function<F,T> func, IntFunction<T[]> generator) {
			return Arrays.stream(array).map(func).toArray(generator);
		}
		
	}
	
	// experiments with kryo, maybe these can be resumed
//	
//	static class KryoCacheManager implements IOCacheManager{
//	
//		private Kryo kryo;
//
//		public KryoCacheManager() {
//			this.kryo = new Kryo();
//			kryo.register(IOCache.class);
//		}
//		
//		public IOCache loadCacheFromFile(String file) throws FileNotFoundException {
//			com.esotericsoftware.kryo.io.Input buffer = new com.esotericsoftware.kryo.io.Input(1024* 20);
//			FileInputStream fileInputStream = new FileInputStream(file);
//			buffer.setInputStream(fileInputStream);
//			IOCache ioCache = kryo.readObject(buffer, IOCache.class);
//			return ioCache;
//		}
//		
//		public void dumpCacheToFile(String file, IOCache cache) throws FileNotFoundException {
//			com.esotericsoftware.kryo.io.Output buffer = new com.esotericsoftware.kryo.io.Output(1024* 20);
//			FileOutputStream fileOutputStream = new FileOutputStream(file);
//			buffer.setOutputStream(fileOutputStream);
//			kryo.writeObject(buffer, cache);
//			buffer.flush();
//		}
//		
//		static class Pair{
//			@BindMap(
//				 valueSerializer = DefaultSerializers.StringSerializer.class, 
//					keySerializer = DefaultSerializers.IntSerializer.class, 
//			                valueClass = Integer.class, 
//			                keyClass = String.class, 
//			                keysCanBeNull = false
//			)
//			LinkedHashMap<Integer, String> a;
//			
//			@BindMap(
//					 valueSerializer = DefaultSerializers.IntSerializer.class, 
//						keySerializer = DefaultSerializers.DoubleSerializer.class, 
//				                valueClass = Integer.class, 
//				                keyClass = Double.class, 
//				                keysCanBeNull = false
//				)
//			LinkedHashMap<Double, Integer> b;
//			public Pair() {
//				a = null;
//				b = null;
//			}
//			public Pair(LinkedHashMap<Integer, String>  a, LinkedHashMap<Double, Integer> b) {
//				this.a = a;
//				this.b = b;
//			}
//			
//			public String toString() {
//				return a.toString() + b.toString();
//			}
//		}
//		
//		public static void main(String args []) throws Exception{
//			Kryo kryo = new Kryo();
////			MapSerializer serializer = new MapSerializer();
////			serializer.setKeyClass(Integer.class, new DefaultSerializers.IntSerializer ());
////			serializer.setValueClass(String.class, kryo.getDefaultSerializer(String.class));
////			//serializer.setKeyClass(Integer.class, new DefaultSerializers.IntSerializer ());
//////			serializer.setKeyClass(PSymbolInstance.class, new DefaultSerializers.DoubleSerializer ());
//////			serializer.
//////			serializer.setValueClass(String.class, kryo.getDefaultSerializer(String.class));
////			kryo.register(HashMap.class, serializer);
////			kryo.register(LinkedHashMap.class, serializer);
//			kryo.register(Pair.class);
//			
//			
//			
//			
//			//kryo.register(Map.class, serializer);
//			LinkedHashMap<Integer, String> test = new LinkedHashMap<Integer, String>();
//			test.put(1, "1");
//			test.put(2, "2");
//			test.put(3, "3");
//			LinkedHashMap<Double, Integer> test2 = new LinkedHashMap<Double, Integer>();
//			test2.put(1.0, 1);
//			test2.put(2.0, 2);
//			test2.put(3.0, 2);
//			Pair pair = new Pair(test,test2);
//			
//			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//			com.esotericsoftware.kryo.io.Output outputBuffer = new com.esotericsoftware.kryo.io.Output(1024* 20);
//			outputBuffer.setOutputStream(outputStream);
//			kryo.writeObject(outputBuffer, pair);
//			byte [] outputBytes = outputBuffer.toBytes();
//			System.out.println(outputBytes.length);
//			ByteArrayInputStream inputStream = new ByteArrayInputStream(outputBytes);
//			com.esotericsoftware.kryo.io.Input inputBuffer = new com.esotericsoftware.kryo.io.Input(1024* 20);
//			inputBuffer.setInputStream(inputStream);
//			Pair map = kryo.readObject(inputBuffer, Pair.class);
//			
//			System.out.println(map);
//		}
//	}
	
//	public static void main(String args []) {
//		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//		PrintStream out = System.out;
//		String cacheFile = args.length > 0 ? args[0] : "dump.ser";
//		IOCacheManager cm = IOCacheManager.getCacheManager("java");
//		Constants consts = new Constants();
//		IOCache cache = cm.loadCacheFromFile(cacheFile, consts);
//		// useless opt fun
//		out.print("Cache loaded: " + cacheFile);
//		boolean exit = false;
//		do {
//			out.print("Operations Available: \n"
//					+ "0. Load DB \n"
//					+ "1. Remove all entries with prefix \n"
//					+ "2. Remove all entries with output \n"
//					+ "3. Dump new cache \n"
//					+ "4. Exit (Default Operation) \n" );
//			int read = Integer.valueOf(br.readLine());
//			int rowCount;
//			String ans;
//			switch(read) {
//			case 0:
//				out.println("Load database");
//				out.println("Database name:");
//				cacheFile = br.readLine();
//				cache = cm.loadCacheFromFile(cacheFile, consts);
//				out.print("Database loaded: " + cacheFile);
//				break;
//			case 1:
//				out.println("Enter prefix" );
//				String prefix = br.readLine().trim();
//				TraceParser traceParser = new TraceParser(prefix);
//				break;
//			case 2:
//                out.println("Selected Column from "  + columns);
//                column = br.readLine().trim().toLowerCase();
//                if (!columns.contains(column)) {
//                    System.err.println("Column " + column + " does not exist");
//                    continue;
//                } else {
//                    out.println("Selected "+ column + " Update");
//                    out.println("Old String occurrence (empty means all entries):");
//                    String oldVal = br.readLine();
//                    out.println("New Value:");
//                    String newVal = br.readLine();
//                    rowCount = logger.replaceColumn(column, newVal, oldVal);
//                    out.println(rowCount + " rows updated");
//                }
//			case 3:
//				out.println("Selected Entry Removal");
//				out.println("Prefix of traces to be removed:");
//				String prefix = br.readLine();
//				rowCount = logger.deleteEntriesForTracesWithPrefix(prefix);
//				out.println(rowCount + " rows removed");
//				break;
//			case 4:
//				out.println("Selected Output Removal");
//				out.println("Output for traces to be removed:");
//				String output = br.readLine();
//				rowCount = logger.deleteEntriesForTracesWithOutput(output);
//				out.println(rowCount + " rows removed");
//				break;
//			case 5:
//			    out.println("Query fortrace");
//			    out.println("Prefix for traces to be selected:");
//                prefix = br.readLine();
//                ans = logger.query(prefix);
//                out.println(ans);
//			    break;
//			
//			case 6:
//			    out.println("Get existing traces with prefix");
//			    out.println("Prefix for traces to be selected:");
//                prefix = br.readLine();
//                List<String> resSel = logger.selectTracesWithPrefix(prefix);
//                out.println(resSel);
//                out.println("Num Traces: " + resSel.size());
//			    break;
//			case 7:
//			    out.println("Give databases to normalize sep. by ;");
//			    String[] dbs = br.readLine().split(";");
//			    // to make alphabets consistent again
//			    for (String db : dbs) {
//			        Logger dbLogger = new Logger(db.trim());
//			        int row = 0;
//			        row =+dbLogger.replaceColumn("sequence", "UNIMPLEMENTED", "UNIMPL");
//			        row =+dbLogger.replaceColumn("sequence", "SERVICE_REQUEST_AUTH", "SR_AUTH");
//			        row =+dbLogger.replaceColumn("sequence", "SERVICE_REQUEST_CONN", "SR_CONN");
//			        row =+dbLogger.replaceColumn("result", "UNIMPLEMENTED", "UNIMPL");
//			        row =+dbLogger.replaceColumn("result", "SERVICE_ACCEPT", "SR_ACCEPT");
//			        row =+dbLogger.replaceColumn("result", "+", "_");
//			        System.out.println(row + "rows changed in " + db);
//			    }
//			    break;
//			default:
//				out.println("Selected Exit");
//				logger.close();
//				exit = true;
//			}
//			
//		} while(!exit);
//	}
}
