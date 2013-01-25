package org.jokerd.opensocial.facebook.calls;

import java.util.HashMap;
import java.util.Map;

import org.jokerd.opensocial.oauth.OAuthHelper;
import org.ubimix.commons.json.JsonObject;
import org.ubimix.commons.json.JsonValue;
import org.ubimix.commons.json.rpc.RpcCallHandler;
import org.ubimix.commons.json.rpc.RpcError;
import org.ubimix.commons.json.rpc.RpcRequest;
import org.ubimix.commons.json.rpc.RpcResponse;

/**
 * @author kotelnikov
 */
public class FacebookCallHandler extends RpcCallHandler {

    private final OAuthHelper fHelper;

    public FacebookCallHandler(OAuthHelper helper) {
        fHelper = helper;
    }

    @Override
    protected RpcResponse doHandle(RpcRequest request) throws Exception {
        RpcResponse response = new RpcResponse();
        String url = request.getMethod();
        Map<String, String> params = new HashMap<String, String>();
        JsonObject paramsObj = request.getParamsAsObject();
        for (String key : paramsObj.getKeys()) {
            String value = paramsObj.getObject(key, JsonValue.STRING_FACTORY);
            params.put(key, value);
        }
        String resultStr = fHelper.call(url, params.entrySet());
        JsonObject result = JsonObject.FACTORY.newValue(resultStr);
        RpcError error = result.getObject("error", RpcError.FACTORY);
        if (error != null) {
            response.setError(error);
        } else {
            response.setResult(result);
        }
        return response;
    }
}