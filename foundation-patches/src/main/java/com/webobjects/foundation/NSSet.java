package com.webobjects.foundation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * <div class="en"> NSSet reimplementation to support JDK 1.5 templates. Use
 * with </div>
 *
 * <div class="ja"> JDK 1.5 テンプレートをサポートする為の再実装。使用は </div>
 *
 * <pre>{@code
 * NSSet<E> setA = new NSSet<E>(NSArray < E > listA);
 * NSSet<E> setB = new NSSet<E>(NSArray < E > listB);
 * logger.debug("intersection contains " + setA.setByIntersectingSet(setB));
 * }</pre>
 *
 * @param <E> - type of set contents
 */
public class NSSet<E> implements Cloneable, Serializable, NSCoding, _NSFoundationCollection, Set<E> {

	static final long serialVersionUID = -8833684352747517048L;

	public static final Class _CLASS = _NSUtilities._classWithFullySpecifiedName("com.webobjects.foundation.NSSet");

	protected static int _NSSetClassHashCode = _CLASS.hashCode();

	public static final NSSet EmptySet = new NSSet();

	private static final String SerializationValuesFieldKey = "objects";
	private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[] {
			new ObjectStreamField(SerializationValuesFieldKey, _NSUtilities._NoObjectArray.getClass()) };

	public static Object decodeObject(final NSCoder coder) {
		return new NSSet<>(coder.decodeObjects());
	}

	protected transient int _capacity;

	protected transient int _count;

	protected transient int _deletionLimit;

	protected transient byte[] _flags;

	protected transient int _hashCache;

	protected transient int _hashtableBuckets;

	protected Object[] _objects;

	protected transient Object[] _objectsCache;

	public NSSet() {
		_initializeSet();
	}

	public NSSet(final Collection<? extends E> collection) {
		final Object[] objects = collection.toArray();
		initFromObjects(objects, true);
	}

	public NSSet(final NSArray<? extends E> objects) {
		this(objects == null ? null : (E[]) objects.objectsNoCopy(), false);
	}

	public NSSet(final NSSet<? extends E> otherSet) {
		this(otherSet == null ? null : (E[]) otherSet.objectsNoCopy(), false);
	}

	public NSSet(final Set<? extends E> set, final boolean ignoreNull) {
		if (set == null) {
			throw new IllegalArgumentException("Set cannot be null");
		}

		if (!ignoreNull && set.contains(null)) {
			throw new IllegalArgumentException("Attempt to insert null into an  " + getClass().getName() + ".");
		}

		final Object[] aSet = set.toArray();
		initFromObjects(aSet, !ignoreNull);
	}

	public NSSet(final E object) {
		if (object == null) {
			throw new IllegalArgumentException("Attempt to insert null into an  " + getClass().getName() + ".");
		}
		_initializeSet();
		_ensureCapacity(1);
		if (_NSCollectionPrimitives.addValueToSet(object, _objects, _flags)) {
			_setCount(count() + 1);
		}
	}

	public NSSet(final E[] objects) {
		this(objects, true);
	}

	public NSSet(final E object, final E... objects) {
		this(objects, true);
		_ensureCapacity(count() + 1);
		if (_NSCollectionPrimitives.addValueToSet(object, _objects, _flags)) {
			_setCount(count() + 1);
		}
	}

	private NSSet(final E[] objects, final boolean checkForNull) {
		if (objects == null) {
			throw new IllegalArgumentException("Objects cannot be null.");
		}
		initFromObjects(objects, checkForNull);
	}

	public Object[] _allObjects() {
		final int count = count();
		final Object[] objects = new Object[count];
		if (count > 0) {
			System.arraycopy(objectsNoCopy(), 0, objects, 0, count);
		}
		return objects;
	}

	protected void _clearDeletionsAndCollisions() {
		final int size = _hashtableBuckets;
		if (count() == 0) {
			_flags = new byte[size];
		} else {
			final Object[] oldObjects = _objects;
			final byte[] oldFlags = _flags;
			_objects = new Object[size];
			_flags = new byte[size];
			for (int i = 0; i < size; i++) {
				if ((oldFlags[i] & 0xffffffc0) == -128) {
					_NSCollectionPrimitives.addValueToSet(oldObjects[i], _objects, _flags);
				}
			}

		}
		_deletionLimit = _NSCollectionPrimitives.deletionLimitForTableBuckets(size);
	}

	protected void _ensureCapacity(final int capacity) {
		final int currentCapacity = capacity();
		if (capacity > currentCapacity) {
			final int newCapacity = _NSCollectionPrimitives.hashTableCapacityForCapacity(capacity);
			if (newCapacity != currentCapacity) {
				final int oldSize = _hashtableBuckets;
				_setCapacity(newCapacity);
				_hashtableBuckets = _NSCollectionPrimitives.hashTableBucketsForCapacity(newCapacity);
				final int newSize = _hashtableBuckets;
				if (newSize == 0) {
					_objects = null;
					_flags = null;
				} else {
					final Object[] oldObjects = _objects;
					final byte[] oldFlags = _flags;
					_objects = new Object[newSize];
					_flags = new byte[newSize];
					for (int i = 0; i < oldSize; i++) {
						if ((oldFlags[i] & 0xffffffc0) == -128) {
							_NSCollectionPrimitives.addValueToSet(oldObjects[i], _objects, _flags);
						}
					}

				}
				_deletionLimit = _NSCollectionPrimitives.deletionLimitForTableBuckets(newSize);
			}
		}
	}

