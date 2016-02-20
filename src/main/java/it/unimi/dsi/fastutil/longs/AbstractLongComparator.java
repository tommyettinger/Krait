/* Generic definitions */
/* Assertions (useful to generate conditional code) */
/* Current type and class (and size, if applicable) */
/* Value methods */
/* Interfaces (keys) */
/* Interfaces (values) */
/* Abstract implementations (keys) */
/* Abstract implementations (values) */
/* Static containers (keys) */
/* Static containers (values) */
/* Implementations */
/* Synchronized wrappers */
/* Unmodifiable wrappers */
/* Other wrappers */
/* Methods (keys) */
/* Methods (values) */
/* Methods (keys/values) */
/* Methods that have special names depending on keys (but the special names depend on values) */
/* Equality */
/* Object/Reference-only definitions (keys) */
/* Primitive-type-only definitions (keys) */
/* Object/Reference-only definitions (values) */
/*		 
 * Copyright (C) 2002-2015 Sebastiano Vigna
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
package it.unimi.dsi.fastutil.longs;

/** An abstract class facilitating the creation of type-specific {@linkplain java.util.Comparator comparators}.
 *
 * <P>To create a type-specific comparator you need both a method comparing primitive types and a method comparing objects. However, if you have the first one you can just inherit from this class and
 * get for free the second one.
 * 
 * @see java.util.Comparator */
public abstract class AbstractLongComparator implements LongComparator, java.io.Serializable {
	private static final long serialVersionUID = 0L;

	protected AbstractLongComparator() {}

	public int compare(Long ok1, Long ok2 ) {
		return compare( ok1.longValue(), ok2.longValue() );
	}

	public abstract int compare( long k1, long k2 );
}
