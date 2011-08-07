/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.seam.wicket.web;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.Application;
import org.apache.wicket.IClusterable;
import org.apache.wicket.MetaDataKey;

/**
 * based on InjectorHolder in wicket-ioc and the guiceinjectorholder
 * 
 * This is a holder for the Injector. The reason we need a holder is that metadata only supports
 * storing serializable objects but Injector is not. The holder acts as a serializable wrapper for
 * the context. Notice that although holder implements IClusterable it really is not because it has
 * a reference to non-serializable context - but this is ok because metadata objects in application
 * are never serialized.
 * @author pengt
 */
public final class SeamWicketInjectorHolder implements IClusterable {

    private static final Logger LOGGER = Logger.getLogger(SeamWicketInjector.class.getName());
    public static final MetaDataKey<SeamWicketInjector> INJECTOR_KEY = new MetaDataKey<SeamWicketInjector>() {

        private static final long serialVersionUID = 1L;
    };
    
    private SeamWicketInjectorHolder()
    {
        
    }

    /**
     * Gets an injector
     * @return injector
     */
    public static  SeamWicketInjector getInjector() {
        SeamWicketInjector injector = Application.get().getMetaData(INJECTOR_KEY);
        if (injector == null) {
            throw new IllegalStateException("InjectorHolder has not been assigned an injector. "
                    + "Use InjectorHolder.setInjector() to assign an injector. "
                    + "In most cases this should be done once inside "
                    + "your WebApplication subclass's init() method.");
        }
        return injector;
    }

    /**
     * Sets an injector
     * 
     * @param newInjector
     *            new injector
     */
    public static void setInjector(SeamWicketInjector newInjector) {
        LOGGER.log(Level.INFO, "setting the injector");
        Application.get().setMetaData(SeamWicketInjectorHolder.INJECTOR_KEY, newInjector);
    }
}