	private boolean _equalsSet(final NSSet<?> otherSet) {
		final int count = count();
		if (count != otherSet.count()) {
			return false;
		}
		final Object[] objects = objectsNoCopy();
		for (int i = 0; i < count; i++) {
			if (otherSet.member(objects[i]) == null) {
				return false;
			}
		}

		return true;
	}

	protected void _initializeSet() {
		_capacity = _count = 0;
		_objects = _objectsCache = null;
		_flags = null;
		_hashtableBuckets = _NSCollectionPrimitives.hashTableBucketsForCapacity(_capacity);
		_deletionLimit = _NSCollectionPrimitives.deletionLimitForTableBuckets(_hashtableBuckets);
	}

	@Override
	public int _shallowHashCode() {
		return _NSSetClassHashCode;
	}

	@Override
	public boolean add(final E o) {
		throw new UnsupportedOperationException("add is not a supported operation in com.webobjects.foundation.NSSet");
	}

	@Override
	public boolean addAll(final Collection<? extends E> c) {
		throw new UnsupportedOperationException(
				"addAll is not a supported operation in com.webobjects.foundation.NSSet");
	}

	public NSArray<E> allObjects() {
		return new NSArray<>((E[]) objectsNoCopy());
	}

	public E anyObject() {
		return count() <= 0 ? null : (E) objectsNoCopy()[0];
	}

	@Override
	public Class classForCoder() {
		return _CLASS;
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException(
				"clear is not a supported operation in com.webobjects.foundation.NSSet");
	}

	@Override
	public Object clone() {
		return this;
	}

	@Override
	public boolean contains(final Object o) {
		return containsObject(o);
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		if (c == null) {
			throw new NullPointerException("Collection passed into containsAll() cannot be null");
		}
		final Object[] objects = c.toArray();
		if (objects.length > 0) {
			for (final Object object : objects) {
				if ((object == null) || (member(object) == null)) {
					return false;
				}
			}

		}
		return true;
	}

	public boolean containsObject(final Object object) {
		return object == null ? false : member(object) != null;
	}

	public int count() {
		return _count;
	}

	protected void _setCount(final int count) {
		_count = count;
	}

	protected int capacity() {
		return _capacity;
	}

	protected void _setCapacity(final int capacity) {
		_capacity = capacity;
	}

	@Override
	public void encodeWithCoder(final NSCoder coder) {
		coder.encodeObjects(objectsNoCopy());
	}

	@SuppressWarnings("cast")
	public static <T> NSSet<T> emptySet() {
		return EmptySet;
	}

	/**
	 * Returns an immutable empty {@code NSSet}.
	 *
	 * @param <E> the {@code NSSet}'s element type
	 * @return an empty {@code NSSet}
	 */
	public static <E> NSSet<E> of() {
		return EmptySet;
	}

	/**
	 * Returns an immutable {@code NSSet} containing one element.
	 *
	 * @param <E>     the {@code NSSet}'s element type
	 * @param element the element to be contained in the array
	 * @return a {@code NSSet} containing the specified element
	 */
	public static <E> NSSet<E> of(final E element) {
		return new NSSet<>(element);
	}

	/**
	 * Returns an immutable {@code NSSet} containing an arbitrary number of
	 * elements.
	 *
	 * @param <E>      the {@code NSSet}'s element type
	 * @param elements the elements to be contained in the array
	 * @return a {@code NSSet} containing the specified elements
	 */
	@SafeVarargs
	public static <E> NSSet<E> of(final E... elements) {
		if (elements.length == 0) {
			return EmptySet;
		}
		if (elements.length == 1) {
			return new NSSet(elements[0]);
		}

		return new NSSet<>(elements);
	}

