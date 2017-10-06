package lsfusion.server.logics.service.process;

import lsfusion.server.context.ThreadType;
import lsfusion.server.data.StatusMessage;

import java.sql.Timestamp;

public class SQLProcess {
    public Timestamp dateTimeCall;
    public ThreadType threadType;
    public String query;
    public String fullQuery;
    public Long user;
    public Long computer;
    public String addressUser;
    public Timestamp dateTime;
    public Boolean isActive;
    public Boolean fusionInTransaction;
    public Boolean baseInTransaction;
    public Long startTransaction;
    public String attemptCount;
    public String status;
    public StatusMessage statusMessage;
    public String lockOwnerId;
    public String lockOwnerName;
    public Integer sqlId;
    public Boolean isDisabledNestLoop;
    public Integer queryTimeout;
    public String debugInfo;
    public String threadName;
    public String threadStackTrace;

    public SQLProcess(Timestamp dateTimeCall, ThreadType threadType, String query, String fullQuery, Long user, Long computer, String addressUser,
                      Timestamp dateTime, Boolean isActive, Boolean fusionInTransaction, Boolean baseInTransaction,
                      Long startTransaction, String attemptCount, String status, StatusMessage statusMessage, String lockOwnerId, String lockOwnerName,
                      Integer sqlId, Boolean isDisabledNestLoop, Integer queryTimeout, String debugInfo, String threadName, String threadStackTrace) {
        this.dateTimeCall = dateTimeCall;
        this.threadType = threadType;
        this.query = query;
        this.fullQuery = fullQuery;
        this.user = user;
        this.computer = computer;
        this.addressUser = addressUser;
        this.dateTime = dateTime;
        this.isActive = isActive;
        this.fusionInTransaction = fusionInTransaction;
        this.baseInTransaction = baseInTransaction;
        this.startTransaction = startTransaction;
        this.attemptCount = attemptCount;
        this.status = status;
        this.statusMessage = statusMessage;
        this.lockOwnerId = lockOwnerId;
        this.lockOwnerName = lockOwnerName;
        this.sqlId = sqlId;
        this.isDisabledNestLoop = isDisabledNestLoop;
        this.queryTimeout = queryTimeout;
        this.debugInfo = debugInfo;
        this.threadName = threadName;
        this.threadStackTrace = threadStackTrace;
    }
}