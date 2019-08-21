// Copyright 2019 Oath Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.model.container;

import com.yahoo.config.model.api.TlsSecrets;
import com.yahoo.config.model.api.container.ContainerServiceType;
import com.yahoo.config.model.producer.AbstractConfigProducer;
import com.yahoo.container.bundle.BundleInstantiationSpecification;
import com.yahoo.osgi.provider.model.ComponentModel;
import com.yahoo.prelude.fastsearch.FS4ResourcePool;
import com.yahoo.vespa.model.container.component.Component;
import com.yahoo.vespa.model.container.http.Http;
import com.yahoo.vespa.model.container.http.JettyHttpServer;
import com.yahoo.vespa.model.container.http.ssl.HostedSslConnectorFactory;

import java.util.Optional;

/**
 * A container that is typically used by container clusters set up from the user application.
 *
 * @author gjoranv
 */
public final class ApplicationContainer extends Container {

    private static final String defaultHostedJVMArgs = "-XX:+UseOSErrorReporting -XX:+SuppressFatalErrorMessage";

    private final boolean isHostedVespa;

    public ApplicationContainer(AbstractConfigProducer parent, String name, int index, boolean isHostedVespa, Optional<TlsSecrets> tlsSecrets, Optional<String> tlsCa) {
        this(parent, name, false, index, isHostedVespa, tlsSecrets, tlsCa);
    }

    public ApplicationContainer(AbstractConfigProducer parent, String name, boolean retired, int index, boolean isHostedVespa, Optional<TlsSecrets> tlsSecrets, Optional<String> tlsCa) {
        super(parent, name, retired, index);
        this.isHostedVespa = isHostedVespa;

        if (isHostedVespa && tlsSecrets.isPresent()) {

            JettyHttpServer server = Optional.ofNullable(getHttp())
                                             .map(Http::getHttpServer)
                                             .orElse(getDefaultHttpServer());
            String serverName = server.getComponentId().getName();
            var connectorFactory = tlsCa
                    .map(caCert -> new HostedSslConnectorFactory(serverName, tlsSecrets.get(), caCert))
                    .orElseGet(() -> new HostedSslConnectorFactory(serverName, tlsSecrets.get()));
            server.addConnector(connectorFactory);
        }
        addComponent(getFS4ResourcePool()); // TODO Remove when FS4 based search protocol is gone
    }

    private static Component<?, ComponentModel> getFS4ResourcePool() {
        BundleInstantiationSpecification spec = BundleInstantiationSpecification.
                getInternalSearcherSpecificationFromStrings(FS4ResourcePool.class.getName(), null);
        return new Component<>(new ComponentModel(spec));
    }


    @Override
    protected ContainerServiceType myServiceType() {
        if (parent instanceof ContainerCluster) {
            ContainerCluster cluster = (ContainerCluster)parent;
            // TODO: The 'qrserver' name is retained for legacy reasons (e.g. system tests and log parsing).
            if (cluster.getSearch() != null && cluster.getDocproc() == null && cluster.getDocumentApi() == null) {
                return ContainerServiceType.QRSERVER;
            }
        }
        return ContainerServiceType.CONTAINER;
    }

    /** Returns the jvm arguments this should start with */
    @Override
    public String getJvmOptions() {
        String jvmArgs = super.getJvmOptions();
        return isHostedVespa && hasDocproc()
                ? ("".equals(jvmArgs) ? defaultHostedJVMArgs : defaultHostedJVMArgs + " " + jvmArgs)
                : jvmArgs;
    }

    private boolean hasDocproc() {
        return (parent instanceof ContainerCluster) && (((ContainerCluster)parent).getDocproc() != null);
    }

}