	@Override
	public boolean equals(final Object object) {
		if (object == this) {
			return true;
		}
		if (object instanceof NSSet) {
			return _equalsSet((NSSet<?>) object);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return _NSSetClassHashCode ^ count();
	}

	public HashSet<E> hashSet() {
		final E[] objects = (E[]) objectsNoCopy();
		final HashSet<E> set = new HashSet<>(objects.length);
		Collections.addAll(set, objects);

		return set;
	}

	public NSSet<E> immutableClone() {
		return this;
	}

	private void initFromObjects(final Object[] objects, final boolean checkForNull) {
		_initializeSet();
		_ensureCapacity(objects.length);
		for (final Object object : objects) {
			if (object == null) {
				if (checkForNull) {
					throw new IllegalArgumentException(
							"Attempt to insert null object into an  " + getClass().getName() + ".");
				}
			} else if (_NSCollectionPrimitives.addValueToSet(object, _objects, _flags)) {
				_setCount(count() + 1);
			}
		}

	}

	public boolean intersectsSet(final NSSet<?> otherSet) {
		if (count() != 0 && otherSet != null && otherSet.count() != 0) {
			final Object[] objects = objectsNoCopy();
			for (final Object object : objects) {
				if (otherSet.member(object) != null) {
					return true;
				}
			}

		}
		return false;
	}

	@Override
	public boolean isEmpty() {
		return count() == 0;
	}

	public boolean isEqualToSet(final NSSet<?> otherSet) {
		if (otherSet == null) {
			return false;
		}
		if (otherSet == this) {
			return true;
		}

		return _equalsSet(otherSet);
	}

	public boolean isSubsetOfSet(final NSSet<?> otherSet) {
		final int count = count();
		if (otherSet == null || otherSet.count() < count) {
			return false;
		}
		if (count == 0) {
			return true;
		}
		final Object[] objects = objectsNoCopy();
		for (final Object object : objects) {
			if (otherSet.member(object) == null) {
				return false;
			}
		}

		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<E> iterator() {
		return new _NSJavaSetIterator(objectsNoCopy());
	}

	public E member(final Object object) {
		return count() != 0 && object != null
				? (E) _NSCollectionPrimitives.findValueInHashTable(object, _objects, _objects, _flags)
				: null;
	}

	public NSMutableSet<E> mutableClone() {
		return new NSMutableSet<>(this);
	}

	@SuppressWarnings("unchecked")
	public Enumeration<E> objectEnumerator() {
		return new _NSCollectionEnumerator(_objects, _flags, count());
	}

	protected Object[] objectsNoCopy() {
		if (_objectsCache == null) {
			_objectsCache = count() != 0
					? _NSCollectionPrimitives.valuesInHashTable(_objects, _objects, _flags, capacity(),
							_hashtableBuckets)
					: _NSCollectionPrimitives.EmptyArray;
		}
		return _objectsCache;
	}

	private void readObject(final ObjectInputStream s) throws IOException, ClassNotFoundException {
		java.io.ObjectInputStream.GetField fields = s.readFields();
		Object[] keys = (Object[]) fields.get(SerializationValuesFieldKey, _NSUtilities._NoObjectArray);
		keys = keys != null ? keys : _NSUtilities._NoObjectArray;
		initFromObjects(keys, true);
	}

	private Object readResolve() throws ObjectStreamException {
		if (getClass() == _CLASS && count() == 0) {
			return EmptySet;
		}

		return this;
	}

	@Override
	public boolean remove(final Object o) {
		throw new UnsupportedOperationException(
				"remove is not a supported operation in com.webobjects.foundation.NSSet");
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		throw new UnsupportedOperationException(
				"removeAll is not a supported operation in com.webobjects.foundation.NSSet");
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		throw new UnsupportedOperationException(
				"retainAll is not a supported operation in com.webobjects.foundation.NSSet");
	}

	public NSSet<E> setByIntersectingSet(final NSSet<?> otherSet) {
		final NSMutableSet<E> set = new NSMutableSet<>(this);
		set.intersectSet(otherSet);
		return set;
	}

	public NSSet<E> setBySubtractingSet(final NSSet<?> otherSet) {
		final NSMutableSet<E> set = new NSMutableSet<>(this);
		set.subtractSet(otherSet);
		return set;
	}

	public NSSet<E> setByUnioningSet(final NSSet<? extends E> otherSet) {
		final NSMutableSet<E> set = new NSMutableSet<>(this);
		set.unionSet(otherSet);
		return set;
	}

	@Override
	public int size() {
		return count();
	}

	@Override
	public Object[] toArray() {
		final Object[] currObjects = objectsNoCopy();
		final Object[] objects = new Object[currObjects.length];
		if (currObjects.length > 0) {
			System.arraycopy(currObjects, 0, objects, 0, currObjects.length);
		}
		return objects;
	}

	@Override
	public <T> T[] toArray(T[] objects) {
		if (objects == null) {
			throw new NullPointerException("Cannot pass null as parameter");
		}
		final Object[] currObjects = objectsNoCopy();
		if (objects.length < currObjects.length) {
			objects = (T[]) java.lang.reflect.Array.newInstance(objects.getClass().getComponentType(),
					currObjects.length);
		}
		System.arraycopy(currObjects, 0, objects, 0, currObjects.length);
		return objects;
	}

	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder(128);
		buffer.append('(');
		final Object[] objects = objectsNoCopy();
		for (int i = 0; i < objects.length; i++) {
			final Object object = objects[i];
			if (i > 0) {
				buffer.append(", ");
			}
			if (object instanceof String) {
				buffer.append('"');
				buffer.append((String) object);
				buffer.append('"');
				continue;
			}
			if (object instanceof Boolean) {
				buffer.append(((Boolean) object).booleanValue() ? "true" : "false");
			} else {
				buffer.append(object.toString());
			}
		}

		buffer.append(')');
		return buffer.toString();
	}

	private void writeObject(final ObjectOutputStream s) throws IOException {
		final java.io.ObjectOutputStream.PutField fields = s.putFields();
		fields.put(SerializationValuesFieldKey, _allObjects());
		s.writeFields();
	}
}
