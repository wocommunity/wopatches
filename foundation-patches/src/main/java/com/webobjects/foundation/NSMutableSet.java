package com.webobjects.foundation;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * <div class="en"> NSSet reimplementation to support JDK 1.5 templates. Use
 * with </div>
 *
 * <div class="ja"> JDK 1.5 テンプレートをサポートする為の再実装。使用は </div>
 *
 * <pre>{@code
 * NSMutableSet<E> set = new NSMutableSet<E>();
 * set.put(new E())
 *
 * for (E t : set)
 *     logger.debug(t);
 * }</pre>
 *
 * @param <E> - type of set contents
 */
public class NSMutableSet<E> extends NSSet<E> {

	static final long serialVersionUID = -6054074706096120227L;

	public NSMutableSet() {
	}

	public NSMutableSet(final int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("Capacity cannot be less than 0");
		}

		_ensureCapacity(capacity);
	}

	public NSMutableSet(final Collection<? extends E> collection) {
		super(collection);
	}

	public NSMutableSet(final E object) {
		super(object);
	}

	public NSMutableSet(final E[] objects) {
		super(objects);
	}

	public NSMutableSet(final E object, final E... objects) {
		super(object, objects);
	}

	public NSMutableSet(final NSArray<? extends E> objects) {
		super(objects);
	}

	public NSMutableSet(final NSSet<? extends E> otherSet) {
		super(otherSet);
	}

	public NSMutableSet(final Set<? extends E> set, final boolean ignoreNull) {
		super(set, ignoreNull);
	}

	public void addObject(final E object) {
		if (object == null) {
			throw new IllegalArgumentException("Attempt to insert null into an  " + getClass().getName() + ".");
		}
		if (count() == capacity()) {
			_ensureCapacity(count() + 1);
		}
		if (_NSCollectionPrimitives.addValueToSet(object, _objects, _flags)) {
			_setCount(count() + 1);
			_objectsCache = null;
		}
	}

	public void addObjects(final E... objects) {
		if (objects != null && objects.length > 0) {
			if (count() + objects.length > capacity()) {
				_ensureCapacity(count() + objects.length);
			}
			for (final E object : objects) {
				if (object == null) {
					throw new IllegalArgumentException("Attempt to insert null into an  " + getClass().getName() + ".");
				}
				if (_NSCollectionPrimitives.addValueToSet(object, _objects, _flags)) {
					_setCount(count() + 1);
					_objectsCache = null;
				}
			}
		}
	}

	public E removeObject(final Object object) {
		Object result = null;
		if (object != null && count() != 0) {
			result = _NSCollectionPrimitives.removeValueInHashTable(object, _objects, _objects, _flags);
			if (result != null) {
				_setCount(count() - 1);
				_deletionLimit--;
				if (count() == 0 || _deletionLimit == 0) {
					_clearDeletionsAndCollisions();
				}
				_objectsCache = null;
			}
		}
		return (E) result;
	}

	public void removeAllObjects() {
		if (count() != 0) {
			_objects = new Object[_hashtableBuckets];
			_flags = new byte[_hashtableBuckets];
			_setCount(0);
			_objectsCache = null;
			_deletionLimit = _NSCollectionPrimitives.deletionLimitForTableBuckets(_hashtableBuckets);
		}
	}

	public void setSet(final NSSet<? extends E> otherSet) {
		if (otherSet != this) {
			removeAllObjects();
			if (otherSet != null) {
				final E[] objects = (E[]) otherSet.objectsNoCopy();
				for (final E object : objects) {
					addObject(object);
				}

			}
		}
	}

	public void addObjectsFromArray(final NSArray<? extends E> array) {
		if (array != null) {
			final E[] objects = (E[]) array.objectsNoCopy();
			for (final E object : objects) {
				addObject(object);
			}

		}
	}

	public void intersectSet(final NSSet<?> otherSet) {
		if (otherSet != this) {
			if (otherSet == null || otherSet.count() == 0) {
				removeAllObjects();
				return;
			}
			final E[] objects = (E[]) objectsNoCopy();
			for (final E object : objects) {
				if (otherSet.member(object) == null) {
					removeObject(object);
				}
			}

		}
	}

	public void subtractSet(final NSSet<?> otherSet) {
		if (otherSet == null || otherSet.count() == 0) {
			return;
		}
		if (otherSet == this) {
			removeAllObjects();
			return;
		}
		final Object[] objects = otherSet.objectsNoCopy();
		for (final Object object : objects) {
			if (member(object) != null) {
				removeObject(object);
			}
		}

	}

	public void unionSet(final NSSet<? extends E> otherSet) {
		if (otherSet == null || otherSet.count() == 0 || otherSet == this) {
			return;
		}
		final E[] objects = (E[]) otherSet.objectsNoCopy();
		for (final E object : objects) {
			addObject(object);
		}

	}

	@Override
	public Object clone() {
		return new NSMutableSet<>(this);
	}

	@Override
	public NSSet<E> immutableClone() {
		return new NSSet<>(this);
	}

	@Override
	public NSMutableSet<E> mutableClone() {
		return (NSMutableSet<E>) clone();
	}

	public static final Class _CLASS = _NSUtilities
			._classWithFullySpecifiedName("com.webobjects.foundation.NSMutableSet");

	@Override
	public boolean add(final E o) {
		if (contains(o)) {
			return false;
		}

		addObject(o);

		return true;
	}

	@Override
	public boolean remove(final Object o) {
		return (removeObject(o) != null) == true;
	}

	@Override
	public boolean addAll(final Collection<? extends E> c) {
		boolean updated = false;
		for (final E t : c) {
			if (!contains(t)) {
				add(t);
				updated = true;
			}
		}

		return updated;
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		final NSMutableSet<Object> s = new NSMutableSet<>();
		boolean updated = false;
		for (final Object o : c) {
			s.add(o);
			if (!contains(o)) {
				updated = true;
			}

		}
		intersectSet(s);

		return updated;
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		final NSMutableSet<Object> s = new NSMutableSet<>();
		boolean updated = false;
		for (final Object o : c) {
			s.add(o);
			if (!contains(o)) {
				updated = true;
			}
		}
		subtractSet(s);

		return updated;
	}

	@Override
	public void clear() {
		removeAllObjects();
	}

	@Override
	public Iterator<E> iterator() {
		return new Itr();
	}

	private class Itr implements Iterator<E> {
		int cursor = 0;
		static final int NotFound = -1;
		int lastRet = NotFound;

		protected Itr() {
		}

		@Override
		public boolean hasNext() {
			return cursor != size();
		}

		@Override
		public E next() {
			try {
				final Object next = objectsNoCopy()[cursor];
				lastRet = cursor++;
				return (E) next;
			} catch (final IndexOutOfBoundsException e) {
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			if (lastRet == NotFound) {
				throw new IllegalStateException();
			}

			try {
				removeObject(objectsNoCopy()[lastRet]);
				if (lastRet < cursor) {
					cursor--;
				}
				lastRet = NotFound;
			} catch (final IndexOutOfBoundsException e) {
				throw new ConcurrentModificationException();
			}
		}
	}
}
