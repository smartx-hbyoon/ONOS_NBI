package gist.ac.netcs.fwdtraffic;

import gist.ac.netcs.fwdtraffic.model.HostPair;
import org.onosproject.net.DeviceId;

import java.util.concurrent.ConcurrentMap;

/**
 * Created by netcsnuc on 2/19/16.
 */
public interface FwdTrafficService {
    public ConcurrentMap<DeviceId,ConcurrentMap<HostPair,Long>> getMap();
}
