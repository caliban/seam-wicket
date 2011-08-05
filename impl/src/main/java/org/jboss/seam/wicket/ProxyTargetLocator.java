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
 *
 * @author caliban
 */
public class ProxyTargetLocator implements IProxyTargetLocator {

    private Class clazz;

    public ProxyTargetLocator(Class clazz) {

        this.clazz = clazz;
    }

    @Override
    public Object locateProxyTarget() {
         BeanManager manager = new BeanManagerLocator().getBeanManager();
        return NonContextual.of(clazz, manager).newInstance().produce().inject().get();
    }
}
