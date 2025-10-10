package com.webobjects.eocontrol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;

import com.webobjects.foundation._NSUtilities;

/**
 * Better hashing for long values
 *
 * @author ak
 *
 */
public class _EOIntegralKeyGlobalID extends EOKeyGlobalID {

	public static final Class _CLASS = _NSUtilities
			._classWithFullySpecifiedName("com.webobjects.eocontrol._EOIntegralKeyGlobalID");
	static final long serialVersionUID = 8168566851552998142L;
	private Number keyValue;
	private static final String SerializationKeyValueFieldKey = "keyValue";
	private static final ObjectStreamField serialPersistentFields[];

	static {
		serialPersistentFields = new ObjectStreamField[] {
				new ObjectStreamField("keyValue", _NSUtilities._NumberClass) };
	}

	public _EOIntegralKeyGlobalID(final String entityName, final Number value) {
		super(entityName, _hashCode(entityName, value));
		keyValue = value;
	}

	public _EOIntegralKeyGlobalID(final String entityName, final Object values[]) {
		this(entityName, (Number) values[0]);
	}

	@Override
	public Object clone() {
		final _EOIntegralKeyGlobalID result = new _EOIntegralKeyGlobalID(_literalEntityName(), keyValue);
		_prepClone(result);
		return result;
	}

	@Override
	public Object[] keyValues() {
		return new Object[] { keyValue };
	}

	@Override
	public Object[] _keyValuesNoCopy() {
		return keyValues();
	}

	@Override
	public int keyCount() {
		return 1;
	}

	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder(_NSUtilities.shortClassName(this));
		result.append('[');
		result.append(entityName());
		result.append(" (");
		result.append(keyValue.getClass().getName());
		result.append(')');
		result.append(keyValue);
		result.append(']');
		return result.toString();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof _EOIntegralKeyGlobalID)) {
			return false;
		}
		final _EOIntegralKeyGlobalID other = (_EOIntegralKeyGlobalID) obj;
		final String entityName = _literalEntityName();
		final String otherEntityName = other._literalEntityName();
		if (entityName == otherEntityName && keyValue == other.keyValue) {
			return true;
		}
		if (!keyValue.equals(other.keyValue) || (hashCode() != other.hashCode())) {
			return false;
		}
		return entityName.equals(otherEntityName);
	}

	private static int _hashCode(final String entityName, final Number value) {
		int hashCode = entityName.hashCode();
		if (value != null) {
			final long longValue = value.longValue();
			hashCode ^= longValue;
			hashCode ^= longValue >> 16L;
			hashCode ^= longValue >> 32L;
			hashCode ^= longValue >> 48L;
		}
		return hashCode == 0 ? 42 : hashCode;
	}

	private void writeObject(final ObjectOutputStream s) throws IOException {
		final java.io.ObjectOutputStream.PutField fields = s.putFields();
		fields.put("keyValue", keyValue);
		s.writeFields();
	}

	private void readObject(final ObjectInputStream s) throws IOException, ClassNotFoundException {
		java.io.ObjectInputStream.GetField fields = s.readFields();
		keyValue = (Number) fields.get("keyValue", null);
		if (keyValue == null) {
			throw new IOException("key value must not be a null reference.");
		}
	}
}
