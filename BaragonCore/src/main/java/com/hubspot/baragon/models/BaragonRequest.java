package com.hubspot.baragon.models;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonRequest {
  @NotNull
  @Pattern(regexp = "[^\\s/|]+", message = "cannot contain whitespace, '/', or '|'", flags = Pattern.Flag.MULTILINE)
  private final String loadBalancerRequestId;

  @NotNull
  @Valid
  private final BaragonService loadBalancerService;

  @NotNull
  private final List<UpstreamInfo> addUpstreams;

  @NotNull
  private final List<UpstreamInfo> removeUpstreams;

  public static BaragonRequestBuilder builder(String loadBalancerRequestId, BaragonService loadBalancerService) {
    return new BaragonRequestBuilder(loadBalancerRequestId, loadBalancerService);
  }

  @JsonCreator
  public BaragonRequest(@JsonProperty("loadBalancerRequestId") String loadBalancerRequestId,
                        @JsonProperty("loadBalancerService") BaragonService loadBalancerService,
                        @JsonProperty("addUpstreams") List<UpstreamInfo> addUpstreams,
                        @JsonProperty("removeUpstreams") List<UpstreamInfo> removeUpstreams) {
    this.loadBalancerRequestId = loadBalancerRequestId;
    this.loadBalancerService = loadBalancerService;
    this.addUpstreams = addRequestId(addUpstreams, loadBalancerRequestId);
    this.removeUpstreams = addRequestId(removeUpstreams, loadBalancerRequestId);
  }

  public String getLoadBalancerRequestId() {
    return loadBalancerRequestId;
  }

  public BaragonService getLoadBalancerService() {
    return loadBalancerService;
  }

  public List<UpstreamInfo> getAddUpstreams() {
    return addUpstreams;
  }

  public List<UpstreamInfo> getRemoveUpstreams() {
    return removeUpstreams;
  }

  private List<UpstreamInfo> addRequestId(List<UpstreamInfo> upstreams, String requestId) {
    if (upstreams == null || requestId == null) {
      return upstreams;
    }

    List<UpstreamInfo> upstreamsWithRequestId = Lists.newArrayListWithCapacity(upstreams.size());
    for (UpstreamInfo upstream : upstreams) {
      upstreamsWithRequestId.add(addRequestId(upstream, requestId));
    }

    return upstreamsWithRequestId;
  }

  private UpstreamInfo addRequestId(UpstreamInfo upstream, String requestId) {
    if (!upstream.getRequestId().isPresent()) {
      return new UpstreamInfo(upstream.getUpstream(), Optional.of(requestId), upstream.getRackId());
    } else {
      return upstream;
    }
  }

  @Override
  public String toString() {
    return "BaragonRequest [" +
        "loadBalancerRequestId='" + loadBalancerRequestId + '\'' +
        ", loadBalancerService=" + loadBalancerService +
        ", addUpstreams=" + addUpstreams +
        ", removeUpstreams=" + removeUpstreams +
        ']';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BaragonRequest request = (BaragonRequest) o;

    if (!addUpstreams.equals(request.addUpstreams)) return false;
    if (!loadBalancerRequestId.equals(request.loadBalancerRequestId)) return false;
    if (!loadBalancerService.equals(request.loadBalancerService)) return false;
    if (!removeUpstreams.equals(request.removeUpstreams)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = loadBalancerRequestId.hashCode();
    result = 31 * result + loadBalancerService.hashCode();
    result = 31 * result + addUpstreams.hashCode();
    result = 31 * result + removeUpstreams.hashCode();
    return result;
  }
}
