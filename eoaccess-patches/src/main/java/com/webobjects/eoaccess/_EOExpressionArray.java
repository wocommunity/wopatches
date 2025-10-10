package com.webobjects.eoaccess;

import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;

class _EOExpressionArray
		extends NSMutableArray<Object>
		implements EOSQLExpression.SQLValue {
	static final long serialVersionUID = 2726361908862120105L;
	protected String _prefix;
	protected String _infix;
	protected String _suffix;

	public _EOExpressionArray(final Object object) {
		super(object);
		_initWithPrefixInfixSuffix("", "", "");
	}

	public _EOExpressionArray() {
		_initWithPrefixInfixSuffix("", "", "");
	}

	public _EOExpressionArray(final String prefix, final String infix, final String suffix) {
		_initWithPrefixInfixSuffix(prefix, infix, suffix);
	}

	private void _initWithPrefixInfixSuffix(final String prefix, final String infix, final String suffix) {
		_prefix = prefix;
		_infix = infix;
		_suffix = suffix;
	}

	public String prefix() {
		return _prefix;
	}

	public String infix() {
		return _infix;
	}

	public String suffix() {
		return _suffix;
	}

	public void setPrefix(final String prefix) {
		_prefix = prefix;
	}

	public void setInfix(final String infix) {
		_infix = infix;
	}

	public void setSuffix(final String suffix) {
		_suffix = suffix;
	}

	public boolean referencesObject(final Object object) {
		for (int i = 0; i < count(); i++) {
			final Object member = objectAtIndex(i);
			if ((object == member) || (member instanceof _EOExpressionArray && ((_EOExpressionArray) member).referencesObject(object))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Object clone() {
		final _EOExpressionArray aCopy = new _EOExpressionArray(_prefix, _infix, _suffix);
		aCopy.addObjectsFromArray(this);
		return aCopy;
	}

	@Override
	public String valueForSQLExpression(final EOSQLExpression context) {
		final int count = count();

		if (count == 0) {
			return null;
		}

		if (context != null && objectAtIndex(0) instanceof EORelationship) {
			return context.sqlStringForAttributePath(this);
		}

		final StringBuilder aString = _prefix != null ? new StringBuilder(_prefix) : new StringBuilder(64);
		int pieces;
		for (int i = pieces = 0; i < count; i++) {
			final Object expression = objectAtIndex(i);
			final String result = valueForSQLExpression(expression, context);
			if (result != null && result.length() > 0) {
				pieces++;
				if (pieces > 1 &&
						_infix != null) {
					aString.append(_infix);
				}
				aString.append(result);
			}
		}

		if (pieces == 0) {
			return null;
		}
		if (_suffix != null) {
			aString.append(_suffix);
		}
		return new String(aString);
	}

	public String valueForSQLExpression(final Object expression, final EOSQLExpression context) {
		if (expression instanceof EOSQLExpression.SQLValue) {
			return ((EOSQLExpression.SQLValue) expression).valueForSQLExpression(context);
		}
		if (expression instanceof Number) {
			return String.valueOf(expression);
		}
		if (expression instanceof String) {
			return (String) expression;
		}
		if (expression == NSKeyValueCoding.NullValue) {
			return "NULL";
		}
		return expression.toString();
	}

	public boolean _isPropertyPath() {
		if (count() <= 0) {
			return false;
		}
		final Object object = objectAtIndex(0);
		return object instanceof EORelationship;
	}
}
