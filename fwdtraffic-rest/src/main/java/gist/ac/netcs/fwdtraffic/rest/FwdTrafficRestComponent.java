/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gist.ac.netcs.fwdtraffic.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gist.ac.netcs.fwdtraffic.FwdTrafficService;
import gist.ac.netcs.fwdtraffic.model.HostPair;
import org.onosproject.net.DeviceId;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.ConcurrentMap;

/**
 * Skeletal ONOS application component.
 */
@Path("fwdtraffic")
public class FwdTrafficRestComponent extends AbstractWebResource {

    protected FwdTrafficService service;

    private ConcurrentMap<DeviceId, ConcurrentMap<HostPair, Long>> map;

    private ObjectNode convert(HostPair hp, Long counter) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode on = mapper.createObjectNode();
        on.put("src", hp.getSrc().toString());
        on.put("dst", hp.getDst().toString());
        on.put("counter", counter);
        return on;
    }
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        service = get(FwdTrafficService.class);
        map = service.getMap();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode dev = mapper.createArrayNode();
        ObjectNode element = mapper().createObjectNode();

        for (DeviceId devId : map.keySet()){
            ArrayNode flow = mapper().createArrayNode();
            map.get(devId).forEach((k,v)-> flow.add(convert(k,v)));
            element.set(devId.toString(),flow);
        }


        // iterate the map, extract the element and fill up element JSON obj
        // hint: need to generate flow ArrayNode first, add converted
        // JSON object of host pair and counter into the flow
        // Add flow into element by specifying switch id


        dev.add(element);
        root.set("data", dev);
        return ok(root).build();
    }

    /*
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}")
    public Response getByDevice(@PathParam("deviceId") String deviceId) {

    }
    */

}
