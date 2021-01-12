package de.intranda.goobi.plugins;

import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
public class RZSCatalogueImportPlugin extends AbstractCatalogueImportPlugin {

    @Override
    public String getTitle() {
        return "RZS";
    }
}
