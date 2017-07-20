package lsfusion.client.form;

import lsfusion.base.Pair;
import lsfusion.client.ClientLoggers;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;

public abstract class RmiRequest<T> {
    private static final Logger logger = ClientLoggers.invocationLogger;

    private long requestIndex = -1;
    private long lastReceivedRequestIndex = -1;
    private String name;
    
    /**
     * first - power base;
     * second - offset coefficient
     */
    private Pair<Integer, Integer> timeoutParams;

    protected RmiRequest(String name) {
        this.name = name;
    }

    protected RmiRequest(String name, Pair<Integer, Integer> timeoutParams) {
        this.name = name;
        this.timeoutParams = timeoutParams;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * first - power base;
     * second - offset coefficient
     */
    public void setTimeoutParams(Pair<Integer, Integer> timeoutParams) {
        this.timeoutParams = timeoutParams;
    }

    public Pair<Integer, Integer> getTimeoutParams() {
        return timeoutParams;
    }

    void setRequestIndex(long rmiRequestIndex) {
        this.requestIndex = rmiRequestIndex;
    }

    public long getRequestIndex() {
        return requestIndex;
    }

    void setLastReceivedRequestIndex(long lastReceivedRequestIndex) {
        this.lastReceivedRequestIndex = lastReceivedRequestIndex;
    }

    final T doRequest() throws RemoteException {
        if (logger.isDebugEnabled()) {
            logger.debug("DoRequest: " + this);
        }
        return doRequest(requestIndex, lastReceivedRequestIndex);
    }

    final void onAsyncRequest() {
        if (logger.isDebugEnabled()) {
            logger.debug("OnAsyncRequest: " + this);
        }
        onAsyncRequest(requestIndex);
    }

    final void onResponse(T result) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("OnResponse: " + this);
        }
        onResponse(requestIndex, result);
    }

    final void onResponseGetFailed(Exception e) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("OnResponseGetFailed: " + this);
        }
        onResponseGetFailed(requestIndex, e);
    }

    protected void onAsyncRequest(long requestIndex) {
    }

    protected abstract T doRequest(long requestIndex, long lastReceivedRequestIndex) throws RemoteException;

    protected void onResponseGetFailed(long requestIndex, Exception e) throws Exception {
    }

    protected void onResponse(long requestIndex, T result) throws Exception {
    }

    @Override
    public String toString() {
        return "RR[" + name + " : " + requestIndex + ", last received : " + lastReceivedRequestIndex + "]";
    }
}