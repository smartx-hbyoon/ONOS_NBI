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
package gist.ac.netcs.fwdtraffic.gui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import gist.ac.netcs.fwdtraffic.FwdTrafficService;
import gist.ac.netcs.fwdtraffic.model.HostPair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.DeviceId;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiView;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Skeletal ONOS UI application component.
 */
@Component(immediate = true)
public class FwdTrafficUiComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected UiExtensionService uiExtensionService;

    protected FwdTrafficService fwdTrafficService;

    // List of application views
    private final List<UiView> uiViews = ImmutableList.of(
            new UiView(UiView.Category.OTHER, "fwdtraffic", "Packet_Traffic")
    );

    // Factory for UI message handlers
    private final UiMessageHandlerFactory messageHandlerFactory =
            () -> ImmutableList.of(
                    new AppUiMessageHandler()
            );

    // Application UI extension
    protected UiExtension extension = new UiExtension(uiViews, messageHandlerFactory,
                                                      getClass().getClassLoader());

    @Activate
    protected void activate() {
        uiExtensionService.register(extension);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        uiExtensionService.unregister(extension);
        log.info("Stopped");
    }

    // Application UI message handler
    private class AppUiMessageHandler extends UiMessageHandler {
        private static final String FWDTRAFFIC_DATA_REQ = "fwdtrafficDataRequest";
        private static final String FWDTRAFFIC_DATA_RESP = "fwdtrafficDataResponse";
        private static final String FWDTRAFFIC = "fwdtraffics";

        private static final String ID = "id";
        private static final String SRC = "src";
        private static final String DST = "dst";
        private static final String CNT = "cnt";
        private final String[] COL_IDS = { ID, SRC, DST, CNT };

        private ConcurrentMap<DeviceId, ConcurrentMap<HostPair, Long>> map;
        @Override
        protected Collection<RequestHandler> createRequestHandlers() {
            return ImmutableSet.of(new FwdTrafficDataRequest());
        }

        private final class FwdTrafficDataRequest extends TableRequestHandler {
            public FwdTrafficDataRequest() {
                super(FWDTRAFFIC_DATA_REQ,FWDTRAFFIC_DATA_RESP,FWDTRAFFIC);
            }

            @Override
            protected String[] getColumnIds() {
                return COL_IDS;
            }

            @Override
            protected void populateTable(TableModel tm, ObjectNode payload) {
                map = get(FwdTrafficService.class).getMap();
                for (DeviceId devId : map.keySet()){
                    map.get(devId).forEach((k,v)->populateRow(tm.addRow(),devId,k,v));
                }
                // iterate map and extract device id
                // for each device id need to further extract the host pair,
                // counter and add to TableModel...
                // hint: you can make use of pupulateRow method

            }
            private void populateRow(TableModel.Row row, DeviceId devId,
                                     HostPair hp, Long cnt) {
                // add each cell value, constructs a row value
                row.cell(ID,devId).cell(SRC,hp.getSrc()).cell(DST,hp.getDst()).cell(CNT,cnt);
            }
        }
    }

}
