/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.seam.wicket;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.proxy.IProxyTargetLocator;
import org.jboss.seam.solder.beanManager.BeanManagerLocator;

/**
 * this proxytargetlocator receives a class and an inital object and produces 
 * each time when it is deserialized a new object from this class, injected
 * by seam/weld
 * NEW VERSION: Input from hwellmann included, based on his work on the CdiProxyTargetLocator
 * @author caliban
 */
public class ProxyTargetLocator implements IProxyTargetLocator {

    private Class fieldHolder;
    private transient Object transientObject;
    private String nameOfField;
    
    private static final Logger LOGGER = Logger.getLogger(ProxyTargetLocator.class.getName());

    /**
     * @param clazz the target class
     * @param transientObject an inital object, may be null
     */
    public ProxyTargetLocator(Object fieldHolder, Field field) {

        LOGGER.log(Level.INFO, "proxy set on "+fieldHolder.getClass().getSimpleName() + " and field" +field.getName());
        this.fieldHolder = fieldHolder.getClass();
        this.nameOfField = field.getName();
        try {
            field.setAccessible(true);
            this.transientObject = field.get(fieldHolder);
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Object locateProxyTarget() {
        if (transientObject == null) {
            transientObject = getObject();
        }
        return transientObject;
    }

    /**
     * get the real reference
     */
    private Object getObject() {
        BeanManager mgr = new BeanManagerLocator().getBeanManager();
        LOGGER.log(Level.INFO, "obtaining new object for class "+fieldHolder + " and field" +nameOfField);
        Class clazz = fieldHolder;
        Field field; 
        try {
            field = clazz.getDeclaredField(nameOfField);
        } catch (NoSuchFieldException exc) {
            throw new WicketRuntimeException(exc);
        }
        Class<?> memberType = field.getType();
        Annotation[] annotations = field.getAnnotations();
        Set<Annotation> qualifiers = new HashSet<Annotation>();
        for (Annotation annotation : annotations) {
            if (mgr.isQualifier(annotation.getClass())) {
                qualifiers.add(annotation);
            }
        }
        Set<Bean<?>> beans = mgr.getBeans(memberType, qualifiers.toArray(new Annotation[0]));

        if (beans.isEmpty()) {
            throw new WicketRuntimeException("No bean for type " + memberType);
        }

        if (beans.size() > 1) {
            throw new WicketRuntimeException("Ambiguous injection point of type " + memberType);
        }

        Bean<?> bean = beans.iterator().next();

        //non contextual
        CreationalContext<Object> context = mgr.createCreationalContext(null);
        Object reference = mgr.getReference(bean, memberType, context);
        return reference;

    }
}
