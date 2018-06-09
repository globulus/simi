package net.globulus.simi.net;

import net.globulus.simi.api.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedHashMap;

@SimiJavaClass
public class Net {

    @SimiJavaMethod
    public static SimiProperty post(SimiObject self, BlockInterpreter interpreter, SimiProperty request, SimiProperty callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SimiObject object = request.getValue().getObject();
                SimiEnvironment env = interpreter.getEnvironment();
                CloseableHttpClient client = HttpClients.createDefault();
                HttpPost post = new HttpPost(object.get("url", env).getValue().getString());
                try {
                    StringEntity entity = new StringEntity(object.get("json", env).getValue().getString());
                    post.setEntity(entity);
                    SimiObject headers = object.get("headers", env).getValue().getObject();
                    if (headers != null) {
                        for (SimiValue header : headers.keys()) {
                            post.setHeader(header.getString(), headers.get(header.getString(), env).getValue().getString());
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                try {
                    CloseableHttpResponse response = client.execute(post);
                    LinkedHashMap<String, SimiProperty> props = new LinkedHashMap<>();
                    props.put("code", new SimiValue.Number(response.getStatusLine().getStatusCode()));
                    props.put("body", new SimiValue.String(EntityUtils.toString(response.getEntity())));
                    client.close();
                    SimiValue simiResponse = new SimiValue.Object(interpreter.newObject(true, props));
                    callback.getValue().getCallable().call(interpreter, Collections.singletonList(simiResponse), false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return null;
    }
}
