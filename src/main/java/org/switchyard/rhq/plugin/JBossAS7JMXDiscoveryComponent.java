package org.switchyard.rhq.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.ClassLoaderFacet;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Based on the RHQ version, this is the parent resource for all SwitchYard MBean resources.
 */
public class JBossAS7JMXDiscoveryComponent<T extends ResourceComponent<JBossAS7JMXComponent<?>>>
    implements ResourceDiscoveryComponent<T>, ClassLoaderFacet<ResourceComponent<JBossAS7JMXComponent<?>>>
{
    /**
     * The logger instance.
     */
    private static Log LOG = LogFactory.getLog(JBossAS7JMXDiscoveryComponent.class);

    /**
     * Get any additional classpath elements for this resource.
     * @param context The context for the current discovery component.
     * @param details Details of the discovered resource.
     * @throws MalformedURLException if the discovered classpath URLs are invalid.
     * @return The additional classpath URLs for this resource.
     */
    @Override
    public List<URL> getAdditionalClasspathUrls(
        final ResourceDiscoveryContext<ResourceComponent<JBossAS7JMXComponent<?>>> context, DiscoveredResourceDetails details)
            throws MalformedURLException {

        final Configuration pluginConfig = details.getPluginConfiguration();
        final String clientJarLocation = pluginConfig.getSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_CLIENT_JAR_LOCATION);
        if (clientJarLocation == null) {
            LOG.warn("Missing the client jar location - cannot connect to the JBossAS instance: "
                + details.getResourceKey());
            return null;
        }

        File clientJarDir = new File(clientJarLocation);
        if (!clientJarDir.isDirectory()) {
            LOG.warn("The client jar location [" + clientJarDir.getAbsolutePath()
                + "] does not exist - cannot connect to the JBossAS instance: " + details.getResourceKey());
            return null;
        }

        ArrayList<URL> clientJars = new ArrayList<URL>();
        for (File clientJarFile : clientJarDir.listFiles()) {
            if (clientJarFile.getName().endsWith(".jar")) {
                clientJars.add(clientJarFile.toURI().toURL());
            }
        }

        if (clientJars.size() == 0) {
            LOG.warn("The client jar location [" + clientJarDir.getAbsolutePath()
                + "] is missing client jars - cannot connect to the JBossAS instance: " + details.getResourceKey());
            return null;
        }

        return clientJars;
    }

    /**
     * Discover existing resources.
     * @param context The context for the current discovery component.
     * @return The discovered resources. 
     */
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(final ResourceDiscoveryContext<T> context) {

        final HashSet<DiscoveredResourceDetails> result = new HashSet<DiscoveredResourceDetails>();
        final ResourceContext<?> parentResourceContext = context.getParentResourceContext();
        final Configuration parentPluginConfig = parentResourceContext.getPluginConfiguration();

        // TODO: use additional methods to look around for other places where this can be find
        // for example, we might be able to look in the /modules directory for some jars to
        // use if the bin/client dir is gone.  Also, if for some reason we shouldn't use this jar,
        // we may need to instead use various additional jars required by jbossas/bin/jconsole.sh.
        File clientJarDir = null;

        final String homeDirStr = parentPluginConfig.getSimpleValue("homeDir");
        if (homeDirStr != null) {
            final File homeDirFile = new File(homeDirStr);
            if (homeDirFile.exists()) {
                clientJarDir = new File(homeDirFile, "bin/client");
                if (!clientJarDir.exists()) {
                    LOG.warn("The client jar location [" + clientJarDir.getAbsolutePath()
                        + "] does not exist - will not be able to connect to the AS7 instance");
                }
            }
        }

        final String clientJarLocation = (clientJarDir != null) ? clientJarDir.getAbsolutePath() : null;

        final String key = "SwitchYardSubsystem";
        final String name = "SwitchYard";
        final String version = parentResourceContext.getVersion();
        final String description = "Container for SwitchYard services";

        Configuration pluginConfig = context.getDefaultPluginConfiguration();

        final String hostname = parentPluginConfig.getSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_HOSTNAME, "127.0.0.1");
        pluginConfig.setSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_HOSTNAME, hostname);

        String port = parentPluginConfig.getSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_PORT,
            JBossAS7JMXComponent.DEFAULT_PLUGIN_CONFIG_PORT);
        if (!JBossAS7JMXComponent.DEFAULT_PLUGIN_CONFIG_PORT.equals(port)) {
            port = String.valueOf(Integer.valueOf(port) + 9);
        }
        pluginConfig.setSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_PORT, port);

        final String user = parentPluginConfig.getSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_USERNAME, "rhqadmin");
        pluginConfig.setSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_USERNAME, user);

        final String password = parentPluginConfig.getSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_PASSWORD, "rhqadmin");
        pluginConfig.setSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_PASSWORD, password);

        pluginConfig.setSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_CLIENT_JAR_LOCATION, clientJarLocation);

        final DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, pluginConfig, null);

        result.add(resource);

        return result;
    }
}
