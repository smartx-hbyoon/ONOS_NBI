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
package gist.ac.netcs.fwdtraffic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import gist.ac.netcs.fwdtraffic.model.HostPair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onosproject.cli.Comparators;
import org.onosproject.cli.net.ConnectivityIntentCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
@Service
public class FwdTrafficComponent extends ConnectivityIntentCommand implements FwdTrafficService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ConcurrentMap<DeviceId, ConcurrentMap<HostPair, Long>> map;
    long packetSum=0;
    long packetBytes=0;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    DeviceService deviceService = get(DeviceService.class);
    FlowRuleService service = get(FlowRuleService.class);
    SortedMap<Device, List<FlowEntry>> flows = getSortedFlows(deviceService, service);

    HostId src=null;
    HostId dst=null;

    private FwdTrafficPacketProcessor processor = new FwdTrafficPacketProcessor();
    String state = null;
    String uri = null;

    private ApplicationId appId;

    TrafficSelector selector = buildTrafficSelector();
    TrafficTreatment treatment = buildTrafficTreatment();
    List<Constraint> constraints = buildConstraints();
    private List<Host> hosts = Lists.newArrayList();
    private InternalHostListener hostListener = new InternalHostListener();

    private boolean intentsSummary = false;
    private boolean pending = false;
    private boolean showInstallable = false;
    public boolean ss=true;
    HostEvent hostEvent;


    @Activate
    protected void activate() {
        appId = coreService.registerApplication("gist.ac.netcs.fwdtraffic");
        map = new ConcurrentHashMap<DeviceId, ConcurrentMap<HostPair, Long>>();

        packetService.addProcessor(processor, PacketProcessor.ADVISOR_MAX + 10);
        requestPackests();

        log.info("Started with Application ID {}", appId.id());
        hostService.addListener(hostListener);
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;
        hostService.removeListener(hostListener);
        log.info("Stopped with Application ID {}", appId.id());
    }
    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent hostEvent) {
            switch (hostEvent.type()) {

                case HOST_ADDED:
                    //execute();
                     accConnectivity(hostEvent.subject());
                    //firstHost();
                    ss=true;
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
     //   for (Host dst : hosts){

            print("host: %s",host.id().toString());
            //print("dst: %s",dst.toString());
            if(host.id().toString().contains("B8:27:EB")){

                HostId twoId = HostId.hostId("B8:AE:ED:79:CA:21/-1");
                HostToHostIntent intent = HostToHostIntent.builder().appId(appId).one(host.id()).two(twoId).build();
                intentService.submit(intent);
                print("Host to Host intent submitted:\n%s", intent.toString());
            }

       // }
    }

    @Override
    public void execute() {

        IntentService service = get(IntentService.class);

        if (intentsSummary) {
            IntentSummaries intentSummaries = new IntentSummaries();
            intentSummaries.collectIntentSummary(service,
                                                 service.getIntents());
            if (outputJson()) {
                print("%s", intentSummaries.json());
                print("0000000\n");
            } else {
                intentSummaries.printSummary();
            }
            return;
        } else if (pending) {
            if (outputJson()) {
                print("%s", json(service, service.getPending()));
                print("1111111\n");
            } else {
                service.getPending().forEach(intent ->
                                                     print("id=%s, key=%s, type=%s, appId=%s",
                                                           intent.id(), intent.key(),
                                                           intent.getClass().getSimpleName(),
                                                           intent.appId().name())
                );
                print("22222222\n");
            }
            return;
        }

        if (outputJson()) {
            print("%s", json(service, service.getIntents()));
            print("333333333\n");
        } else {
            for (Intent intent : service.getIntents()) {
                IntentState state = service.getIntentState(intent.key());
                if (state != null) {
                    /*
                    print("id=%s, state=%s, key=%s, type=%s, appId=%s",
                          intent.id(), state, intent.key(),
                          intent.getClass().getSimpleName(),
                          intent.appId().name());
                    printDetails(service, intent);
                    */
                    String source=null,destination=null;
                    if (src.toString().contains("B8:27:EB")){
                        if ( dst.toString().contains("B8:AE:ED:79:CA:21")) {
                            HostToHostIntent pi = (HostToHostIntent) intent;
                            // log.info("Pi = {}",pi.one().toString());
                            //  log.info("src = {}",src.toString());

                            source =src.toString();
                            destination=dst.toString();
                            if (!(pi.one().toString().equals(source.toString()))){
                                if (!(pi.two().toString().equals(destination.toString()))){
                                    print("999999999999999999999999999");
                                    print("Pi src =%s", pi.one().toString());
                                    print("src = %s", source.toString());
                                    if (!(pi.one().toString().equals(destination.toString()))){
                                        if (!(pi.two().toString().equals(source.toString()))){
                                            print("ppppppppppppppppppppppppppppp");
                                            print("Pi dst =%s", pi.two().toString());
                                            print("dst = %s", destination.toString());
                                            HostId oneId = HostId.hostId(source.toString());
                                            HostId twoId = HostId.hostId(destination.toString());
/*
                                            HostToHostIntent fintent = HostToHostIntent.builder()
                                                    .appId(appId())
                                                    .key(key())
                                                    .one(oneId)
                                                    .two(twoId)
                                                    .selector(selector)
                                                    .treatment(treatment)
                                                    .constraints(constraints)
                                                    .priority(priority())
                                                    .build();

                                            intentService.submit(fintent);
                                            print("Host to Host intent submitted:\n%s", fintent.toString());
*/
                                        }
                                    }
                                }
                            }
                        }
                    }

                }

            }

            if(src.toString().contains("B8:27:EB")&& dst.toString().contains("B8:AE:ED:79:CA:21")) {
                if (ss) {
                    ss = false;
                    HostId oneId = HostId.hostId(src.toString());
                    HostId twoId = HostId.hostId(dst.toString());
                    HostToHostIntent fintent = HostToHostIntent.builder()
                            .appId(appId())
                            .key(key())
                            .one(oneId)
                            .two(twoId)
                            .selector(selector)
                            .treatment(treatment)
                            .constraints(constraints)
                            .priority(priority())
                            .build();

                   // intentService.submit(fintent);
                  //  print("Host to Host intent submitted:\n%s", fintent.toString());
                }
            }
        }

       // print(dst.toString());
       // print("##66666666666666666\n");

    }

    private class FwdTrafficPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

            // add the logic for receiving the packet-in message

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            DeviceId devId = pkt.receivedFrom().deviceId();
            src = HostId.hostId(ethPkt.getSourceMAC());
            dst = HostId.hostId(ethPkt.getDestinationMAC());

            HostPair pair = new HostPair(src, dst);
            if (map.containsKey(devId)) {
                Map<HostPair, Long> imap = map.get(devId);
                if (imap.containsKey(pair)) {
                    Long counter = imap.get(pair) + 1;
                    imap.put(pair, 1L);
                } else {
                    imap.put(pair, 1l);
                }

            } else {
                ConcurrentMap<HostPair, Long> tmp = new ConcurrentHashMap<HostPair, Long>();
                tmp.put(pair, 1l);
                map.put(devId, tmp);
            }
            //ss=false;
            //execute();
            flows.forEach((device, flow) -> printFlows(device, flow, coreService));
        }
    }

    // Indicates whether this is a control packet, e.g. LLDP, BDDP
    private boolean isControlPacket(Ethernet eth) {
        short type = eth.getEtherType();
        return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN;
    }
    /**
     * Request packet in via PacketService.
     */
    private void requestPackests() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE,
                                     appId);
        selector.matchEthType(Ethernet.TYPE_ARP);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE,
                                     appId);
    }

    @Override
    public ConcurrentMap<DeviceId, ConcurrentMap<HostPair, Long>> getMap() {
        return map;
    }

    public SortedMap<Device, List<FlowEntry>> getSortedFlows(DeviceService deviceService,
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
        //print("deviceId=%s, flowRuleCount=%d", d.id(), empty ? 0 : flows.size());
        if (!empty) {
            for (FlowEntry f : flows) {
                ApplicationId appId = coreService.getAppId(f.appId());
                if (f.selector().criteria().toString().contains("B8:27:EB") && f.appId()!=1){
                    packetSum= packetSum+f.packets();
                    packetBytes= packetBytes+f.bytes();

                }
            }
          //  if (packetSum> 100){
               // print("exceed flow / found other way ! ");
                execute();
          //  }


        }
    }
    /**
     * Internal local class to keep track of all intent summaries.
     */
    private class IntentSummaries {
        private IntentSummary summaryAll;
        private IntentSummary summaryConnectivity;
        private IntentSummary summaryHostToHost;
        private IntentSummary summaryPointToPoint;
        private IntentSummary summaryMultiPointToSinglePoint;
        private IntentSummary summarySinglePointToMultiPoint;
        private IntentSummary summaryPath;
        private IntentSummary summaryLinkCollection;
        private IntentSummary summaryUnknownType;

        /**
         * Initializes the internal state.
         */
        private void init() {
            summaryAll = new IntentSummary("All");
            summaryConnectivity = new IntentSummary("Connectivity");
            summaryHostToHost = new IntentSummary("HostToHost");
            summaryPointToPoint = new IntentSummary("PointToPoint");
            summaryMultiPointToSinglePoint =
                    new IntentSummary("MultiPointToSinglePoint");
            summarySinglePointToMultiPoint =
                    new IntentSummary("SinglePointToMultiPoint");
            summaryPath = new IntentSummary("Path");
            summaryLinkCollection = new IntentSummary("LinkCollection");
            summaryUnknownType = new IntentSummary("UnknownType");
        }

        /**
         * Collects summary of all intents.
         *
         * @param service the Intent Service to use
         * @param intents the intents
         */
        private void collectIntentSummary(IntentService service,
                                          Iterable<Intent> intents) {
            init();

            // Collect the summary for each intent type intents
            for (Intent intent : intents) {
                IntentState intentState = service.getIntentState(intent.key());
                if (intentState == null) {
                    continue;
                }

                // Update the summary for all Intents
                summaryAll.update(intentState);

                if (intent instanceof ConnectivityIntent) {
                    summaryConnectivity.update(intentState);
                    // NOTE: ConnectivityIntent is a base type Intent
                    // continue;
                }
                if (intent instanceof HostToHostIntent) {
                    summaryHostToHost.update(intentState);
                    continue;
                }
                if (intent instanceof PointToPointIntent) {
                    summaryPointToPoint.update(intentState);
                    continue;
                }
                if (intent instanceof MultiPointToSinglePointIntent) {
                    summaryMultiPointToSinglePoint.update(intentState);
                    continue;
                }
                if (intent instanceof SinglePointToMultiPointIntent) {
                    summarySinglePointToMultiPoint.update(intentState);
                    continue;
                }
                if (intent instanceof PathIntent) {
                    summaryPath.update(intentState);
                    continue;
                }
                if (intent instanceof LinkCollectionIntent) {
                    summaryLinkCollection.update(intentState);
                    continue;
                }

                summaryUnknownType.update(intentState);
            }
        }

        /**
         * Gets JSON representation of all Intents summary.
         *
         * @return JSON representation of all Intents summary
         */
        ObjectNode json() {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode result = mapper.createObjectNode();
            result.set("connectivity", summaryConnectivity.json(mapper));
            result.set("hostToHost", summaryHostToHost.json(mapper));
            result.set("pointToPoint", summaryPointToPoint.json(mapper));
            result.set("multiPointToSinglePoint",
                       summaryMultiPointToSinglePoint.json(mapper));
            result.set("singlePointToMultiPoint",
                       summarySinglePointToMultiPoint.json(mapper));
            result.set("path", summaryPath.json(mapper));
            result.set("linkCollection", summaryLinkCollection.json(mapper));
            result.set("unknownType", summaryUnknownType.json(mapper));
            result.set("all", summaryAll.json(mapper));
            return result;
        }

        /**
         * Prints summary of the intents.
         */
        private void printSummary() {
            summaryConnectivity.printState();
            summaryHostToHost.printState();
            summaryPointToPoint.printState();
            summaryMultiPointToSinglePoint.printState();
            summarySinglePointToMultiPoint.printState();
            summaryPath.printState();
            summaryLinkCollection.printState();
            summaryUnknownType.printState();
            summaryAll.printState();
        }

        /**
         * Internal local class to keep track of a single type Intent summary.
         */
        private class IntentSummary {
            private final String intentType;
            private int total = 0;
            private int installReq = 0;
            private int compiling = 0;
            private int installing = 0;
            private int installed = 0;
            private int recompiling = 0;
            private int withdrawReq = 0;
            private int withdrawing = 0;
            private int withdrawn = 0;
            private int failed = 0;
            private int unknownState = 0;

            private static final String FORMAT_SUMMARY_LINE1 =
                    "%-23s    total=        %7d   installed=   %7d";
            private static final String FORMAT_SUMMARY_LINE2 =
                    "%-23s    withdrawn=    %7d   failed=      %7d";
            private static final String FORMAT_SUMMARY_LINE3 =
                    "%-23s    installReq=   %7d   compiling=   %7d";
            private static final String FORMAT_SUMMARY_LINE4 =
                    "%-23s    installing=   %7d   recompiling= %7d";
            private static final String FORMAT_SUMMARY_LINE5 =
                    "%-23s    withdrawReq=  %7d   withdrawing= %7d";
            private static final String FORMAT_SUMMARY_LINE6 =
                    "%-23s    unknownState= %7d";

            /**
             * Constructor.
             *
             * @param intentType the scring describing the Intent type
             */
            IntentSummary(String intentType) {
                this.intentType = intentType;
            }

            /**
             * Updates the Intent Summary.
             *
             * @param intentState the state of the Intent
             */
            void update(IntentState intentState) {
                total++;
                switch (intentState) {
                    case INSTALL_REQ:
                        installReq++;
                        break;
                    case COMPILING:
                        compiling++;
                        break;
                    case INSTALLING:
                        installing++;
                        break;
                    case INSTALLED:
                        installed++;
                        break;
                    case RECOMPILING:
                        recompiling++;
                        break;
                    case WITHDRAW_REQ:
                        withdrawReq++;
                        break;
                    case WITHDRAWING:
                        withdrawing++;
                        break;
                    case WITHDRAWN:
                        withdrawn++;
                        break;
                    case FAILED:
                        failed++;
                        break;
                    default:
                        unknownState++;
                        break;
                }
            }

            /**
             * Prints the Intent Summary.
             */
            void printState() {
                print(FORMAT_SUMMARY_LINE1, intentType, total, installed);
                print(FORMAT_SUMMARY_LINE2, intentType, withdrawn, failed);
                print(FORMAT_SUMMARY_LINE3, intentType, installReq, compiling);
                print(FORMAT_SUMMARY_LINE4, intentType, installing, recompiling);
                print(FORMAT_SUMMARY_LINE5, intentType, withdrawReq, withdrawing);
                if (unknownState != 0) {
                    print(FORMAT_SUMMARY_LINE6, intentType, unknownState);
                }
            }

            /**
             * Gets the JSON representation of the Intent Summary.
             *
             * @return the JSON representation of the Intent Summary
             */
            JsonNode json(ObjectMapper mapper) {
                ObjectNode result = mapper.createObjectNode()
                        .put("total", total)
                        .put("installed", installed)
                        .put("failed", failed)
                        .put("installReq", installReq)
                        .put("compiling", compiling)
                        .put("installing", installing)
                        .put("recompiling", recompiling)
                        .put("withdrawReq", withdrawReq)
                        .put("withdrawing", withdrawing)
                        .put("withdrawn", withdrawn)
                        .put("unknownState", unknownState);

                return result;
            }
        }
    }
    private void printDetails(IntentService service, Intent intent) {
        if (!intent.resources().isEmpty()) {
            print("    resources=%s", intent.resources());
        }
        if (intent instanceof ConnectivityIntent) {
            ConnectivityIntent ci = (ConnectivityIntent) intent;
            if (!ci.selector().criteria().isEmpty()) {
                print("    selector=%s", ci.selector().criteria());
            }
            if (!ci.treatment().allInstructions().isEmpty()) {
                print("    treatment=%s", ci.treatment().allInstructions());
            }
            if (ci.constraints() != null && !ci.constraints().isEmpty()) {
                print("    constraints=%s", ci.constraints());
            }
        }

        if (intent instanceof HostToHostIntent) {
            HostToHostIntent pi = (HostToHostIntent) intent;
            print("    host1=%s, host2=%s", pi.one(), pi.two());
        } else if (intent instanceof PointToPointIntent) {
            PointToPointIntent pi = (PointToPointIntent) intent;
            print("    ingress=%s, egress=%s", pi.ingressPoint(), pi.egressPoint());
        } else if (intent instanceof MultiPointToSinglePointIntent) {
            MultiPointToSinglePointIntent pi = (MultiPointToSinglePointIntent) intent;
            print("    ingress=%s, egress=%s", pi.ingressPoints(), pi.egressPoint());
        } else if (intent instanceof SinglePointToMultiPointIntent) {
            SinglePointToMultiPointIntent pi = (SinglePointToMultiPointIntent) intent;
            print("    ingress=%s, egress=%s", pi.ingressPoint(), pi.egressPoints());
        } else if (intent instanceof PathIntent) {
            PathIntent pi = (PathIntent) intent;
            print("    path=%s, cost=%d", pi.path().links(), pi.path().cost());
        } else if (intent instanceof LinkCollectionIntent) {
            LinkCollectionIntent li = (LinkCollectionIntent) intent;
            print("    links=%s", li.links());
            print("    egress=%s", li.egressPoints());
        }

        List<Intent> installable = service.getInstallableIntents(intent.key());
        if (showInstallable && installable != null && !installable.isEmpty()) {
            print("    installable=%s", installable);
        }
    }

    // Produces JSON array of the specified intents.
    private JsonNode json(IntentService service, Iterable<Intent> intents) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        intents.forEach(intent -> result.add(jsonForEntity(intent, Intent.class)));
        return result;
    }


}
