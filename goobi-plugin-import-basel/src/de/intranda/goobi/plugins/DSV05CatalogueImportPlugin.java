package de.intranda.goobi.plugins;

import net.xeoh.plugins.base.annotations.PluginImplementation;


@PluginImplementation
public class DSV05CatalogueImportPlugin extends AbstractCatalogueImportPlugin {

    @Override
    public String getTitle() {
        return "DSV05";
    }
}
