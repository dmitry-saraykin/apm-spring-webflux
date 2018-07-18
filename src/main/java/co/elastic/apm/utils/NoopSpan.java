package co.elastic.apm.utils;

import co.elastic.apm.api.Span;

public enum NoopSpan implements Span {
    INSTANCE;

    @Override
    public void setName(String name) {
        
    }

    @Override
    public void setType(String type) {
        
    }

    @Override
    public Span createSpan() {
        return this;
    }

    @Override
    public void end() {
        
    }

}
