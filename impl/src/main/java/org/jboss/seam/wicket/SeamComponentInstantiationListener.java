package org.jboss.seam.wicket;

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

import org.apache.wicket.Component;
import org.apache.wicket.application.IComponentInstantiationListener;
import org.apache.wicket.proxy.IProxyTargetLocator;
import org.apache.wicket.proxy.LazyInitProxyFactory;
import org.jboss.seam.wicket.util.NonContextual;

/**
 * This listener uses the BeanManager to handle injections for all wicket components.
 *
 * @author cpopetz
 */
public class SeamComponentInstantiationListener implements IComponentInstantiationListener {

    
    private static final Logger logger =Logger.getLogger(SeamComponentInstantiationListener.class.getName());
    @Inject
    private BeanManager manager;

    @Override
    public void onInstantiation(Component component) {
        /*
         * The manager could be null in unit testing environments
         */
        if (manager != null) {
            NonContextual.of(component.getClass(), manager).existingInstance(component).inject();
            Field[] fields = extractFields(component);
            fields = filterForEJB(fields);

            for (Field field : fields) {

                Object proxy = LazyInitProxyFactory.createProxy(field.getType(), constructLocator(field.getType()));
                field.setAccessible(true);
                try {
                    field.set(component, proxy);
                } catch (IllegalArgumentException ex) {
                    logger.log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }

        }
    }

    private Field[] extractFields(Component comp) {
        Class clazz = comp.getClass();
        //get all declared fields
        Field[] fields = clazz.getDeclaredFields();
        //prepare return value
        List<Field> retval = new ArrayList<Field>();
        for (Field field : fields) {
            //filter the fields for @inject or @ejb
            if (field.getAnnotation(Inject.class) != null || field.getAnnotation(javax.ejb.EJB.class) != null) {
                logger.log(Level.INFO, "found "+field.toGenericString());
                retval.add(field);
            }
        }
        return retval.toArray(new Field[retval.size()]);

    }

    private Field[] filterForEJB(Field[] fields) {
        List<Field> retval = new ArrayList<Field>();
        for (Field field : fields) {
            field.setAccessible(true);
            Class clazz = field.getType();
             logger.log(Level.INFO, "declaringclass"+clazz.getCanonicalName());
            //look for @stateless, @stateful, @remote or @local
            if (clazz.getAnnotation(Stateless.class) != null || clazz.getAnnotation(Stateful.class) != null || clazz.getAnnotation(Local.class) != null
                    || clazz.getAnnotation(Remote.class) != null) {
                logger.log(Level.INFO, "found class with "+clazz.getCanonicalName());
                retval.add(field);
            }
        }
        return retval.toArray(new Field[retval.size()]);
    }

    private IProxyTargetLocator constructLocator(final Class clazz) {
       return new ProxyTargetLocator(clazz);
    }
}
