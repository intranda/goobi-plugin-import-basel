package de.intranda.goobi.plugins;

import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
public class UBECatalogueImportPlugin extends AbstractCatalogueImportPlugin {

    @Override
    public String getTitle() {
        return "ZBS";
    }
}
