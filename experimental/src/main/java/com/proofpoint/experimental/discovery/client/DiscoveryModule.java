package com.proofpoint.experimental.discovery.client;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.proofpoint.experimental.discovery.client.HttpDiscoveryClient.ServiceDescriptorsRepresentation;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static com.proofpoint.configuration.ConfigurationModule.bindConfig;
import static com.proofpoint.experimental.json.JsonCodecBinder.jsonCodecBinder;

public class DiscoveryModule implements Module
{
    @Override
    public void configure(Binder binder)
    {
        // bind discovery client and dependencies
        binder.bind(DiscoveryClient.class).to(HttpDiscoveryClient.class).in(Scopes.SINGLETON);
        bindConfig(binder).to(DiscoveryClientConfig.class);
        jsonCodecBinder(binder).bindJsonCodec(ServiceDescriptorsRepresentation.class);
        jsonCodecBinder(binder).bindJsonCodec(Announcement.class);

        // bind announcer
        binder.bind(Announcer.class).in(Scopes.SINGLETON);
        // Must create a multibinder for service announcements or construction will fail if no
        // service announcements are bound, which is legal for processes that don't have public services
        Multibinder.newSetBinder(binder, ServiceAnnouncement.class);

    }

    @Provides
    @ForDiscoverClient
    public ScheduledExecutorService createDiscoveryExecutor()
    {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(10, new ThreadFactoryBuilder().setNameFormat("Discovery-%s").build());
        return MoreExecutors.getExitingScheduledExecutorService(executor);
    }
}