/*
 * Copyright 2016-present Open Networking Laboratory
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
package gist.ac.netcs.netcsmon;

import gist.ac.netcs.netcsmon.model.HostPair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
@Service
public class NetcsMonComponent implements NetcsMonService, traffic {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ConcurrentMap<DeviceId, ConcurrentMap<HostPair, Long>> map;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    private NetcsMonPacketProcessor processor = new NetcsMonPacketProcessor();

    private ApplicationId appId;

    private DeviceId dev;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("gist.ac.netcs.netcsmon");
        map = new ConcurrentHashMap<DeviceId, ConcurrentMap<HostPair, Long>>();
        packetService.addProcessor(processor, PacketProcessor.ADVISOR_MAX + 10);
        requestPackests();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;
        log.info("Stopped with Application ID {}", appId.id());
        log.info("Stopped");
    }

    @Override
    public long getTraffic() {
        return 0;
    }

    private class NetcsMonPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext packetContext) {
            InboundPacket pkt = packetContext.inPacket();
            Ethernet ethPkt = pkt.parsed();
            DeviceId devId = pkt.receivedFrom().deviceId();
            dev= devId;
            HostId src = HostId.hostId(ethPkt.getSourceMAC());
            HostId dst = HostId.hostId(ethPkt.getDestinationMAC());
            HostPair pair = new HostPair(src ,dst);
            if (map.containsKey(devId)){
                Map<HostPair, Long> imap = map.get(devId);
                if (imap.containsKey(pair)){
                    Long counter = imap.get(pair) +1;
                    imap.put(pair, counter);
                }
                else { imap.put(pair, 1L); }
            } else{
                ConcurrentMap<HostPair, Long> tmp = new ConcurrentHashMap<HostPair,Long>();
                tmp.put(pair, 1L);
                map.put(devId,tmp);
            }

        }
    }
    private boolean isControlPacket(Ethernet eth) {
        short type = eth.getEtherType();
        return type == Ethernet.TYPE_LLDP || type == Ethernet.TYPE_BSN;
    }
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
}
