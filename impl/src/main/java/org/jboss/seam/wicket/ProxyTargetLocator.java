/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.seam.wicket;

import javax.enterprise.inject.spi.BeanManager;
import org.apache.wicket.proxy.IProxyTargetLocator;
import org.jboss.seam.solder.beanManager.BeanManagerLocator;
import org.jboss.seam.wicket.util.NonContextual;

/**
 * this proxytargetlocator receives a class and an inital object and produces 
 * each time when it is deserialized a new object from this class, injected
 * by seam/weld
 * @author caliban
 */
public class ProxyTargetLocator implements IProxyTargetLocator {

    private Class clazz;
    private transient Object transientObject;

    /**
     * @param clazz the target class
     * @param transientObject an inital object, may be null
     */
    public ProxyTargetLocator(Class clazz, Object transientObject) {

        this.clazz = clazz;
        this.transientObject = transientObject;
    }

    @Override
    public Object locateProxyTarget() {
        if (transientObject == null) {
            BeanManager manager = new BeanManagerLocator().getBeanManager();
            transientObject = NonContextual.of(clazz, manager).newInstance().produce().inject().get();
        }
        return transientObject;
    }
}
