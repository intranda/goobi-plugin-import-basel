package de.intranda.goobi.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.goobi.production.enums.ImportReturnValue;
import org.goobi.production.enums.ImportType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.importer.DocstructElement;
import org.goobi.production.importer.ImportObject;
import org.goobi.production.importer.Record;
import org.goobi.production.plugin.PluginLoader;
import org.goobi.production.plugin.interfaces.IImportPlugin;
import org.goobi.production.plugin.interfaces.IOpacPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.properties.ImportProperty;

import de.sub.goobi.forms.MassImportForm;
import de.sub.goobi.helper.exceptions.ImportPluginException;
import de.unigoettingen.sub.search.opac.ConfigOpac;
import de.unigoettingen.sub.search.opac.ConfigOpacCatalogue;
import lombok.extern.log4j.Log4j;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.Prefs;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.WriteException;

@Log4j
@PluginImplementation
public class DSV01CatalogueImportPlugin implements IImportPlugin, IPlugin {

    private static final String name = "DSV01";

    private Prefs prefs;
    private MassImportForm form = null;

    private IOpacPlugin opacPlugin;

    private String importFolder;
    List<ImportType> typeList = new ArrayList<>();

    private String identifier;
    private Fileformat fileformat = null;
    private String ats = null;

    public DSV01CatalogueImportPlugin() {
        typeList.add(ImportType.ID);
        typeList.add(ImportType.Record);

        opacPlugin = (IOpacPlugin) PluginLoader.getPluginByTitle(PluginType.Opac, "GBV-MARC");
    }

    @Override
    public PluginType getType() {
        return PluginType.Import;
    }

    @Override
    public String getTitle() {
        return name;
    }

    @Override
    public void setPrefs(Prefs prefs) {
        this.prefs = prefs;
    }

    @Override
    public Fileformat convertData() throws ImportPluginException {
        fileformat = null;
        ConfigOpacCatalogue coc = ConfigOpac.getInstance().getCatalogueByName("DSV01");

        try {
            fileformat = opacPlugin.search("12", identifier, coc, prefs);
            ats = opacPlugin.getAtstsl();
        } catch (Exception e) {
            throw new ImportPluginException(e);
        }
        return fileformat;
    }

    @Override
    public void setData(Record r) {
    }

    @Override
    public String getImportFolder() {
        return importFolder;
    }

    @Override
    public String getProcessTitle() {
        StringBuilder sb = new StringBuilder();
        sb.append(ats);
        String sorting = null;
        String identifier = null;
        String sourceCatalogue = null;
        if (fileformat != null) {
            try {
                DocStruct logical = fileformat.getDigitalDocument().getLogicalDocStruct();
                List<? extends Metadata> catalogueList = logical.getAllMetadataByType(prefs.getMetadataTypeByName("SourceCatalog"));
                if (catalogueList != null && !catalogueList.isEmpty()) {
                    sourceCatalogue = catalogueList.get(0).getValue();
                }
                List<? extends Metadata> sortingList = null;
                List<? extends Metadata> identifierList = null;
                switch (logical.getType().getName()) {
                    case "Periodical":
                        //        <processtitle isdoctype="periodical">TSL+'_'+Identifier b-Satz+'_'+Quellkatalog+'_'+Nummer (Benennung)</processtitle>

                        sortingList = logical.getAllChildren().get(0).getAllMetadataByType(prefs.getMetadataTypeByName("CurrentNoSorting"));
                        identifierList = logical.getAllMetadataByType(prefs.getMetadataTypeByName("CatalogIDDigital"));

                        if (sortingList != null && !sortingList.isEmpty()) {
                            sorting = sortingList.get(0).getValue();
                        }
                        if (identifierList != null && !identifierList.isEmpty()) {
                            identifier = identifierList.get(0).getValue();
                        }
                        break;
                    case "MultivolumeWork":
                        //        <processtitle isdoctype="multivolume">ATS+TSL+'_'+Identifier f-Satz+'_'+Quellkatalog+'_'+Nummer (Benennung)</processtitle>
                        sortingList = logical.getAllChildren().get(0).getAllMetadataByType(prefs.getMetadataTypeByName("CurrentNoSorting"));
                        identifierList = logical.getAllChildren().get(0).getAllMetadataByType(prefs.getMetadataTypeByName("CatalogIDDigital"));

                        if (sortingList != null && !sortingList.isEmpty()) {
                            sorting = sortingList.get(0).getValue();
                        }
                        if (identifierList != null && !identifierList.isEmpty()) {
                            identifier = identifierList.get(0).getValue();
                        }
                        break;
                    case "Manuscript":
                        //        <processtitle isdoctype="manuscript">ATS+TSL+'_'+Identifier+'_'+Quellkatalog</processtitle>
                        identifierList = logical.getAllMetadataByType(prefs.getMetadataTypeByName("CatalogIDDigital"));
                        if (identifierList != null && !identifierList.isEmpty()) {
                            identifier = identifierList.get(0).getValue();
                        }
                        break;

                    default:
                        // Monograph
                        //        <processtitle isdoctype="monograph">ATS+TSL+'_'+Identifier a-Satz+'_'+Quellkatalog</processtitle>

                        identifierList = logical.getAllMetadataByType(prefs.getMetadataTypeByName("CatalogIDDigital"));
                        if (identifierList != null && !identifierList.isEmpty()) {
                            identifier = identifierList.get(0).getValue();
                        }
                        break;
                }

            } catch (PreferencesException e) {
                log.error(e);
            }
        }
        if (identifier != null) {
            sb.append("_").append(identifier);
        }
        if (sourceCatalogue != null) {
            sb.append("_").append(sourceCatalogue);
        }
        if (sorting != null) {
            sb.append("_").append(sorting);
        }
        return sb.toString();
    }

