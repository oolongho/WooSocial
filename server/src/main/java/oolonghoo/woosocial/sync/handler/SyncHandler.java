package com.oolonghoo.woosocial.sync.handler;

import com.oolonghoo.woosocial.sync.SyncMessage;

public interface SyncHandler {
    
    void initialize();
    
    void shutdown();
    
    void sendMessage(SyncMessage message);
    
    void broadcast(SyncMessage message);
    
    boolean isAvailable();
    
    String getName();
}
