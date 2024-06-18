package org.apache.karaf.camel.itests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ExternalResourceWithPrerequisite implements ExternalResource {

    private final Map<String, String> properties = new HashMap<>();

    @Override
    public void before() {
        for (ExternalResource prerequisite : getPrerequisites()) {
            prerequisite.before();
            prerequisite.properties().forEach(this::setProperty);
        }
        doStart();
    }

    @Override
    public void after() {
        doStop();
        for (ExternalResource prerequisite : getPrerequisites()) {
            prerequisite.after();
        }
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    @Override
    public Map<String, String> properties() {
        return properties;
    }

    protected abstract List<ExternalResource> getPrerequisites();

    protected abstract void doStart();

    protected abstract void doStop();

}
