package com.hubspot.baragon.models;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BaragonServiceBuilder {
  private String serviceId;
  private Collection<String> owners;
  private String serviceBasePath;
  private List<String> loadBalancerGroups;
  private Map<String, Object> options;

  public BaragonServiceBuilder(String serviceId, String serviceBasePath) {
    this.serviceId = serviceId;
    this.owners = Collections.emptyList();
    this.serviceBasePath = serviceBasePath;
    this.loadBalancerGroups = Collections.emptyList();
    this.options = Collections.emptyMap();
  }

  public BaragonServiceBuilder(BaragonService service) {
    this.serviceId = service.getServiceId();
    this.owners = service.getOwners();
    this.serviceBasePath = service.getServiceBasePath();
    this.loadBalancerGroups = service.getLoadBalancerGroups();
    this.options = service.getOptions();
  }

  public String getServiceId() {
    return serviceId;
  }

  public BaragonServiceBuilder setServiceId(String serviceId) {
    this.serviceId = serviceId;
    return this;
  }

  public Collection<String> getOwners() {
    return owners;
  }

  public BaragonServiceBuilder setOwners(Collection<String> owners) {
    this.owners = owners;
    return this;
  }

  public String getServiceBasePath() {
    return serviceBasePath;
  }

  public BaragonServiceBuilder setServiceBasePath(String serviceBasePath) {
    this.serviceBasePath = serviceBasePath;
    return this;
  }

  public List<String> getLoadBalancerGroups() {
    return loadBalancerGroups;
  }

  public BaragonServiceBuilder setLoadBalancerGroups(List<String> loadBalancerGroups) {
    this.loadBalancerGroups = loadBalancerGroups;
    return this;
  }

  public Map<String, Object> getOptions() {
    return options;
  }

  public BaragonServiceBuilder setOptions(Map<String, Object> options) {
    this.options = options;
    return this;
  }

  public BaragonService build() {
    return new BaragonService(serviceId, owners, serviceBasePath, loadBalancerGroups, options);
  }
}
