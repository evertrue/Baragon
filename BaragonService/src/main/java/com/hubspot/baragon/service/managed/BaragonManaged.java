package com.hubspot.baragon.service.managed;

import com.hubspot.baragon.service.listeners.AbstractLatchListener;
import io.dropwizard.lifecycle.Managed;

import java.util.concurrent.ScheduledExecutorService;
import java.util.Set;

import javax.inject.Named;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.BaragonConfiguration;


public class BaragonManaged implements Managed {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonManaged.class);

  private final ScheduledExecutorService executorService;
  private final LeaderLatch leaderLatch;
  private final BaragonConfiguration config;
  private final Set<AbstractLatchListener> listeners;

  @Inject
  public BaragonManaged(Set<AbstractLatchListener> listeners,
                        @Named(BaragonServiceModule.BARAGON_SERVICE_SCHEDULED_EXECUTOR) ScheduledExecutorService executorService,
                        @Named(BaragonDataModule.BARAGON_SERVICE_LEADER_LATCH) LeaderLatch leaderLatch,
                        BaragonConfiguration config) {
    this.listeners = listeners;
    this.executorService = executorService;
    this.leaderLatch = leaderLatch;
    this.config = config;
  }

  @Override
  public void start() throws Exception {
    for (AbstractLatchListener listener : listeners) {
      if (listener.isEnabled()) {
        leaderLatch.addListener(listener);
      }
    }
    leaderLatch.start();
  }

  @Override
  public void stop() throws Exception {
    leaderLatch.close();
    executorService.shutdown();
  }
}
