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
package gist.ac.netcs.fwdtraffic.cli;

import gist.ac.netcs.fwdtraffic.FwdTrafficService;
import gist.ac.netcs.fwdtraffic.model.HostPair;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;

import java.util.concurrent.ConcurrentMap;

/**
 * Sample Apache Karaf CLI command
 */
@Command(scope = "onos", name = "fwdtraffic",
         description = "Sample Apache Karaf CLI command")
public class FwdTrafficCommand extends AbstractShellCommand{

    private static final String DEVICE_FMT = "========== %s ==========";
    private static final String HOST_FMT = "src=%s, dst=%s, counter=%d";

    @Argument(index = 0, name = "deviceId", description = "Device ID of switch", required = false, multiValued = false)
    private String deviceId = null;

    private FwdTrafficService service;



    private ConcurrentMap<DeviceId, ConcurrentMap<HostPair, Long>> map;
    @Override
    protected void execute() {
        service = get(FwdTrafficService.class);

        map = service.getMap();

        DeviceId device;



        if (deviceId != null) {
            device = DeviceId.deviceId(deviceId);

            if (!map.containsKey(device)) {
                return;
            }
            map.get(device).forEach((k, v) -> {
                print(HOST_FMT, k.getSrc(), k.getDst(), v);

                // print("Map2: %s", map.toString());
            });
            // printout all the host pair along with counter

        } else {
            for (DeviceId devId : map.keySet()) {
                print(DEVICE_FMT, devId);
                // printout the host pair along with counter that belongs to a device
                map.get(devId).forEach((k, v) -> {
                    print(HOST_FMT, k.getSrc(), k.getDst(), v);
                });
            }
        }
    }

}
