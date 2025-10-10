/*
 * Created on 26.08.2004
 */
package com.webobjects.directtoweb;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EOAttribute;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOModelGroup;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;

/**
 * Optimizes custom attribute handling and fixes a problem when a context can't
 * find its task or entity even though it is given in the rules.
 *
 * @author david caching
 * @author ak factory, thread safety, fix
 */
public class ERD2WContext extends D2WContext implements Serializable {
	/**
	 * Do I need to update serialVersionUID? See section 5.6 <cite>Type Changes
	 * Affecting Serialization</cite> on page 51 of the
	 * <a href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object
	 * Serialization Spec</a>
	 */
	private static final long serialVersionUID = 1L;

	private static Map customAttributes = new HashMap();
	private static final Object NOT_FOUND = new Object();

	static {
		if (WOApplication.application().isConcurrentRequestHandlingEnabled()) {
			customAttributes = Collections.synchronizedMap(customAttributes);
		}
	}

	/**
	 * Factory to create D2WContext's. You can provide your own subclass and set it
	 * via {@link #setFactory(Factory)}. The static methods newContext(...) should
	 * be used throughout ERD2W.
	 *
	 * @author ak
	 */
	public static class Factory {
		public D2WContext newContext() {
			return new ERD2WContext();
		}

		public D2WContext newContext(final WOSession session) {
			return new ERD2WContext(session);
		}

		public D2WContext newContext(final D2WContext context) {
			return new ERD2WContext(context);
		}
	}

	private static Factory _factory = new Factory();

	public static D2WContext newContext() {
		return _factory.newContext();
	}

	public static D2WContext newContext(final WOSession session) {
		return _factory.newContext(session);
	}

	public static D2WContext newContext(final D2WContext context) {
		return _factory.newContext(context);
	}

	public static void setFactory(final Factory factory) {
		_factory = factory;
	}

	public ERD2WContext() {
	}

	public ERD2WContext(final WOSession session) {
		super(session);
	}

	public ERD2WContext(final D2WContext session) {
		super(session);
	}

	/**
	 * Overrridden because when a page config is set, task and entity are cleared,
	 * but not re-set when you just call task() or entity(). This leads to NPEs,
	 * errors that a pageName can't be found and others. Setting it here fixes it.
	 */
	@Override
	public void setDynamicPage(final String page) {
		super.setDynamicPage(page);
		setTask(task());
		setEntity(entity());
	}

	/**
	 * Overridden so that custom attributes are cached as a performance
	 * optimization.
	 */
	@Override
	EOAttribute customAttribute(final String s, final EOEntity eoentity) {
		final String s1 = eoentity.name() + "." + s;
		final Object o = customAttributes.get(s1);
		if (o == NOT_FOUND) {
			return null;
		}
		EOAttribute eoattribute = (EOAttribute) o;
		if (eoattribute == null && s != null) {
			final Class class1 = D2WUtils.dataTypeForCustomKeyAndEntity(s, eoentity);
			if (class1 != null) {
				eoattribute = new EOAttribute();
				eoattribute.setName(s);
				eoattribute.setClassName(class1.getName());
				customAttributes.put(s1, eoattribute);
			} else {
				// this should be cached, too
				// can save up to 100 millis and more for complex pages
				customAttributes.put(s1, NOT_FOUND);
			}
		}
		return eoattribute;
	}

	private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
		takeValueForKey(in.readObject(), D2WModel.SessionKey);
		takeValueForKey(in.readBoolean() ? D2WModel.One : D2WModel.Zero, D2WModel.FrameKey);
		takeValueForKey(in.readObject(), D2WModel.TaskKey);
		final String entityName = (String) in.readObject();
		final EOEntity entity = entityName == null ? null : EOModelGroup.defaultGroup().entityNamed(entityName);
		takeValueForKey(entity, D2WModel.EntityKey);
		takeValueForKey(in.readObject(), D2WModel.PropertyKeyKey);
		takeValueForKey(in.readObject(), D2WModel.DynamicPageKey);
		/*
		 * The ec must be deserialized before the EO. Otherwise, when the EO is
		 * deserialized, it attempts to deserialize the EC, which turns around and tries
		 * to deserialize the EO again. The EO is returned in its partially deserialized
		 * state, which results in a NullPointerException when the EC starts to try to
		 * load values into the EO's dictionary... which is null.
		 */
		final EOEditingContext ec = (EOEditingContext) in.readObject();
		takeValueForKey(in.readObject(), "object");
	}

	private void writeObject(final ObjectOutputStream out) throws IOException {
		out.writeObject(valueForKey(D2WModel.SessionKey));
		out.writeBoolean(frame());
		out.writeObject(task());
		out.writeObject(entity() == null ? null : entity().name());
		out.writeObject(propertyKey());
		out.writeObject(dynamicPage());
		final EOEnterpriseObject obj = (EOEnterpriseObject) valueForKey("object");
		final EOEditingContext ec = obj == null || obj.editingContext() == null ? null : obj.editingContext();
		/*
		 * The ec must be deserialized before the EO. Otherwise, when the EO is
		 * deserialized, it attempts to deserialize the EC, which turns around and tries
		 * to deserialize the EO again. The EO is returned in its partially deserialized
		 * state, which results in a NullPointerException when the EC starts to try to
		 * load values into the EO's dictionary... which is null.
		 */
		out.writeObject(ec);
		/*
		 * If a create page is cancelled, the object is deleted. When that happens, the
		 * ec is null. Writing the EO without an EC results with the same error as not
		 * writing the ec at all.
		 */
		out.writeObject(ec == null ? null : obj);
	}
}
