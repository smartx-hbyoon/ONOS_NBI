package gist.ac.netcs.netcsmon;

import gist.ac.netcs.netcsmon.model.HostPair;
import org.onosproject.net.DeviceId;

import java.util.concurrent.ConcurrentMap;

/**
 * Created by netcsnuc on 2/19/16.
 */
public interface NetcsMonService {
    public ConcurrentMap<DeviceId,ConcurrentMap<HostPair,Long>> getMap();
}