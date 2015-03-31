package com.hubspot.baragon.service.resources;

import java.net.ResponseCache;
import java.util.Collection;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.managers.ServiceManager;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonServiceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/state")
@Produces(MediaType.APPLICATION_JSON)
public class StateResource {
  private final ServiceManager serviceManager;

  @Inject
  public StateResource(ServiceManager serviceManager) {
    this.serviceManager = serviceManager;
  }

  @GET
  public Collection<BaragonServiceState> getAllServices() {
    return serviceManager.getAllServices();
  }

  @GET
  @Path("/{serviceId}")
  public Optional<BaragonServiceState> getService(@PathParam("serviceId") String serviceId) {
    return serviceManager.getService(serviceId);
  }


  @PUT
  @Path("/{serviceId}/reload")
  public BaragonResponse reloadConfigs(@PathParam("serviceId") String serviceId) {
    return serviceManager.enqueueReloadServiceConfigs(serviceId);
  }

  @DELETE
  @Path("/{serviceId}")
  public BaragonResponse removeService(@PathParam("serviceId") String serviceId) {
    return serviceManager.enqueueRemoveService(serviceId);
  }
}
