/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.seam.wicket.web;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.inject.spi.BeanManager;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.injection.ConfigurableInjector;
import org.apache.wicket.injection.IFieldValueFactory;
import org.apache.wicket.proxy.IProxyTargetLocator;
import org.apache.wicket.util.collections.ClassMetaCache;
import org.jboss.seam.solder.beanManager.BeanManagerLocator;
import org.jboss.seam.wicket.SeamWicketFieldValueFactory;
import org.jboss.seam.wicket.util.NonContextual;

/**
 * This injector delegates most of its work to the seam/weld beanmanger for the injection.
 * If a ejb is detected a lazyinitproxy from wicket-ioc will be inserted between the 
 * component containing the ejb and the ejb itself. For the record, this ist the third proxy
 * object, the first may be the one from the server (glassfish for example) the second is from
 * weld. the reason for this proxy is, wicket requires that all member of a component must be
 * serializable, and the glassfish proxy isn't. 
 * TODO wicket ioc enhancement 
 * @author pengt
 */
public final class SeamWicketInjector extends ConfigurableInjector {

    private static final Logger LOGGER = Logger.getLogger(SeamWicketInjector.class.getName());
    private static final Field[] EMPTY_FIELDS = new Field[0];
    private final ClassMetaCache<Field[]> cache = new ClassMetaCache<Field[]>();

    /**
     * injects any injectable members into an object
     * @param component the object
     */
    @Override
    public Object inject(Object component) {

        //The manager could be null in unit testing environments

        BeanManager manager = new BeanManagerLocator().getBeanManager();

        if (manager != null) {
            LOGGER.log(Level.INFO, "inject for comp " + component.getClass());
            //first step, let the beanmanager inject the whole object
            NonContextual.of(component.getClass(), manager).existingInstance(component).inject();
            //second; trigger the proxy replacement strategy
            super.inject(component);
            return component;

        } else {
            throw new WicketRuntimeException("could not obtain the beanmanager");
        }
    }

    /**
     * c/p from wicket-ioc
     * we cannot use the standard inject mechano from wicket ioc because it checks if 
     * the field is null, if yes, it will inject a proxy, else the algorithm does nothing.
     * but the core idea of this extension is to weave a proxylocator in a already injected 
     * between the ejb and the component. so the object will hardly be null
     * 
     */
    @Override
    public Object inject(Object object, IFieldValueFactory factory) {
        final Class<?> clazz = object.getClass();

        Field[] fields = getFields(clazz, factory);

        for (int i = 0; i < fields.length; i++) {
            final Field field = fields[i];

            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            try {
                //the field may be null (almost impossible)
                //or it may have already an object. thats fine too

                Object value = factory.getFieldValue(field, object);

                if (value != null) {
                    field.set(object, value);

                }


            } catch (IllegalArgumentException e) {
                throw new RuntimeException("error while injecting object [" + object.toString()
                        + "] of type [" + object.getClass().getName() + "]", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("error while injecting object [" + object.toString()
                        + "] of type [" + object.getClass().getName() + "]", e);
            }
        }

        return object;
    }

    @Override
    protected IFieldValueFactory getFieldValueFactory() {
        return new SeamWicketFieldValueFactory();
    }

    /**
     * caches results of {@link #getFields(Class, IFieldValueFactory)}
     * c/p from wicket-ioc, no changes
     * @param clazz
     * @param factory
     * @return cached results as returned by {@link #getFields(Class, IFieldValueFactory)}
     */
    private Field[] getFields(Class<?> clazz, IFieldValueFactory factory) {
        Field[] fields = cache.get(clazz);

        if (fields == null) {
            fields = findFields(clazz, factory);

            // write to cache
            cache.put(clazz, fields);
        }

        return fields;
    }

    /**
     * Returns an array of fields that can be injected using the given field value factory
     * c/p from wicket-ioc, no changes
     * @param clazz
     * @param factory
     * @return an array of fields that can be injected using the given field value factory
     */
    private Field[] findFields(Class<?> clazz, IFieldValueFactory factory) {
        List<Field> matched = new ArrayList<Field>();

        while (clazz != null && !isBoundaryClass(clazz)) {
            Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                final Field field = fields[i];

                if (factory.supportsField(field)) {
                    matched.add(field);
                }
            }
            clazz = clazz.getSuperclass();
        }

        return matched.size() == 0 ? EMPTY_FIELDS : matched.toArray(new Field[matched.size()]);
    }
}
