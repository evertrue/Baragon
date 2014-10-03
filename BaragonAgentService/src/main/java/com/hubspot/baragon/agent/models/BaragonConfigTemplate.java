package com.hubspot.baragon.agent.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jknack.handlebars.Template;
import com.google.common.base.Objects;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonConfigTemplate {
  private final String filename;
  private final Template template;

  @JsonCreator
  public BaragonConfigTemplate(@JsonProperty("filename") String filename,
                               @JsonProperty("template") Template template) {
    this.filename = filename;
    this.template = template;
  }

  public String getFilename() {
    return filename;
  }

  public Template getTemplate() {
    return template;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(BaragonConfigTemplate.class)
        .add("filename", filename)
        .add("template", template)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(filename, template);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BaragonConfigTemplate that = (BaragonConfigTemplate) o;

    if (!filename.equals(that.filename)) return false;
    if (!template.equals(that.template)) return false;

    return true;
  }
}
