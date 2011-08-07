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
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import org.apache.wicket.proxy.IProxyTargetLocator;
import org.apache.wicket.proxy.LazyInitProxyFactory;
import org.jboss.seam.solder.beanManager.BeanManagerLocator;
import org.jboss.seam.wicket.ProxyTargetLocator;
import org.jboss.seam.wicket.util.NonContextual;

/**
 * This injector delegates most of its work to the seam/weld beanmanger for the injection.
 * If a ejb is detected a lazyinitproxy from wicket-ioc will be inserted between the 
 * component containing the ejb and the ejb itself. For the record, this ist the third proxy
 * object, the first may be the one from the server (glassfish for example) the second is from
 * weld. the reason for this proxy is, wicket requires that all member of a component must be
 * serializable, and the glassfish proxy isn't. 
 * @author pengt
 */
public final class SeamWicketInjector {

    private static final Logger LOGGER = Logger.getLogger(SeamWicketInjector.class.getName());
    
    /**
     * injects any injectable members into an object
     * @param component the object
     */
    public void inject(Object component) {
        
         //The manager could be null in unit testing environments
        
        BeanManager manager = new BeanManagerLocator().getBeanManager();
        if (manager != null) {
            //first step, let the beanmanager inject the whole object
            NonContextual.of(component.getClass(), manager).existingInstance(component).inject();
            //second, filter for injectable fields
            Field[] fields = extractInjectableFields(component);
            //third
            fields = filterForEJB(fields);

            for (Field field : fields) {

                //if we are here, the current field is a) injectable, b
                try {
                    //the crucial step, we weave an lazyinitproxy in beween.
                    // the lazyinitproxy is misleading, in fact the injected value will be 
                    //stored in a transien field, and any subsequent (de)serialization will 
                    //result in a fresh injection of an target object
                    Object proxy = LazyInitProxyFactory.createProxy(field.getType(), constructLocator(field.getType(), field.get(component)));
                    field.setAccessible(true);
                    field.set(component, proxy);
                } catch (IllegalArgumentException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }

        }
    }

    /**
     * look for injectable fields such as @ejb or @inject. @resource is omitted,
     * because that can't shouldn't be an ejb
     * @param comp the object with fields
     * @return an array of fields matching this condition
     */
    private Field[] extractInjectableFields(Object comp) {
        Class clazz = comp.getClass();
        //get all declared fields
        Field[] fields = clazz.getDeclaredFields();
        //prepare return value
        List<Field> retval = new ArrayList<Field>();
        for (Field field : fields) {
            //filter the fields for @inject or @ejb
            if (field.getAnnotation(Inject.class) != null || field.getAnnotation(javax.ejb.EJB.class) != null) {
                LOGGER.log(Level.INFO, "found " + field.toGenericString());
                retval.add(field);
            }
        }
        return retval.toArray(new Field[retval.size()]);

    }

    /**
     * filter this field array for ejbs. the type class must have either @stateless, @remote, @statefull, @local
     * TODO any more annotations supported?
     * @param the fields found
     * @return an array of fields matching this condition
     */
    private Field[] filterForEJB(Field[] fields) {
        List<Field> retval = new ArrayList<Field>();
        for (Field field : fields) {
            field.setAccessible(true);
            Class clazz = field.getType();
            LOGGER.log(Level.INFO, "typeclass" + clazz.getCanonicalName());
            //look for @stateless, @stateful, @remote or @local
            if (clazz.getAnnotation(Stateless.class) != null || clazz.getAnnotation(Stateful.class) != null || clazz.getAnnotation(Local.class) != null
                    || clazz.getAnnotation(Remote.class) != null) {
                LOGGER.log(Level.INFO, "found class with " + clazz.getCanonicalName());
                retval.add(field);
            }
        }
        return retval.toArray(new Field[retval.size()]);
    }

    /**
     * construct a new Proxytargetlocator
     *
     */
    private IProxyTargetLocator constructLocator(final Class clazz, Object object) {
        // TODO omit this method, it is obsolete
        return new ProxyTargetLocator(clazz, object);
    }
}
