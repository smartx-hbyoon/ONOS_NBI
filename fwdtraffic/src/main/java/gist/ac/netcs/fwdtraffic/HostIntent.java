package gist.ac.netcs.fwdtraffic;

import com.google.common.collect.Lists;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.IntentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by netcsnuc on 2/23/16.
 */

@Component(immediate = true)
@Service
public class HostIntent extends AbstractShellCommand implements FirstHost {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;

    private InternalHostListener hostListener = new InternalHostListener();

    private List<Host> hosts = Lists.newArrayList();
    private ApplicationId appId;

    private FwdTrafficService service;
    private long PacketBytes;
    private boolean ss;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("gist.ac.netcs.fwdtraffic");
       // hostService.addListener(hostListener);
      //  execute();
        log.info("Started Intent heebumheebum ");
    }

    @Deactivate
    protected void deactivate() {
      //  hostService.removeListener(hostListener);
        log.info("Stopped Intent ");
    }


    @Override
    public boolean firstHost() {
        return true;
    }

    @Override
    protected void execute() {
      //  service = get(FwdTrafficService.class);
     //   PacketBytes = service.getPacketBytes();
     //   log.info("PacketBytes Host Intent {}",PacketBytes);
    }

    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent hostEvent) {
            switch (hostEvent.type()) {

                case HOST_ADDED:
                    //execute();
                   // accConnectivity(hostEvent.subject());
                    //firstHost();
                    hosts.add(hostEvent.subject());
                    log.info("intent host: {}",hostEvent.subject().toString());
                    break;
                case HOST_REMOVED:
                    break;
                case HOST_UPDATED:
                    break;
                case HOST_MOVED:
                    break;
            }
        }
    }

    private void accConnectivity(Host host) {
        for (Host dst : hosts){
            HostToHostIntent intent = HostToHostIntent.builder().appId(appId).one(host.id()).two(dst.id()).build();
            intentService.submit(intent);
        }
    }
}
