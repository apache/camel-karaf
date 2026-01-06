package org.apache.camel.blueprint;

public class CamelBlueprintConfigAdminPlaceholder {
    // tell Camel the path of the .cfg file to use for OSGi ConfigAdmin in the blueprint XML file
    private final String filename;
    //  tell Camel the persistence-id of the cm:property-placeholder in the blueprint XML file
    private final String persistenceId;

    public CamelBlueprintConfigAdminPlaceholder(String filename, String persistenceId) {
        this.filename = filename;
        this.persistenceId = persistenceId;
    }

    public String getFilename() {
        return filename;
    }

    public String getPersistenceId() {
        return persistenceId;
    }
}
