package com.hubspot.baragon.models;

import java.util.Collections;
import java.util.List;

public class BaragonRequestBuilder {
  private String loadBalancerRequestId;
  private BaragonService loadBalancerService;
  private List<UpstreamInfo> addUpstreams;
  private List<UpstreamInfo> removeUpstreams;

  public BaragonRequestBuilder(String loadBalancerRequestId, BaragonService loadBalancerService) {
    this.loadBalancerRequestId = loadBalancerRequestId;
    this.loadBalancerService = loadBalancerService;
    this.addUpstreams = Collections.emptyList();
    this.removeUpstreams = Collections.emptyList();
  }

  public BaragonRequestBuilder(BaragonRequest request) {
    this.loadBalancerRequestId = request.getLoadBalancerRequestId();
    this.loadBalancerService = request.getLoadBalancerService();
    this.addUpstreams = request.getAddUpstreams();
    this.removeUpstreams = request.getRemoveUpstreams();
  }

  public String getLoadBalancerRequestId() {
    return loadBalancerRequestId;
  }

  public BaragonRequestBuilder setLoadBalancerRequestId(String loadBalancerRequestId) {
    this.loadBalancerRequestId = loadBalancerRequestId;
    return this;
  }

  public BaragonService getLoadBalancerService() {
    return loadBalancerService;
  }

  public BaragonRequestBuilder setLoadBalancerService(BaragonService loadBalancerService) {
    this.loadBalancerService = loadBalancerService;
    return this;
  }

  public List<UpstreamInfo> getAddUpstreams() {
    return addUpstreams;
  }

  public BaragonRequestBuilder setAddUpstreams(List<UpstreamInfo> addUpstreams) {
    this.addUpstreams = addUpstreams;
    return this;
  }

  public List<UpstreamInfo> getRemoveUpstreams() {
    return removeUpstreams;
  }

  public BaragonRequestBuilder setRemoveUpstreams(List<UpstreamInfo> removeUpstreams) {
    this.removeUpstreams = removeUpstreams;
    return this;
  }

  public BaragonRequest build() {
    return new BaragonRequest(loadBalancerRequestId, loadBalancerService, addUpstreams, removeUpstreams);
  }
}
