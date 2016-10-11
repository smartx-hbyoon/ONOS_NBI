package gist.ac.netcs.netcsmon.cli;

import gist.ac.netcs.netcsmon.NetcsMonService;
import gist.ac.netcs.netcsmon.model.HostPair;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;

import java.util.concurrent.ConcurrentMap;

@Command(scope = "onos", name = "netcsmon", description = "Lists the requested host pair the counter")
public class NetcsMonCommand extends AbstractShellCommand {


    private static final String DEVICE_FMT = "========== %s ==========";
    private static final String HOST_FMT = "src=%s, dst=%s, counter=%d";

    @Argument(index = 0, name = "deviceId", description = "Device ID of switch", required = false, multiValued = false)
    private String deviceId = null;

    private NetcsMonService service;
    

    private ConcurrentMap<DeviceId, ConcurrentMap<HostPair, Long>> map;
    @Override
    protected void execute() {
        service = get(NetcsMonService.class);

        map = service.getMap();

        DeviceId device;

        if(map != null){
        if (deviceId != null) {
            device = DeviceId.deviceId(deviceId);
            if (!map.containsKey(device)) {
                return;
            }
            map.get(device).forEach((k, v) -> {
                print(HOST_FMT, k.getSrc(), k.getDst(), v);

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

}