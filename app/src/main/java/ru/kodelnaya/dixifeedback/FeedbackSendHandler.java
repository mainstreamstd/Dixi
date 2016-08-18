package ru.kodelnaya.dixifeedback;


import android.util.Log;

import java.io.IOException;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.util.EntityUtils;

public class FeedbackSendHandler implements ResponseHandler<Object> {
    @Override
    public Object handleResponse(HttpResponse response)
            throws ClientProtocolException, IOException {

        HttpEntity r_entity = response.getEntity();
        String responseString = EntityUtils.toString(r_entity);
        Log.d("UPLOAD", responseString);

        return null;
    }
}
