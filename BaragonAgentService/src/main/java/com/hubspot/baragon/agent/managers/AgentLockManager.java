package com.hubspot.baragon.agent.managers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.exceptions.BaragonAgentOccupiedException;

@Singleton
public class AgentLockManager {
  private final Lock lock;
  private final long lockTimeoutMs;

  private Optional<Long> lockedAt;
  private Optional<String> activeRequestId;

  @Inject
  public AgentLockManager(@Named(BaragonAgentServiceModule.AGENT_LOCK_TIMEOUT_MS) long lockTimeoutMs) {
    this.lock = new ReentrantLock();
    this.lockedAt = Optional.absent();
    this.activeRequestId = Optional.absent();

    this.lockTimeoutMs = lockTimeoutMs;
  }

  public synchronized void lockOrThrow(Optional<String> maybeRequestId) throws BaragonAgentOccupiedException, InterruptedException {
    if (lock.tryLock(lockTimeoutMs, TimeUnit.MILLISECONDS)) {
      lockedAt = Optional.of(System.currentTimeMillis());
      activeRequestId = maybeRequestId;
    } else {
      throw new BaragonAgentOccupiedException(activeRequestId, lockedAt.get()); // TODO: arg
    }
  }

  public void unlock() {
    lock.unlock();
  }

  public Optional<Long> getLockedAt() {
    return lockedAt;
  }

  public Optional<String> getActiveRequestId() {
    return activeRequestId;
  }
}
