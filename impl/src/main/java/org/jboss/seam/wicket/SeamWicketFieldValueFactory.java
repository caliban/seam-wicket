/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.seam.wicket;

import java.lang.reflect.Field;
import java.rmi.Remote;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Local;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.apache.wicket.injection.IFieldValueFactory;
import org.apache.wicket.proxy.LazyInitProxyFactory;

/**
 * this c
 * @author caliban
 */
public class SeamWicketFieldValueFactory implements IFieldValueFactory {

    private static final Logger LOGGER = Logger.getLogger(SeamWicketFieldValueFactory.class.getName());

    @Override
    public Object getFieldValue(Field field, Object fieldOwner) {
          LOGGER.log(Level.INFO, "creating proxy for "+field.getType());
        return new LazyInitProxyFactory().createProxy(field.getType(), new ProxyTargetLocator(fieldOwner, field));
    }

    @Override
    public boolean supportsField(Field field) {
        if (field.getAnnotation(Inject.class) != null || field.getAnnotation(javax.ejb.EJB.class) != null) {
            LOGGER.log(Level.INFO, "found " + field.toGenericString());
            Class clazz = field.getType();
            if (clazz.getAnnotation(Stateless.class) != null || clazz.getAnnotation(Stateful.class) != null || clazz.getAnnotation(Local.class) != null
                    || clazz.getAnnotation(Remote.class) != null) {
                LOGGER.log(Level.INFO, "found class with " + clazz.getCanonicalName());
                return true;
            }
        }
        return false;
    }
}
