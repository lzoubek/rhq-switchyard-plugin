package org.switchyard.rhq.plugin;

import java.util.HashSet;
import java.util.Set;

import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * A discovery component for redirecting from one service to a second through properties that define MBean names.
 */
public class IndirectResourceDiscoveryComponent implements ResourceDiscoveryComponent<MBeanResourceComponent<JMXComponent<?>>> {

    /**
     * The logger instance.
     */
    private static Log LOG = LogFactory.getLog(IndirectResourceDiscoveryComponent.class);
    
    /**
     * The property that defines the name of the property containing the referenced children.
     */
    public static final String ATTRIBUTE_NAME = "attributeName";
    /**
     * The property that defines the prefix used when constructing the referenced children.
     */
    public static final String BEAN_PREFIX = "beanPrefix";
    /**
     * The property that defines the property name that should contain the name of the parent.
     */
    public static final String PARENT_NAME = "parentName";
    /**
     * The property that should contain the name of this resource.
     */
    public static final String NAME = "name";
    /**
     * The property defining the key identifying the resource name within the ObjectName.
     */
    public static final String OBJECT_NAME_ATTRIBUTE = "objectNameAttribute";

    /**
     * Discover existing resources.
     * @param context The context for the current discovery component.
     * @return The discovered resources. 
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(final ResourceDiscoveryContext<MBeanResourceComponent<JMXComponent<?>>> context) {

        final HashSet<DiscoveredResourceDetails> result = new HashSet<DiscoveredResourceDetails>();
        final MBeanResourceComponent<JMXComponent<?>> parentComponent = context.getParentResourceComponent();

        final Configuration defaultPluginConfiguration = context.getDefaultPluginConfiguration();
        
        final String attributeValue = getPropertyValue(defaultPluginConfiguration, ATTRIBUTE_NAME);
        
         if (attributeValue != null) {
               final EmsAttribute attribute = parentComponent.getEmsBean().getAttribute(attributeValue);
               if (attribute != null) {
                   final Object value = attribute.getValue();
                   if (value != null) {
                       if (value instanceof String[]) {
                           final String[] beanNames = (String[])value;
                           final String beanPrefix = getPropertyValue(defaultPluginConfiguration, BEAN_PREFIX);
                           final String parentNameValue = context.getParentResourceContext().getPluginConfiguration().getSimpleValue(NAME);
                           for (String beanName : beanNames) {
                               final String key = (beanPrefix != null ? beanPrefix + ObjectName.quote(beanName) : beanName);
                               
                               final DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(),
                                   key, beanName, "", null, null, null);
                               resource.getPluginConfiguration().setSimpleValue(NAME, beanName);
                               
                               final String parentName = getPropertyValue(defaultPluginConfiguration, PARENT_NAME);
                               if ((parentName != null) && (parentNameValue != null)) {
                                   resource.getPluginConfiguration().setSimpleValue(parentName, parentNameValue);
                               }
                               
                               result.add(resource);
                               if (LOG.isDebugEnabled()) {
                                   LOG.debug("Discovered bean " + beanName);
                               }
                           }
                       } else if (value instanceof ObjectName) {
                           final ObjectName objectName = (ObjectName)value;
                           final String objectNameAttribute = getPropertyValue(defaultPluginConfiguration, OBJECT_NAME_ATTRIBUTE);
                           final DiscoveredResourceDetails resource = createResource(context, objectName, objectNameAttribute);
                           result.add(resource);
                       } else if (value instanceof ObjectName[]) {
                           final ObjectName[] objectNames = (ObjectName[])value;
                           final String objectNameAttribute = getPropertyValue(defaultPluginConfiguration, OBJECT_NAME_ATTRIBUTE);
                           for (ObjectName objectName : objectNames) {
                               final DiscoveredResourceDetails resource = createResource(context, objectName, objectNameAttribute);
                               result.add(resource);
                           }
                       } else {
                           LOG.debug("Query returned attribute type of " + value.getClass().getName());
                       }
                   }
               }
        }

        return result;
    }

    /**
     * Create the resource details for a bean reference by an ObjectName.
     * @param context The context for the current discovery component.
     * @param objectName The object name associated with the target MBean
     * @param objectNameAttribute  The key identifying the name of the bean within the objectName
     * @return The discovered resources. 
     */
    private DiscoveredResourceDetails createResource(final ResourceDiscoveryContext<MBeanResourceComponent<JMXComponent<?>>> context,
            final ObjectName objectName, final String objectNameAttribute) {
        final String name = objectName.getCanonicalName();
        final String beanName;
        if (objectNameAttribute != null) {
            final String value = objectName.getKeyProperty(objectNameAttribute);
            beanName = (value != null ? ObjectName.unquote(value) : name);
        } else {
            beanName = name;
        }
        
        final DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(),
                name, beanName, "", null, null, null);
        resource.getPluginConfiguration().setSimpleValue(NAME, beanName);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Discovered bean " + beanName);
        }

        return resource;
    }

    /**
     * Get the value of a PropertySimple as a string.
     * @param configuration The current configuration
     * @param propertyName The name of the property
     * @return The value of the property or null if not present or not a PropertySimple
     */
    private String getPropertyValue(final Configuration configuration,
            final String propertyName) {
        final Property property = configuration.get(propertyName);
        
        if ((property != null) && (property instanceof PropertySimple)) {
            return ((PropertySimple)property).getStringValue();
        } else {
            return null;
        }
    }
}
