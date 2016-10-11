package gist.ac.netcs.netcsmon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import gist.ac.netcs.netcsmon.cli.Comparators;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.net.ConnectivityIntentCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static org.onosproject.net.intent.IntentState.FAILED;
import static org.onosproject.net.intent.IntentState.WITHDRAWN;

/**
 * Created by netcsnuc on 7/20/16.
 */

@Component(immediate = true)
@Command(scope = "onos", name = "netcstraffic", description = "change Flow based on Traffic")
public class IntentComponent extends ConnectivityIntentCommand {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService= get(IntentService.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;

    String state = null;
    String uri = null;

    private InternalHostListener hostListener = new InternalHostListener();


    //ConnectivityIntent connectivityIntent =get(ConnectivityIntent.class);


    private List<Host> hosts = Lists.newArrayList();
    private ApplicationId appId;

    private traffic trafficService;
    private long trafficValue;

    String applicationIdString = null;
    String keyString = null;
    private boolean purgeAfterRemove = false;
    private boolean sync = false;
    private static final EnumSet<IntentState> CAN_PURGE = EnumSet.of(WITHDRAWN, FAILED);

    //MultipleToSingle Intent
    String[] deviceStringsM1 = new String[]{"of:0000000000000001/1", "of:0000000000000001/2", "of:0000000000000001/3",
            "of:0000000000000001/4","of:0000000000000001/5","of:0000000000000001/6","of:0000000000000001/7",
            "of:0000000000000001/8","of:0000000000000001/9",
            "of:0000000000000001/10"};

    String[] deviceStringsM2 = new String[]{"of:0000000000000002/3", "of:0000000000000002/2",
            "of:0000000000000002/1" };

    String[] deviceStringsM3 = new String[]{"of:0000000000000003/3", "of:0000000000000003/2",
            "of:0000000000000003/1" };


    String[] deviceStringsM4 = new String[]{"of:0000000000000008/4", "of:0000000000000008/3",
            "of:0000000000000008/2" };
    String[] deviceStringsM5 = new String[]{"of:0000000000000009/1", "of:0000000000000009/2",
            "of:0000000000000009/3" };

    String[] deviceStringsM6 = new String[]{"of:0000000000000001/1", "of:0000000000000001/2", "of:0000000000000001/3",
            "of:0000000000000001/4",
            "of:0000000000000001/10"};
    String[] deviceStringsM7 = new String[]{"of:0000000000000001/5","of:0000000000000001/6","of:0000000000000001/7",
            "of:0000000000000001/8","of:0000000000000001/9",
            "of:0000000000000001/11"};


    //SingleToMultiple Intent
    String[] deviceStringsS1 = new String[]{"of:0000000000000001/10",
            "of:0000000000000001/1", "of:0000000000000001/2", "of:0000000000000001/3",
            "of:0000000000000001/4","of:0000000000000001/5","of:0000000000000001/6","of:0000000000000001/7",
            "of:0000000000000001/8","of:0000000000000001/9"};

    String[] deviceStringsS2 = new String[]{"of:0000000000000002/1",
            "of:0000000000000002/2","of:0000000000000002/3" };
    String[] deviceStringsS18 = new String[]{"of:0000000000000002/2",
            "of:0000000000000002/1","of:0000000000000002/3" };
    String[] deviceStringsS19 = new String[]{"of:0000000000000002/3",
            "of:0000000000000002/2","of:0000000000000002/1" };

    String[] deviceStringsS3 = new String[]{"of:0000000000000003/1",
            "of:0000000000000003/2","of:0000000000000003/3" };
    String[] deviceStringsS16 = new String[]{"of:0000000000000003/2",
            "of:0000000000000003/1","of:0000000000000003/3" };
    String[] deviceStringsS17 = new String[]{"of:0000000000000003/3",
            "of:0000000000000003/2","of:0000000000000003/1" };


    String[] deviceStringsS4 = new String[]{"of:0000000000000004/1",
            "of:0000000000000004/2","of:0000000000000004/3" };
    String[] deviceStringsS5 = new String[]{"of:0000000000000004/2",
            "of:0000000000000004/1","of:0000000000000004/3" };
    String[] deviceStringsS6 = new String[]{"of:0000000000000004/3",
            "of:0000000000000004/1","of:0000000000000004/2" };

    String[] deviceStringsS7 = new String[]{"of:0000000000000005/1",
            "of:0000000000000005/2","of:0000000000000005/3","of:0000000000000005/4","of:0000000000000005/5" };
    String[] deviceStringsS8 = new String[]{"of:0000000000000005/2",
            "of:0000000000000005/1","of:0000000000000005/3","of:0000000000000005/4","of:0000000000000005/5" };
    String[] deviceStringsS9 = new String[]{"of:0000000000000005/3",
            "of:0000000000000005/2","of:0000000000000005/1","of:0000000000000005/4","of:0000000000000005/5" };
    String[] deviceStringsS10 = new String[]{"of:0000000000000005/4",
            "of:0000000000000005/2","of:0000000000000005/3","of:0000000000000005/1","of:0000000000000005/5" };
    String[] deviceStringsS11 = new String[]{"of:0000000000000005/5",
            "of:0000000000000005/2","of:0000000000000005/3","of:0000000000000005/4","of:0000000000000005/1" };

    String[] deviceStringsS12 = new String[]{"of:0000000000000006/1",
            "of:0000000000000006/2","of:0000000000000006/3" };
    String[] deviceStringsS13 = new String[]{"of:0000000000000006/2",
            "of:0000000000000006/1","of:0000000000000006/3" };
    String[] deviceStringsS14 = new String[]{"of:0000000000000006/3",
            "of:0000000000000006/2","of:0000000000000006/1" };

    String[] deviceStringsS15 = new String[]{"of:0000000000000008/2",
            "of:0000000000000008/3","of:0000000000000008/4" };

    String[] deviceStringsS20 = new String[]{"of:0000000000000009/3",
            "of:0000000000000009/1","of:0000000000000009/2" };


    TrafficSelector selector =buildTrafficSelector();
    TrafficTreatment treatment = buildTrafficTreatment();
    IntentService service = get(IntentService.class);
    List<Constraint> constraints =buildConstraints();

    @Activate
    protected void activate()
    {
        appId = coreService.registerApplication("gist.ac.netcsmon");
        hostService.addListener(hostListener);


        log.info("Started Intent Component");

        MakeMultiToSingleIntent(deviceStringsM1);
        MakeMultiToSingleIntent(deviceStringsM2);
        MakeMultiToSingleIntent(deviceStringsM3);
        MakeMultiToSingleIntent(deviceStringsM4);
        MakeMultiToSingleIntent(deviceStringsM5);

        MakeSingleToMultiIntent(deviceStringsS1);
        MakeSingleToMultiIntent(deviceStringsS2);
        MakeSingleToMultiIntent(deviceStringsS3);
        MakeSingleToMultiIntent(deviceStringsS4);
        MakeSingleToMultiIntent(deviceStringsS5);
        MakeSingleToMultiIntent(deviceStringsS6);
        MakeSingleToMultiIntent(deviceStringsS7);
        MakeSingleToMultiIntent(deviceStringsS8);
        MakeSingleToMultiIntent(deviceStringsS9);
        MakeSingleToMultiIntent(deviceStringsS10);
        MakeSingleToMultiIntent(deviceStringsS11);
        MakeSingleToMultiIntent(deviceStringsS12);
        MakeSingleToMultiIntent(deviceStringsS13);
        MakeSingleToMultiIntent(deviceStringsS14);
        MakeSingleToMultiIntent(deviceStringsS15);
        MakeSingleToMultiIntent(deviceStringsS16);
        MakeSingleToMultiIntent(deviceStringsS17);
        MakeSingleToMultiIntent(deviceStringsS20);


       // MakeSingleToMultiIntent(deviceStringsS18);
      //  MakeSingleToMultiIntent(deviceStringsS19);


    }

    public void MakeSingleToMultiIntent(String[] deviceStringsS1) {
        String ingressDeviceString = deviceStringsS1[0];
        ConnectPoint ingressPoint = ConnectPoint.deviceConnectPoint(ingressDeviceString);

        Set<ConnectPoint> egressPoints = new HashSet<>();
        for (int index = 1; index < deviceStringsS1.length; index++) {
            String egressDeviceString = deviceStringsS1[index];
            ConnectPoint egress = ConnectPoint.deviceConnectPoint(egressDeviceString);
            egressPoints.add(egress);
        }

        TrafficSelector selector = buildTrafficSelector();
        TrafficTreatment treatment = buildTrafficTreatment();
        List<Constraint> constraints = buildConstraints();

        SinglePointToMultiPointIntent intent =
                SinglePointToMultiPointIntent.builder()
                        .appId(appId())
                        .key(key())
                        .selector(selector)
                        .treatment(treatment)
                        .ingressPoint(ingressPoint)
                        .egressPoints(egressPoints)
                        .constraints(constraints)
                        .priority(priority())
                        .build();
        service.submit(intent);


    }

    private void MakeMultiToSingleIntent(String[] deviceStringsM1) {
        String egressDeviceString = deviceStringsM1[deviceStringsM1.length - 1];
        ConnectPoint egress = ConnectPoint.deviceConnectPoint(egressDeviceString);
        Set<ConnectPoint> ingressPoints = new HashSet<>();
        for (int index = 0; index < deviceStringsM1.length - 1; index++) {
            String ingressDeviceString = deviceStringsM1[index];
            ConnectPoint ingress = ConnectPoint.deviceConnectPoint(ingressDeviceString);
            ingressPoints.add(ingress);
        }
        Intent intent = MultiPointToSinglePointIntent.builder()
                .appId(appId())
                .key(key())
                .selector(selector)
                .treatment(treatment)
                .ingressPoints(ingressPoints)
                .egressPoint(egress)
                .constraints(constraints)
                .priority(priority())
                .build();
        service.submit(intent);
    }
    private void removeIntent(IntentService intentService, Intent intent) {
        IntentListener listener = null;
        Key key = intent.key();
        final CountDownLatch withdrawLatch, purgeLatch;
        if (purgeAfterRemove || sync) {
            // set up latch and listener to track uninstall progress
            withdrawLatch = new CountDownLatch(1);
            purgeLatch = purgeAfterRemove ? new CountDownLatch(1) : null;
            listener = (IntentEvent event) -> {
                if (Objects.equals(event.subject().key(), key)) {
                    if (event.type() == IntentEvent.Type.WITHDRAWN ||
                            event.type() == IntentEvent.Type.FAILED) {
                        withdrawLatch.countDown();
                    } else if (purgeAfterRemove &&
                            event.type() == IntentEvent.Type.PURGED) {
                        purgeLatch.countDown();
                    }
                }
            };
            intentService.addListener(listener);
        } else {
            purgeLatch = null;
            withdrawLatch = null;
        }

        // request the withdraw
        intentService.withdraw(intent);

        if (purgeAfterRemove || sync) {
            try { // wait for withdraw event
                withdrawLatch.await(0, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                print("Timed out waiting for intent {} withdraw", key);
            }
            if (purgeAfterRemove && CAN_PURGE.contains(intentService.getIntentState(key))) {
                intentService.purge(intent);
                if (sync) { // wait for purge event
                    /* TODO
                       Technically, the event comes before map.remove() is called.
                       If we depend on sync and purge working together, we will
                       need to address this.
                    */
                    try {
                        purgeLatch.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        print("Timed out waiting for intent {} purge", key);
                    }
                }
            }
        }

        if (listener != null) {
            // clean up the listener
            intentService.removeListener(listener);
        }
    }
    @Deactivate
    protected void deactivate()
    {
        hostService.removeListener(hostListener);
        log.info("Stopped Intent Component");
    }

    @Override
    protected void execute() {
        CoreService coreService = get(CoreService.class);
        DeviceService deviceService = get(DeviceService.class);
        FlowRuleService flowservice = get(FlowRuleService.class);
        SortedMap<Device, List<FlowEntry>> flows;
        flows= getSortedFlows(deviceService, flowservice);
        IntentService intentService = get(IntentService.class);
        if (outputJson()) {
            print("%s", json(flows.keySet(), flows));
        } else {
            flows.forEach((device, flow) -> printFlows(device, flow, coreService));
        }
    }



    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent hostEvent) {
            switch (hostEvent.type()) {

                case HOST_ADDED:
                    addConnectivity(hostEvent.subject());
                    hosts.add(hostEvent.subject());
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

    private void addConnectivity(Host host){
        for (Host dst : hosts){
            //HostToHostIntent intent = HostToHostIntent.builder().appId(appId).one(host.id()).two(dst.id()).build();
            //intentService.submit(intent);
        //    PointToPointIntent intent = PointToPointIntent.builder().appId(appId).selector(selector)
       //             .treatment(treatment).ingressPoint(ingress).egressPoint(egress).constraints(constraints)
         //           .priority(priority()).build();
        //    intentService.submit(intent);

        }
    }
    private JsonNode json(Iterable<Device> devices,
                          Map<Device, List<FlowEntry>> flows) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Device device : devices) {
            result.add(json(mapper, device, flows.get(device)));
        }
        return result;
    }
    private ObjectNode json(ObjectMapper mapper,
                            Device device, List<FlowEntry> flows) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode array = mapper.createArrayNode();

        flows.forEach(flow -> array.add(jsonForEntity(flow, FlowEntry.class)));

        result.put("device", device.id().toString())
                .put("flowCount", flows.size())
                .set("flows", array);
        return result;
    }
    SortedMap<Device, List<FlowEntry>> getSortedFlows(DeviceService deviceService,
                                                      FlowRuleService service) {

        SortedMap<Device, List<FlowEntry>> flows = new TreeMap<>(Comparators.ELEMENT_COMPARATOR);
        List<FlowEntry> rules;
        FlowEntry.FlowEntryState s = null;
        if (state != null && !state.equals("any")) {
            s = FlowEntry.FlowEntryState.valueOf(state.toUpperCase());
        }
        Iterable<Device> devices = uri == null ? deviceService.getDevices() :
                Collections.singletonList(deviceService.getDevice(DeviceId.deviceId(uri)));
        for (Device d : devices) {
            if (s == null) {
                rules = newArrayList(service.getFlowEntries(d.id()));
            } else {
                rules = newArrayList();
                for (FlowEntry f : service.getFlowEntries(d.id())) {
                    if (f.state().equals(s)) {
                        rules.add(f);
                    }
                }
            }
            rules.sort(Comparators.FLOW_RULE_COMPARATOR);
            flows.put(d, rules);
        }
        return flows;
    }
    protected void printFlows(Device d, List<FlowEntry> flows,
                              CoreService coreService) {
        boolean empty = flows == null || flows.isEmpty();
        long packetSum=0;
        long packetBytes=0;
        //print("deviceId=%s, flowRuleCount=%d", d.id(), empty ? 0 : flows.size());
        if (!empty) {
            for (FlowEntry f : flows) {
                ApplicationId appId = coreService.getAppId(f.appId());
                packetSum= packetSum+f.packets();
                packetBytes= packetBytes+f.bytes();
            }
           // print("Total Packet number: %d",packetSum);
           // print("Total Packet size(bytes): %d",packetBytes);
            if(d.id().toString().equals("of:0000000000000001")){
                if (packetBytes>125){

                        log.info("1111111111111111");
                        if (isNullOrEmpty(keyString)) {
                            for (Intent intent : intentService.getIntents()) {
                                if (intent.appId().equals(appId)) {
                                    removeIntent(intentService, intent);
                                }
                            }
                        }
                        MakeMultiToSingleIntent(deviceStringsM2);
                        MakeMultiToSingleIntent(deviceStringsM3);
                        MakeMultiToSingleIntent(deviceStringsM4);
                        MakeMultiToSingleIntent(deviceStringsM5);
                        MakeMultiToSingleIntent(deviceStringsM6);
                        MakeMultiToSingleIntent(deviceStringsM7);

                        MakeSingleToMultiIntent(deviceStringsS1);
                        MakeSingleToMultiIntent(deviceStringsS2);
                        MakeSingleToMultiIntent(deviceStringsS3);
                        MakeSingleToMultiIntent(deviceStringsS4);
                        MakeSingleToMultiIntent(deviceStringsS5);
                        MakeSingleToMultiIntent(deviceStringsS6);
                        MakeSingleToMultiIntent(deviceStringsS7);
                        MakeSingleToMultiIntent(deviceStringsS8);
                        MakeSingleToMultiIntent(deviceStringsS9);
                        MakeSingleToMultiIntent(deviceStringsS10);
                        MakeSingleToMultiIntent(deviceStringsS11);
                        MakeSingleToMultiIntent(deviceStringsS12);
                        MakeSingleToMultiIntent(deviceStringsS13);
                        MakeSingleToMultiIntent(deviceStringsS14);
                        MakeSingleToMultiIntent(deviceStringsS15);
                        MakeSingleToMultiIntent(deviceStringsS16);
                        MakeSingleToMultiIntent(deviceStringsS17);
                        MakeSingleToMultiIntent(deviceStringsS20);
                }
            }
        }
    }
}
