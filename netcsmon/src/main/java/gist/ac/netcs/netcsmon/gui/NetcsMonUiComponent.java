package gist.ac.netcs.netcsmon.gui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import gist.ac.netcs.netcsmon.NetcsMonService;
import gist.ac.netcs.netcsmon.model.HostPair;
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

@Component(immediate = true)
public class NetcsMonUiComponent {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected UiExtensionService uiExtensionService;

    protected NetcsMonService netcsMonService;

    // List of application views
    private final List<UiView> uiViews = ImmutableList
            .of(new UiView(UiView.Category.OTHER, "netcsmon", "Packet_Traffic"));

    // Factory for UI message handlers
    private final UiMessageHandlerFactory messageHandlerFactory =
            () -> ImmutableList.of(
                    new AppUiMessageHandler()
            );

    // Application UI extension
    //protected UiExtension extension = new UiExtension(uiViews, messageHandlerFactory,
   //                                                 getClass().getClassLoader());
    protected UiExtension extension = new UiExtension.Builder(getClass().getClassLoader(), uiViews).messageHandlerFactory(messageHandlerFactory).build();

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

        private static final String FWDMON_DATA_REQ = "netcsmonDataRequest";
        private static final String FWDMON_DATA_RESP = "netcsmonDataResponse";
        private static final String FWDMON = "netcsmons";

        private static final String ID = "id";
        private static final String SRC = "src";
        private static final String DST = "dst";
        private static final String CNT = "cnt";
        private final String[] COL_IDS = { ID, SRC, DST, CNT };

        private ConcurrentMap<DeviceId, ConcurrentMap<HostPair, Long>> map;
        private final class NetcsMonDataRequest extends TableRequestHandler {

            public NetcsMonDataRequest() {
                super(FWDMON_DATA_REQ, FWDMON_DATA_RESP, FWDMON);
            }

            @Override
            protected String[] getColumnIds() {
                return COL_IDS;
            }

            @Override
            protected String noRowsMessage(ObjectNode objectNode) {
                return "asdfasfd";
            }

            @Override
            protected void populateTable(TableModel tm, ObjectNode payload) {
                map = get(NetcsMonService.class).getMap();
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
        @Override
        protected Collection<RequestHandler> createRequestHandlers() {
            return ImmutableSet.of(new NetcsMonDataRequest());
        }


    }
}