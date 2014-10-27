package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

import java.util.Map;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonAgentMetadata {
  private final String baseAgentUri;
  private final Optional<String> domain;
  private final Optional<String> hostname;
  private final Optional<Map<String, String>> metadata;

  @JsonCreator
  public static BaragonAgentMetadata fromString(String value) {
    return new BaragonAgentMetadata(value, Optional.<String>absent(), Optional.<String>absent(), Optional.<Map<String, String>>absent());
  }

  @JsonCreator
  public BaragonAgentMetadata(@JsonProperty("baseAgentUri") String baseAgentUri,
                              @JsonProperty("domain") Optional<String> domain,
                              @JsonProperty("hostname") Optional<String> hostname,
                              @JsonProperty("metadata") Optional<Map<String, String>> metadata) {
    this.baseAgentUri = baseAgentUri;
    this.domain = domain;
    this.hostname = hostname;
    this.metadata = metadata;
  }

  public String getBaseAgentUri() {
    return baseAgentUri;
  }

  public Optional<String> getDomain() {
    return domain;
  }

  public Optional<String> getHostname() {
    return hostname;
  }

  public Optional<Map<String, String>> getMetadata() {
    return metadata;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("baseAgentUri", baseAgentUri)
        .add("domain", domain)
        .add("hostname", hostname)
        .add("metadata", metadata)
        .toString();
  }
}
