/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.theory;

import java.util.EnumSet;

/**
 *
 */
public enum DataRelation {
    ALL, /* This is a special relation meaning that the relation is undetermined, it can be one of any of the relations recognized.
    TODO We should consider removing this data relation and in its place, use getAllRecognizedRelations. 
     */
    EQ,
    EQ_SUMC1,
    EQ_SUMC2,
    DEQ,
    DEQ_SUMC1,
    DEQ_SUMC2,
    LT, 
    LT_SUMC1,
    LT_SUMC2,
    DEFAULT;
    
	
	public static final EnumSet<DataRelation> DEQ_DEF_RELATIONS = EnumSet.of(DEQ, DEQ_SUMC1, DEQ_SUMC2, DEFAULT);
	public static final EnumSet<DataRelation> EQ_RELATIONS = EnumSet.of(EQ, EQ_SUMC1, EQ_SUMC2);
	public static final EnumSet<DataRelation> LT_RELATIONS = EnumSet.of(LT, LT_SUMC1, LT_SUMC2); 
	public static final EnumSet<DataRelation> EQ_DEQ_DEF_RELATIONS = EnumSet.of(EQ, EQ_SUMC1, EQ_SUMC2, DEQ, DEQ_SUMC1, DEQ_SUMC2, DEFAULT); 
	
    public boolean isEq() {
    	return super.name().startsWith("EQ");
    }
}
