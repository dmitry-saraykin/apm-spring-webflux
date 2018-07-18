package co.elastic.apm.utils;

import co.elastic.apm.api.Span;
import co.elastic.apm.api.Transaction;

public enum NoopTransaction implements Transaction {
    INSTANCE;

    @Override
    public void setName(String name) {
        
    }

    @Override
    public void setType(String type) {
        
    }

    @Override
    public void addTag(String key, String value) {
        
    }

    @Override
    public void setUser(String id, String email, String username) {
        
    }

    @Override
    public void end() {
        
    }

    @Override
    public Span createSpan() {
        return NoopSpan.INSTANCE;
    }

}
