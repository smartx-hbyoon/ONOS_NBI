package gist.ac.netcs.fwdtraffic.model;

import org.onosproject.net.HostId;

/**
 * Created by netcsnuc on 2/19/16.
 */
public class HostPair {
    private HostId src;
    private HostId dst;


    public HostPair(HostId src, HostId dst) {
        this.src = src;
        this.dst = dst;
    }

    public HostId getSrc() {
        return src;
    }

    public HostId getDst() {
        return dst;
    }



    public void setSrc(HostId src) {
        this.src = src;
    }

    public void setDst(HostId dst) {
        this.dst = dst;
    }


    @Override
    public boolean equals(Object o) {
        HostPair hp = (HostPair) o;
        return hp.getSrc().mac().equals(src.mac())
                && hp.getDst().mac().equals(dst.mac());
    }

    @Override
    public int hashCode() {
        String ttl = src.mac().toString() + "_" + dst.mac().toString();
        return ttl.hashCode();
    }
}