    @Override
    public List<ImportObject> generateFiles(List<Record> records) {
        List<ImportObject> importedRecords = new LinkedList<>();
        for (Record record : records) {
            if (form != null) {
                form.addProcessToProgressBar();
            }
            ImportObject io = new ImportObject();
            importedRecords.add(io);
            log.info(record.getId());
            identifier = record.getId();
            if (StringUtils.isNotBlank(identifier)) {
                try {
                    fileformat = convertData();
                    if (fileformat != null) {
                        DocStruct anchor = null;
                        DocStruct logical = fileformat.getDigitalDocument().getLogicalDocStruct();
                        if (logical.getType().isAnchor()) {
                            anchor = logical;
                            logical = logical.getAllChildren().get(0);
                        }
                        if (!record.getCollections().isEmpty()) {
                            for (String collection : record.getCollections()) {
                                Metadata md = new Metadata(prefs.getMetadataTypeByName("singleDigCollection"));
                                md.setValue(collection);
                                logical.addMetadata(md);
                                if (anchor != null) {
                                    md = new Metadata(prefs.getMetadataTypeByName("singleDigCollection"));
                                    md.setValue(collection);
                                    anchor.addMetadata(md);
                                }
                            }
                        }
                        io.setProcessTitle(getProcessTitle());
                        fileformat.write(importFolder + record.getId() + ".xml");
                        io.setMetsFilename(importFolder + record.getId() + ".xml");
                        io.setImportReturnValue(ImportReturnValue.ExportFinished);

                    } else {
                        log.info("Can't find record for id " + identifier);
                        io.setErrorMessage(record.getId() + ": error during opac request.");
                        io.setImportReturnValue(ImportReturnValue.InvalidData);
                        io.setProcessTitle(identifier);
                    }

                } catch (ImportPluginException e) {
                    log.error(e);
                } catch (PreferencesException | MetadataTypeNotAllowedException e) {
                    log.error(e);
                } catch (WriteException e) {
                    log.error(e);
                }
            }
        }

        return importedRecords;
    }

    @Override
    public void setForm(MassImportForm form) {
        this.form = form;
    }

    @Override
    public void setImportFolder(String folder) {
        this.importFolder = folder;

    }

    @Override
    public List<Record> splitRecords(String records) {
        List<Record> recordList = new ArrayList<>();
        String[] data = { records };
        if (records.contains(" ")) {
            data = records.split(" ");
        }
        // windows, dos
        else if (records.contains("\r\n")) {
            data = records.split("\r\n");
        }
        // apple
        else if (records.contains("\r")) {
            data = records.split("\r");
        }
        // Linux, unix
        else if (records.contains("\n")) {
            data = records.split("\n");
        }
        if (data != null) {
            for (String id : data) {
                Record record = new Record();
                record.setData(id);
                record.setId(id);
                recordList.add(record);
            }
        }
        return recordList;
    }

    @Override
    public List<Record> generateRecordsFromFile() {
        return null;
    }

    @Override
    public List<Record> generateRecordsFromFilenames(List<String> filenames) {
        return null;
    }

    @Override
    public void setFile(File importFile) {
    }

    @Override
    public List<String> splitIds(String ids) {
        // white space
        if (ids.contains(" ")) {
            return Arrays.asList(ids.split(" "));
        }
        // windows, dos
        else if (ids.contains("\r\n")) {
            return Arrays.asList(ids.split("\r\n"));
        }
        // apple
        else if (ids.contains("\r")) {
            return Arrays.asList(ids.split("\r"));
        }
        // Linux, unix
        else if (ids.contains("\n")) {
            return Arrays.asList(ids.split("\n"));
        }
        List<String> single = new ArrayList<>();
        single.add(ids);
        return single;
    }

    @Override
    public List<ImportType> getImportTypes() {
        return typeList;
    }

    @Override
    public List<ImportProperty> getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getAllFilenames() {
        return null;
    }

    @Override
    public void deleteFiles(List<String> selectedFilenames) {
    }

    @Override
    public List<? extends DocstructElement> getCurrentDocStructs() {
        return null;
    }

    @Override
    public String deleteDocstruct() {
        return null;
    }

    @Override
    public String addDocstruct() {
        return null;
    }

    @Override
    public List<String> getPossibleDocstructs() {
        return null;
    }

    @Override
    public DocstructElement getDocstruct() {
        return null;
    }

    @Override
    public void setDocstruct(DocstructElement dse) {
    }

}
