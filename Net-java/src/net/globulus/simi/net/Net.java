package net.globulus.simi.net;

import net.globulus.simi.api.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedHashMap;

@SimiJavaConfig(apiClassName = "Net_java")
@SimiJavaClass
public class Net {

    @SimiJavaMethod
    public static SimiProperty get(SimiObject self, BlockInterpreter interpreter, SimiProperty request, SimiProperty callback) {
        new Thread(() -> {
            SimiObject object = request.getValue().getObject();
            SimiEnvironment env = interpreter.getEnvironment();
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet get = new HttpGet(object.get("url", env).getValue().getString());
            SimiObject headers = object.get("headers", env).getValue().getObject();
            if (headers != null) {
                for (SimiValue header : headers.keys()) {
                    get.setHeader(header.getString(), headers.get(header.getString(), env).getValue().getString());
                }
            }
            try {
                CloseableHttpResponse response = client.execute(get);
                LinkedHashMap<String, SimiProperty> props = new LinkedHashMap<>();
                props.put("code", new SimiValue.Number(response.getStatusLine().getStatusCode()));
                props.put("body", new SimiValue.String(EntityUtils.toString(response.getEntity())));
                client.close();
                SimiValue simiResponse = new SimiValue.Object(interpreter.newObject(true, props));
                callback.getValue().getCallable().call(interpreter, Collections.singletonList(simiResponse), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        return null;
    }

    @SimiJavaMethod
    public static SimiProperty post(SimiObject self, BlockInterpreter interpreter, SimiProperty request, SimiProperty callback) {
        new Thread(() -> {
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
        }).start();
        return null;
    }

    @SimiJavaMethod
    public static SimiProperty put(SimiObject self, BlockInterpreter interpreter, SimiProperty request, SimiProperty callback) {
        new Thread(() -> {
            SimiObject object = request.getValue().getObject();
            SimiEnvironment env = interpreter.getEnvironment();
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPut put = new HttpPut(object.get("url", env).getValue().getString());
            try {
                StringEntity entity = new StringEntity(object.get("json", env).getValue().getString());
                put.setEntity(entity);
                SimiObject headers = object.get("headers", env).getValue().getObject();
                if (headers != null) {
                    for (SimiValue header : headers.keys()) {
                        put.setHeader(header.getString(), headers.get(header.getString(), env).getValue().getString());
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                CloseableHttpResponse response = client.execute(put);
                LinkedHashMap<String, SimiProperty> props = new LinkedHashMap<>();
                props.put("code", new SimiValue.Number(response.getStatusLine().getStatusCode()));
                props.put("body", new SimiValue.String(EntityUtils.toString(response.getEntity())));
                client.close();
                SimiValue simiResponse = new SimiValue.Object(interpreter.newObject(true, props));
                callback.getValue().getCallable().call(interpreter, Collections.singletonList(simiResponse), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        return null;
    }

    @SimiJavaMethod
    public static SimiProperty delete(SimiObject self, BlockInterpreter interpreter, SimiProperty request, SimiProperty callback) {
        new Thread(() -> {
            SimiObject object = request.getValue().getObject();
            SimiEnvironment env = interpreter.getEnvironment();
            CloseableHttpClient client = HttpClients.createDefault();
            HttpDelete delete = new HttpDelete(object.get("url", env).getValue().getString());
            SimiObject headers = object.get("headers", env).getValue().getObject();
            if (headers != null) {
                for (SimiValue header : headers.keys()) {
                    delete.setHeader(header.getString(), headers.get(header.getString(), env).getValue().getString());
                }
            }
            try {
                CloseableHttpResponse response = client.execute(delete);
                LinkedHashMap<String, SimiProperty> props = new LinkedHashMap<>();
                props.put("code", new SimiValue.Number(response.getStatusLine().getStatusCode()));
                props.put("body", new SimiValue.String(EntityUtils.toString(response.getEntity())));
                client.close();
                SimiValue simiResponse = new SimiValue.Object(interpreter.newObject(true, props));
                callback.getValue().getCallable().call(interpreter, Collections.singletonList(simiResponse), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        return null;
    }
}
