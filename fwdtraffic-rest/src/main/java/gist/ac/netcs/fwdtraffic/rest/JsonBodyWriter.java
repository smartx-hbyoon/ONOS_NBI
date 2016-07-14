package gist.ac.netcs.fwdtraffic.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Created by netcsnuc on 2/19/16.
 */

@Provider
@Produces("application/json")
public class JsonBodyWriter implements MessageBodyWriter<ObjectNode> {
    private ObjectMapper mapper = new ObjectMapper();


    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == ObjectNode.class;
    }

    @Override
    public long getSize(ObjectNode jsonNodes, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(ObjectNode node, Class<?> type, Type genericType,
                        Annotation[] annotations, MediaType mediaType, MultivaluedMap<String,
            Object> httpHeaders, OutputStream entityStream) throws IOException {
        mapper.writer().writeValue(entityStream, node);
        entityStream.flush();

    }
}
