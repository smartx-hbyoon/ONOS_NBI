package gist.ac.netcs.netcsmon.gui;

import com.google.common.collect.ImmutableList;
import gist.ac.netcs.netcsmon.NetcsMonService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.UiMessageHandlerFactory;
import org.onosproject.ui.UiView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * Created by netcsnuc on 7/25/16.
 */

@Component(immediate = true)
public class NetcsTopoUiComponent {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected UiExtensionService uiExtensionService;

    protected NetcsMonService netcsMonService;

    // List of application views
    private final List<UiView> uiViews = ImmutableList
            .of(new UiView(UiView.Category.OTHER, "netcstopo", "Topo Example"));

    // Factory for UI message handlers
    private final UiMessageHandlerFactory messageHandlerFactory =
            () -> ImmutableList.of(
                    new AppUiMessageHandler()
            );
    protected UiExtension extension = new UiExtension.Builder(getClass().getClassLoader(), uiViews).messageHandlerFactory(messageHandlerFactory).build();

    @Activate
    protected void activate() {
       // uiExtensionService.register(extension);
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
        @Override
        protected Collection<RequestHandler> createRequestHandlers() {
            return null;
        }
    }
}
