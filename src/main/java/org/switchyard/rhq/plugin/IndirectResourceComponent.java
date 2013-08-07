package org.switchyard.rhq.plugin;

import org.mc4j.ems.connection.bean.EmsBean;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * Resource component handling references to other beans.
 */
public class IndirectResourceComponent extends MBeanResourceComponent<JMXComponent<?>> {
    /**
     * Load the MBean information using the resource key.
     * @return the EmsBean associated with this key or null if it does not exist.
     */
    @Override
    protected EmsBean loadBean() {
        final String beanName = transformBeanName(getResourceContext().getResourceKey());
        final EmsBean loadedBean = loadBean(beanName);
        return loadedBean;
    }
}
