package org.jboss.seam.wicket;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.application.IComponentInstantiationListener;
import org.jboss.seam.wicket.web.SeamWicketInjector;

import org.apache.wicket.injection.ComponentInjector;
import org.apache.wicket.injection.web.InjectorHolder;
import org.jboss.seam.wicket.web.SeamWicketInjectorHolder;

/**
 * handles the injection with an indirection to the injector(holder). based on the
 * original seamcomponentInstantiationlistener and the wicket-ioc CIlistener.
 * 
 *
 * @author cpopetz
 * @author pengt
 */
public class SeamComponentInstantiationListener implements IComponentInstantiationListener{
    
    /**
     * creates the listener and the injector
     */
    public SeamComponentInstantiationListener()
    {
        createInjector();
    }

    /**
     * creates an injector and stores it 
     */
    private void createInjector() {
       SeamWicketInjectorHolder.setInjector(new SeamWicketInjector());
    }

    @Override
    public void onInstantiation(Component component) {
        SeamWicketInjectorHolder.getInjector().inject(component);
    }
}
