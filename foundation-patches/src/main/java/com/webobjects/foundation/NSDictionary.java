package com.webobjects.foundation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <div class="en"> NSDictionary reimplementation to support JDK 1.5 templates.
 * Use with </div>
 *
 * <div class="ja"> JDK 1.5 テンプレートをサポートする為の再実装。使用は </div>
 *
 * <pre>{@code
 * NSDictionary<String, String> env = new NSDictionary<String, String>(System.getenv(), true);
 *
 * for (String key : env)
 * 	logger.debug(env.valueForKey(key));
 * }</pre>
 *
 * @param <K> type of key contents
 * @param <V> type of value contents
 */
public class NSDictionary<K, V> implements Cloneable, Serializable, NSCoding, NSKeyValueCoding,
		NSKeyValueCodingAdditions, _NSFoundationCollection, Map<K, V> {

	static final long serialVersionUID = 2886170486405617806L;

	public class _JavaNSDictionaryMapEntry<P, Q> implements java.util.Map.Entry<P, Q> {

		@Override
		public P getKey() {
			return _entryKey;
		}

		@Override
		public Q getValue() {
			return _entryValue;
		}

		@Override
		public Q setValue(final Q value) {
			return (Q) NSDictionary.this.put((K) getKey(), (V) value);
		}

		@Override
		public boolean equals(final Object o) {
			return _entryKey == null && ((Map.Entry<P, Q>) o).getKey() == null
					&& getKey().equals(((Map.Entry<P, Q>) o).getKey())
					&& getValue().equals(((Map.Entry<P, Q>) o).getValue());
		}

		@Override
		public int hashCode() {
			return _entryKey == null ? System.identityHashCode(this) : _entryKey.hashCode();
		}

		Q _entryValue;
		P _entryKey;

		public _JavaNSDictionaryMapEntry(final P key, final Q value) {
			_entryKey = key;
			_entryValue = value;
		}
	}

	private void _copyImmutableDictionary(final NSDictionary<? extends K, ? extends V> otherDictionary) {
		_capacity = otherDictionary._capacity;
		_count = otherDictionary._count;
		_hashtableBuckets = otherDictionary._hashtableBuckets;
		_hashCache = otherDictionary._hashCache;
		_objects = otherDictionary._objects;
		_objectsCache = otherDictionary._objectsCache;
		_entrySetCache = null;
		_flags = otherDictionary._flags;
		_keys = otherDictionary._keys;
		_keysCache = otherDictionary._keysCache;
		_deletionLimit = otherDictionary._deletionLimit;
	}

	void _copyMutableDictionary(final NSDictionary<? extends K, ? extends V> otherDictionary) {
		if (otherDictionary.getClass() == NSMutableDictionary._CLASS
				|| otherDictionary.getClass() == NSDictionary._CLASS) {
			_capacity = otherDictionary._capacity;
			_count = otherDictionary._count;
			_hashtableBuckets = otherDictionary._hashtableBuckets;
			_hashCache = otherDictionary._hashCache;
			_objects = _NSCollectionPrimitives.copyArray(otherDictionary._objects);
			_objectsCache = null;
			_entrySetCache = null;
			_flags = _NSCollectionPrimitives.copyArray(otherDictionary._flags);
			_keys = _NSCollectionPrimitives.copyArray(otherDictionary._keys);
			_keysCache = null;
			_keySetCache = null;
			_deletionLimit = otherDictionary._deletionLimit;
		} else {
			_initializeDictionary();
			_ensureCapacity(otherDictionary.count());

			final Enumeration<? extends K> keyEnum = otherDictionary.keyEnumerator();
			while (keyEnum.hasMoreElements()) {
				final K key = keyEnum.nextElement();
				final V object = otherDictionary.objectForKey(key);
				if (_NSCollectionPrimitives.addValueInHashTable(key, object, _keys, _objects, _flags)) {
					_count++;
				}
			}
		}
	}

	protected void _initializeDictionary() {
		_capacity = _count = 0;
		_objects = _objectsCache = null;
		_entrySetCache = null;
		_flags = null;
		_keys = _keysCache = null;
		_hashtableBuckets = _NSCollectionPrimitives.hashTableBucketsForCapacity(_capacity);
		_deletionLimit = _NSCollectionPrimitives.deletionLimitForTableBuckets(_hashtableBuckets);
		_keySetCache = null;
		_entrySetCache = null;
	}

	protected void _ensureCapacity(final int capacity) {
		final int currentCapacity = _capacity;
		if (capacity > currentCapacity) {
			final int newCapacity = _NSCollectionPrimitives.hashTableCapacityForCapacity(capacity);
			if (newCapacity != currentCapacity) {
				final int oldSize = _hashtableBuckets;
				final int newSize = _NSCollectionPrimitives.hashTableBucketsForCapacity(newCapacity);
				_hashtableBuckets = newSize;
				if (newSize == 0) {
					_objects = null;
					_keys = null;
					_flags = null;
				} else {
					final Object[] oldObjects = _objects;
					final Object[] oldKeys = _keys;
					final byte[] oldFlags = _flags;
					final Object[] newObjects = new Object[newSize];
					final Object[] newKeys = new Object[newSize];
					final byte[] newFlags = new byte[newSize];
					for (int i = 0; i < oldSize; i++) {
						if ((oldFlags[i] & 0xffffffc0) == -128) {
							_NSCollectionPrimitives.addValueInHashTable(oldKeys[i], oldObjects[i], newKeys, newObjects,
									newFlags);
						}
					}

					_objects = newObjects;
					_keys = newKeys;
					_flags = newFlags;
				}
				_deletionLimit = _NSCollectionPrimitives.deletionLimitForTableBuckets(newSize);
				_capacity = newCapacity;
			}
		}
	}

	protected void _clearDeletionsAndCollisions() {
		final int size = _hashtableBuckets;
		if (_count == 0) {
			_flags = new byte[size];
		} else {
			final Object[] oldObjects = _objects;
			final Object[] oldKeys = _keys;
			final byte[] oldFlags = _flags;
			final Object[] newObjects = new Object[size];
			final Object[] newKeys = new Object[size];
			final byte[] newFlags = new byte[size];
			for (int i = 0; i < size; i++) {
				if ((oldFlags[i] & 0xffffffc0) == -128) {
					_NSCollectionPrimitives.addValueInHashTable(oldKeys[i], oldObjects[i], newKeys, newObjects,
							newFlags);
				}
			}

		}
		_deletionLimit = _NSCollectionPrimitives.deletionLimitForTableBuckets(size);
	}

	public NSDictionary() {
		_initializeDictionary();
	}

	public NSDictionary(final V object, final K key) {
		if (object == null) {
			throw new IllegalArgumentException("Attempt to insert null object into an  " + getClass().getName() + ".");
		}
		if (key == null) {
			throw new IllegalArgumentException("Attempt to insert null key into an  " + getClass().getName() + ".");
		}
		_initializeDictionary();
		_ensureCapacity(1);
		if (_NSCollectionPrimitives.addValueInHashTable(key, object, _keys, _objects, _flags)) {
			_count++;
		}
	}

	private void initFromKeyValues(final Object[] objects, final Object[] keys, final boolean checkForNull) {
		if (objects != null && keys != null) {
			if (objects.length != keys.length) {
				throw new IllegalArgumentException("Attempt to create an " + getClass().getName()
						+ " with a different number of objects and keys.");
			}
			if (checkForNull) {
				for (int i = 0; i < objects.length; i++) {
					if (objects[i] == null) {
						throw new IllegalArgumentException(
								"Attempt to insert null object into an  " + getClass().getName() + ".");
					}
					if (keys[i] == null) {
						throw new IllegalArgumentException(
								"Attempt to insert null key into an  " + getClass().getName() + ".");
					}
				}

			}
			_initializeDictionary();
			_ensureCapacity(objects.length);
			for (int i = 0; i < objects.length; i++) {
				if (_NSCollectionPrimitives.addValueInHashTable(keys[i], objects[i], _keys, _objects, _flags)) {
					_count++;
				}
			}

		} else if (objects == null && keys == null) {
			_initializeDictionary();
		} else {
			throw new IllegalArgumentException("Either objects and keys cannot be null");
		}
	}

	private NSDictionary(final V[] objects, final K[] keys, final boolean checkForNull) {
		initFromKeyValues(objects, keys, checkForNull);
	}

	public NSDictionary(final V[] objects, final K[] keys) {
		this(objects, keys, CheckForNull);
	}

	public NSDictionary(final NSArray<? extends V> objects, final NSArray<? extends K> keys) {
		this(objects == null ? null : (V[]) objects.objectsNoCopy(), keys == null ? null : (K[]) keys.objectsNoCopy(),
				false);
	}

	public NSDictionary(final NSDictionary<? extends K, ? extends V> otherDictionary) {
		if (otherDictionary.getClass() == _CLASS) {
			_copyImmutableDictionary(otherDictionary);
		} else {
			_copyMutableDictionary(otherDictionary);
		}
	}

	public NSDictionary(final Map<? extends K, ? extends V> map) {
		this(map, false);
	}

	public NSDictionary(final Map<? extends K, ? extends V> map, final boolean ignoreNull) {
		_initializeDictionary();
		if (map == null) {
			throw new NullPointerException("map cannot be null");
		}
		_ensureCapacity(map.size());
		final Set<? extends K> keySet = map.keySet();
		final Iterator<? extends K> it = keySet.iterator();
		do {
			if (!it.hasNext()) {
				break;
			}
			final Object key = it.next();
			final Object object = map.get(key);
			if (key == null) {
				if (!ignoreNull) {
					throw new IllegalArgumentException(
							"Attempt to insert null key into an  " + getClass().getName() + ".");
				}
			} else if (object == null) {
				if (!ignoreNull) {
					throw new IllegalArgumentException(
							"Attempt to insert null value into an  " + getClass().getName() + ".");
				}
			} else if (_NSCollectionPrimitives.addValueInHashTable(key, object, _keys, _objects, _flags)) {
				_count++;
			}
		} while (true);
	}

	public NSDictionary(final Dictionary<? extends K, ? extends V> dictionary, final boolean ignoreNull) {
		_initializeDictionary();
		if (dictionary != null) {
			_ensureCapacity(dictionary.size());
			final Enumeration<? extends K> enumeration = dictionary.keys();
			do {
				if (!enumeration.hasMoreElements()) {
					break;
				}
				final Object key = enumeration.nextElement();
				final Object object = dictionary.get(key);
				if (key == null) {
					if (!ignoreNull) {
						throw new IllegalArgumentException(
								"Attempt to insert null key into an  " + getClass().getName() + ".");
					}
				} else if (object == null) {
					if (!ignoreNull) {
						throw new IllegalArgumentException(
								"Attempt to insert null value into an  " + getClass().getName() + ".");
					}
				} else if (_NSCollectionPrimitives.addValueInHashTable(key, object, _keys, _objects, _flags)) {
					_count++;
				}
			} while (true);
		}
	}

	protected Object[] keysNoCopy() {
		if (_keysCache == null) {
			_keysCache = _count != 0
					? _NSCollectionPrimitives.keysInHashTable(_keys, _objects, _flags, _capacity, _hashtableBuckets)
					: _NSCollectionPrimitives.EmptyArray;
		}
		return _keysCache;
	}

	protected Object[] objectsNoCopy() {
		if (_objectsCache == null) {
			_objectsCache = _count != 0
					? _NSCollectionPrimitives.valuesInHashTable(_keys, _objects, _flags, _capacity, _hashtableBuckets)
					: _NSCollectionPrimitives.EmptyArray;
			_entrySetCache = null;
		}
		return _objectsCache;
	}

	public int count() {
		return _count;
	}

	public V objectForKey(final Object key) {
		return _count != 0 && key != null
				? (V) _NSCollectionPrimitives.findValueInHashTable(key, _keys, _objects, _flags)
				: null;
	}

	public Hashtable<K, V> hashtable() {
		final Object[] keys = keysNoCopy();
		final int c = keys.length;
		final Hashtable<K, V> hashtable = new Hashtable<>(c <= 0 ? 1 : c);
		for (int i = 0; i < c; i++) {
			hashtable.put((K) keys[i], objectForKey(keys[i]));
		}

		return hashtable;
	}

	public HashMap<K, V> hashMap() {
		final Object[] keys = keysNoCopy();
		final int c = keys.length;
		final HashMap<K, V> map = new HashMap<>(c <= 0 ? 1 : c);
		for (int i = 0; i < c; i++) {
			map.put((K) keys[i], objectForKey(keys[i]));
		}

		return map;
	}

	public NSArray<K> allKeysForObject(final Object object) {
		if (object != null) {
			final Object[] keys = keysNoCopy();
			final NSMutableArray<K> array = new NSMutableArray<>(keys.length);
			for (final Object key : keys) {
				final Object compareObject = objectForKey(key);
				if (object == compareObject || object.equals(compareObject)) {
					array.addObject((K) key);
				}
			}

			return array;
		}

		return NSArray.EmptyArray;
	}

	public NSArray<V> objectsForKeys(final NSArray<? extends K> keys, final V notFoundMarker) {
		if (keys != null) {
			final Object[] keysArray = keys.objectsNoCopy();
			final NSMutableArray<V> array = new NSMutableArray<>(keysArray.length);
			for (final Object element : keysArray) {
				final V object = objectForKey(element);
				if (object != null) {
					array.addObject(object);
					continue;
				}
				if (notFoundMarker != null) {
					array.addObject(notFoundMarker);
				}
			}

			return array;
		}

		return NSArray.EmptyArray;
	}

	private boolean _equalsDictionary(final NSDictionary<?, ?> otherDictionary) {
		final int count = count();
		if (count != otherDictionary.count()) {
			return false;
		}
		final Object[] keys = keysNoCopy();
		for (int i = 0; i < count; i++) {
			final Object value = objectForKey(keys[i]);
			final Object otherValue = otherDictionary.objectForKey(keys[i]);
			if (otherValue == null || !value.equals(otherValue)) {
				return false;
			}
		}

		return true;
	}

	public boolean isEqualToDictionary(final NSDictionary<?, ?> otherDictionary) {
		if (otherDictionary == null) {
			return false;
		}
		if (otherDictionary == this) {
			return true;
		}

		return _equalsDictionary(otherDictionary);
	}

	@Override
	public boolean equals(final Object object) {
		if (object == this) {
			return true;
		}
		if (object instanceof NSDictionary) {
			return _equalsDictionary((NSDictionary<K, V>) object);
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	public NSArray<K> allKeys() {
		return new NSArray(keysNoCopy());
	}

	@SuppressWarnings("unchecked")
	public NSArray<V> allValues() {
		return new NSArray(objectsNoCopy());
	}

	@SuppressWarnings("unchecked")
	public Enumeration<K> keyEnumerator() {
		return new _NSCollectionEnumerator(_keys, _flags, _count);
	}

	@SuppressWarnings("unchecked")
	public Enumeration<V> objectEnumerator() {
		return new _NSCollectionEnumerator(_objects, _flags, _count);
	}

	@Override
	public Object valueForKey(final String key) {
		final Object value = objectForKey(key);
		if (value == null && key != null) {
			if ("allValues".equals(key)) {
				return allValues();
			}
			if ("allKeys".equals(key)) {
				return allKeys();
			}
			if ("count".equals(key)) {
				return _NSUtilities.IntegerForInt(count());
			}
		}
		return value;
	}

	@Override
	public void takeValueForKey(final Object value, final String key) {
		throw new IllegalStateException(getClass().getName() + " is immutable.");
	}

	@Override
	public Object valueForKeyPath(final String keyPath) {
		final Object flattenedKeyPresent = objectForKey(keyPath);
		if (flattenedKeyPresent != null) {
			return flattenedKeyPresent;
		}

		return NSKeyValueCodingAdditions.DefaultImplementation.valueForKeyPath(this, keyPath);
	}

	@Override
	public void takeValueForKeyPath(final Object value, final String keyPath) {
		NSKeyValueCodingAdditions.DefaultImplementation.takeValueForKeyPath(this, value, keyPath);
	}

	@Override
	public Class classForCoder() {
		return _CLASS;
	}

	public static final <K, V> NSDictionary<K, V> emptyDictionary() {
		return NSDictionary.EmptyDictionary;
	}

	/**
	 * Returns an immutable {@code NSDictionary} containing zero entries.
	 *
	 * @param <K>the {@code NSDictionary}'s key type
	 * @param <V>the {@code NSDictionary}'s value type
	 * @return an empty {@code NSDictionary}
	 */
	public static <K, V> NSDictionary<K, V> of() {
		return EmptyDictionary;
	}

	/**
	 * Returns an immutable {@code NSDictionary} containing a single entry.
	 *
	 * @param <K>the {@code NSDictionary}'s key type
	 * @param <V>the {@code NSDictionary}'s value type
	 * @return a {@code NSDictionary} containing the specified mapping
	 */
	public static <K, V> NSDictionary<K, V> of(final K k1, final V v1) {
		return dictionaryOfImpl(k1, v1);
	}

	/**
	 * Returns an immutable {@code NSDictionary} containing two entries.
	 *
	 * @param <K>the {@code NSDictionary}'s key type
	 * @param <V>the {@code NSDictionary}'s value type
	 * @return a {@code NSDictionary} containing the specified mapping
	 */
	public static <K, V> NSDictionary<K, V> of(final K k1, final V v1, final K k2, final V v2) {
		return dictionaryOfImpl(k1, v1, k2, v2);
	}

	/**
	 * Returns an immutable {@code NSDictionary} containing three entries.
	 *
	 * @param <K>the {@code NSDictionary}'s key type
	 * @param <V>the {@code NSDictionary}'s value type
	 * @return a {@code NSDictionary} containing the specified mapping
	 */
	public static <K, V> NSDictionary<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
		return dictionaryOfImpl(k1, v1, k2, v2, k3, v3);
	}

	/**
	 * Returns an immutable {@code NSDictionary} containing four entries.
	 *
	 * @param <K>the {@code NSDictionary}'s key type
	 * @param <V>the {@code NSDictionary}'s value type
	 * @return a {@code NSDictionary} containing the specified mapping
	 */
	public static <K, V> NSDictionary<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
			final K k4, final V v4) {
		return dictionaryOfImpl(k1, v1, k2, v2, k3, v3, k4, v4);
	}

	/**
	 * Returns an immutable {@code NSDictionary} containing five entries.
	 *
	 * @param <K>the {@code NSDictionary}'s key type
	 * @param <V>the {@code NSDictionary}'s value type
	 * @return a {@code NSDictionary} containing the specified mapping
	 */
	public static <K, V> NSDictionary<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
			final K k4, final V v4, final K k5, final V v5) {
		return dictionaryOfImpl(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
	}

	/**
	 * Returns an immutable {@code NSDictionary} containing six entries.
	 *
	 * @param <K>the {@code NSDictionary}'s key type
	 * @param <V>the {@code NSDictionary}'s value type
	 * @return a {@code NSDictionary} containing the specified mapping
	 */
	public static <K, V> NSDictionary<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
			final K k4, final V v4, final K k5, final V v5, final K k6, final V v6) {
		return dictionaryOfImpl(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
	}

	/**
	 * Returns an immutable {@code NSDictionary} containing seven entries.
	 *
	 * @param <K>the {@code NSDictionary}'s key type
	 * @param <V>the {@code NSDictionary}'s value type
	 * @return a {@code NSDictionary} containing the specified mapping
	 */
	public static <K, V> NSDictionary<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
			final K k4, final V v4, final K k5, final V v5, final K k6, final V v6, final K k7, final V v7) {
		return dictionaryOfImpl(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
	}

	/**
	 * Returns an immutable {@code NSDictionary} containing eight entries.
	 *
	 * @param <K>the {@code NSDictionary}'s key type
	 * @param <V>the {@code NSDictionary}'s value type
	 * @return a {@code NSDictionary} containing the specified mapping
	 */
	public static <K, V> NSDictionary<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
			final K k4, final V v4, final K k5, final V v5, final K k6, final V v6, final K k7, final V v7, final K k8,
			final V v8) {
		return dictionaryOfImpl(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
	}

	/**
	 * Returns an immutable {@code NSDictionary} containing nine entries.
	 *
	 * @param <K>the {@code NSDictionary}'s key type
	 * @param <V>the {@code NSDictionary}'s value type
	 * @return a {@code NSDictionary} containing the specified mapping
	 */
	public static <K, V> NSDictionary<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
			final K k4, final V v4, final K k5, final V v5, final K k6, final V v6, final K k7, final V v7, final K k8,
			final V v8, final K k9, final V v9) {
		return dictionaryOfImpl(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
	}

	/**
	 * Returns an immutable {@code NSDictionary} containing ten entries.
	 *
	 * @param <K>the {@code NSDictionary}'s key type
	 * @param <V>the {@code NSDictionary}'s value type
	 * @return a {@code NSDictionary} containing the specified mapping
	 */
	public static <K, V> NSDictionary<K, V> of(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3,
			final K k4, final V v4, final K k5, final V v5, final K k6, final V v6, final K k7, final V v7, final K k8,
			final V v8, final K k9, final V v9, final K k10, final V v10) {
		return dictionaryOfImpl(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
	}

	@SuppressWarnings("unchecked")
	private static <K, V> NSDictionary<K, V> dictionaryOfImpl(final Object... input) {
		if (input.length == 2) {
			return new NSDictionary<>((V) input[1], (K) input[0]);
		}

		final int capacity = input.length / 2;
		final Object[] keys = new Object[capacity];
		final Object[] values = new Object[capacity];

		for (int i = 0, j = 0; i < input.length; i += 2, j++) {
			keys[j] = input[i];
			values[j] = input[i + 1];
		}

		return new NSDictionary<>((V[]) values, (K[]) keys);
	}

	public static Object decodeObject(final NSCoder coder) {
		final int count = coder.decodeInt();
		final Object[] keys = new Object[count];
		final Object[] objects = new Object[count];
		for (int i = 0; i < count; i++) {
			keys[i] = coder.decodeObject();
			objects[i] = coder.decodeObject();
		}

		return new NSDictionary<>(objects, keys);
	}

	@Override
	public void encodeWithCoder(final NSCoder coder) {
		final int count = count();
		coder.encodeInt(count);
		if (count > 0) {
			final Object[] keys = keysNoCopy();
			for (final Object key : keys) {
				coder.encodeObject(key);
				coder.encodeObject(objectForKey(key));
			}

		}
	}

	@Override
	public int _shallowHashCode() {
		return _NSDictionaryClassHashCode;
	}

	@Override
	public int hashCode() {
		return _NSDictionaryClassHashCode ^ count();
	}

	@Override
	public Object clone() {
		return this;
	}

	public NSDictionary<K, V> immutableClone() {
		return this;
	}

	public NSMutableDictionary<K, V> mutableClone() {
		return new NSMutableDictionary<>(this);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(128);
		sb.append('{');
		final Object[] keys = keysNoCopy();
		for (final Object key : keys) {
			final Object object = objectForKey(key);
			sb.append(key.toString());
			sb.append(" = ");
			if (object instanceof String) {
				sb.append('"');
				sb.append((String) object);
				sb.append('"');
			} else if (object instanceof Boolean) {
				sb.append(((Boolean) object).toString());
			} else {
				sb.append(object);
			}
			sb.append("; ");
		}

		sb.append('}');
		return sb.toString();
	}

	private void writeObject(final ObjectOutputStream s) throws IOException {
		final java.io.ObjectOutputStream.PutField fields = s.putFields();
		final Object[] keys = keysNoCopy();
		final int c = keys.length;
		final Object[] values = new Object[c];
		for (int i = 0; i < c; i++) {
			values[i] = objectForKey(keys[i]);
		}

		fields.put(SerializationKeysFieldKey, keys);
		fields.put(SerializationValuesFieldKey, values);
		s.writeFields();
	}

	private void readObject(final ObjectInputStream s) throws IOException, ClassNotFoundException {
		java.io.ObjectInputStream.GetField fields = s.readFields();
		Object[] keys = (Object[]) fields.get(SerializationKeysFieldKey, _NSUtilities._NoObjectArray);
		Object[] values = (Object[]) fields.get(SerializationValuesFieldKey, _NSUtilities._NoObjectArray);
		keys = keys != null ? keys : _NSUtilities._NoObjectArray;
		values = values != null ? values : _NSUtilities._NoObjectArray;
		initFromKeyValues(values, keys, CheckForNull);
	}

	private Object readResolve() throws ObjectStreamException {
		if (getClass() == _CLASS && count() == 0) {
			return EmptyDictionary;
		}

		return this;
	}

	@Override
	public int size() {
		return count();
	}

	@Override
	public boolean isEmpty() {
		return count() <= 0;
	}

	@Override
	public boolean containsKey(final Object key) {
		return objectForKey(key) != null;
	}

	@Override
	public boolean containsValue(final Object value) {
		return allValues().containsObject(value);
	}

	@Override
	public V get(final Object key) {
		return objectForKey(key);
	}

	@Override
	public V put(final K key, final V value) {
		throw new UnsupportedOperationException(
				"put is not a supported operation in com.webobjects.foundation.NSDictionary");
	}

	@Override
	public V remove(final Object key) {
		throw new UnsupportedOperationException(
				"remove is not a supported operation in com.webobjects.foundation.NSDictionary");
	}

	@Override
	public void putAll(final Map<? extends K, ? extends V> t) {
		throw new UnsupportedOperationException(
				"putAll is not a supported operation in com.webobjects.foundation.NSDictionary");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException(
				"putAll is not a supported operation in com.webobjects.foundation.NSDictionary");
	}

	@Override
	public Set<K> keySet() {
		if (_keySetCache == null) {
			final Object[] currKeys = keysNoCopy();
			if (currKeys != null && currKeys.length > 0) {
				_keySetCache = new NSSet<>((K[]) currKeys);
			} else {
				_keySetCache = NSSet.EmptySet;
			}
		}
		return _keySetCache;
	}

	@Override
	public Collection<V> values() {
		return allValues();
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		if (_entrySetCache == null) {
			_entrySetCache = _initMapEntrySet();
		}

		return _entrySetCache;
	}

	private Set<Map.Entry<K, V>> _initMapEntrySet() {
		final Object[] keys = keysNoCopy();
		final int length = keys.length;
		final _JavaNSDictionaryMapEntry<K, V>[] set = new _JavaNSDictionaryMapEntry[length];
		for (int i = 0; i < length; i++) {
			final K key = (K) keys[i];
			final V object = objectForKey(key);
			final _JavaNSDictionaryMapEntry<K, V> current = new _JavaNSDictionaryMapEntry<>(key, object);
			set[i] = current;
		}

		return new NSSet<>(set);
	}

	public static final Class _CLASS = _NSUtilitiesExtra
			._classWithFullySpecifiedNamePrime("com.webobjects.foundation.NSDictionary");
	public static final Class _MAP_ENTRY_CLASS = _NSUtilitiesExtra
			._classWithFullySpecifiedNamePrime("com.webobjects.foundation.NSDictionary$_JavaNSDictionaryMapEntry");
	public static final NSDictionary EmptyDictionary = new NSDictionary();
	private static final String SerializationKeysFieldKey = "keys";
	private static final String SerializationValuesFieldKey = "objects";
	private static final Class<?> _objectArrayClass = new Object[0].getClass();
	protected transient int _capacity;
	protected transient int _hashtableBuckets;
	protected transient int _count;
	protected Object[] _objects;
	protected transient Object[] _objectsCache;
	protected transient byte[] _flags;
	protected Object[] _keys;
	protected transient Object[] _keysCache;
	protected transient int _hashCache;
	protected transient int _deletionLimit;
	protected static int _NSDictionaryClassHashCode = _CLASS.hashCode();
	protected static int _NSDictionaryMapEntryHashCode = _MAP_ENTRY_CLASS.hashCode();
	protected Set<K> _keySetCache;
	protected Set<Map.Entry<K, V>> _entrySetCache;
	public static final boolean CheckForNull = true;
	public static final boolean IgnoreNull = true;
	private static final ObjectStreamField[] serialPersistentFields = {
			new ObjectStreamField(SerializationKeysFieldKey, _objectArrayClass),
			new ObjectStreamField(SerializationValuesFieldKey, _objectArrayClass) };
}
