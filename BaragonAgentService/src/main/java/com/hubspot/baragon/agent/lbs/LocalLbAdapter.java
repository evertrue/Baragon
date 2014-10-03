package com.hubspot.baragon.agent.lbs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.agent.config.LoadBalancerConfiguration;
import com.hubspot.baragon.exceptions.InvalidConfigException;
import com.hubspot.baragon.exceptions.LbAdapterExecuteException;

@Singleton
public class LocalLbAdapter {
  private final LoadBalancerConfiguration loadBalancerConfiguration;

  @Inject
  public LocalLbAdapter(LoadBalancerConfiguration loadBalancerConfiguration) {
    this.loadBalancerConfiguration = loadBalancerConfiguration;
  }
  
  private void executeWithTimeout(CommandLine command, int timeout) throws LbAdapterExecuteException, IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DefaultExecutor executor = new DefaultExecutor();
    
    executor.setStreamHandler(new PumpStreamHandler(baos));
    executor.setWatchdog(new ExecuteWatchdog(timeout));
    
    try {
      executor.execute(command);
    } catch (ExecuteException e) {
      throw new LbAdapterExecuteException(baos.toString(), e);
    }
  }

  public void checkConfigs() throws InvalidConfigException {
    try {
      executeWithTimeout(CommandLine.parse(loadBalancerConfiguration.getCheckConfigCommand()), loadBalancerConfiguration.getCommandTimeoutMs());
    } catch (LbAdapterExecuteException e) {
      System.out.println("!!! LbAdapterExecuteException: " + e.getOutput());
      throw new InvalidConfigException(e.getOutput());
    } catch (IOException e) {
      System.out.println("!!! IOException: " + e.getMessage());
      throw new InvalidConfigException(e.getMessage());
    }
  }

  public void reloadConfigs() throws LbAdapterExecuteException, IOException {
    executeWithTimeout(CommandLine.parse(loadBalancerConfiguration.getReloadConfigCommand()), loadBalancerConfiguration.getCommandTimeoutMs());
  }
}
