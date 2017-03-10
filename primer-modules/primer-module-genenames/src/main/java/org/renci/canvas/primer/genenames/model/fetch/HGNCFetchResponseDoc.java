package org.renci.canvas.primer.genenames.model.fetch;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HGNCFetchResponseDoc {

    @JsonProperty("hgnc_id")
    private String hgncId;

    private String symbol;

    private String name;

    private String status;

    @JsonProperty("locus_type")
    private String locusType;

    @JsonProperty("locus_group")
    private String locusGroup;

    @JsonProperty("prev_symbol")
    private List<String> previousSymbols;

    @JsonProperty("prev_name")
    private List<String> previousNames;

    @JsonProperty("alias_name")
    private List<String> aliasNames;

    @JsonProperty("alias_symbol")
    private List<String> aliasSymbols;

    private String location;

    @JsonProperty("date_approved_reserved")
    private String dateApprovedReserved;

    @JsonProperty("date_modified")
    private String dateModified;

    @JsonProperty("date_name_changed")
    private String dateNameChanged;

    @JsonProperty("date_symbol_changed")
    private String dateSymbolChanged;

    @JsonProperty("ena")
    private List<String> europeanNucleotideArchive;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("_version_")
    private Long version;

    @JsonProperty("entrez_id")
    private String entrezId;

    @JsonProperty("mgd_id")
    private List<String> mouseGenomeDatabaseId;

    @JsonProperty("pubmed_id")
    private List<Integer> pubmedId;

    @JsonProperty("refseq_accession")
    private List<String> refseqAccessions;

    @JsonProperty("gene_family")
    private List<String> geneFamily;

    @JsonProperty("gene_family_id")
    private List<Integer> geneFamilyId;

    @JsonProperty("location_sortable")
    private String locationSortable;

    @JsonProperty("vega_id")
    private String vegaId;

    @JsonProperty("ucsc_id")
    private String ucscId;

    @JsonProperty("omim_id")
    private List<Integer> omimId;

    @JsonProperty("ccds_id")
    private List<String> concensusCDSId;

    @JsonProperty("iuphar")
    private String iuphar;

    @JsonProperty("ensembl_gene_id")
    private String ensemblGeneId;

    @JsonProperty("uniprot_ids")
    private List<String> uniprotIds;

    @JsonProperty("rgd_id")
    private List<String> ratGenomeDatabaseId;

    @JsonProperty("lncrnadb")
    private String longNonCodingRNADatabaseId;

    @JsonProperty("cd")
    private String cellDifferentiation;

    @JsonProperty("bioparadigms_slc")
    private String bioParadigmsSLC;

    private String cosmic;

    @JsonProperty("enzyme_id")
    private List<String> enzymeIds;

    @JsonProperty("homeodb")
    private Integer homeoDatabaseId;

    @JsonProperty("horde_id")
    private String hordeId;

    @JsonProperty("imgt")
    private String immunoGenetics;

    @JsonProperty("intermediate_filament_db")
    private String intermediateFilamentDatabaseId;

    @JsonProperty("kznf_gene_catalog")
    private Integer kznfGeneCatalog;

    @JsonProperty("lsdb")
    private List<String> locusSpecificMutationDatabaseId;

    @JsonProperty("mamit-trnadb")
    private Integer mamittRNADatabaseId;

    @JsonProperty("merops")
    private String merops;

    @JsonProperty("mirbase")
    private String miRBaseId;

    @JsonProperty("orphanet")
    private Integer orphanetId;

    @JsonProperty("pseudogene.org")
    private String pseudogeneId;

    @JsonProperty("snornabase")
    private String snoRNABaseId;

    @JsonProperty("rna_central_ids")
    private List<String> rnaCentralIDs;

    public HGNCFetchResponseDoc() {
        super();
    }

    public List<String> getRnaCentralIDs() {
        return rnaCentralIDs;
    }

    public void setRnaCentralIDs(List<String> rnaCentralIDs) {
        this.rnaCentralIDs = rnaCentralIDs;
    }

    public String getHgncId() {
        return hgncId;
    }

    public void setHgncId(String hgncId) {
        this.hgncId = hgncId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLocusType() {
        return locusType;
    }

    public void setLocusType(String locusType) {
        this.locusType = locusType;
    }

    public String getLocusGroup() {
        return locusGroup;
    }

    public void setLocusGroup(String locusGroup) {
        this.locusGroup = locusGroup;
    }

    public List<String> getPreviousSymbols() {
        return previousSymbols;
    }

    public void setPreviousSymbols(List<String> previousSymbols) {
        this.previousSymbols = previousSymbols;
    }

    public List<String> getPreviousNames() {
        return previousNames;
    }

    public void setPreviousNames(List<String> previousNames) {
        this.previousNames = previousNames;
    }

    public List<String> getAliasNames() {
        return aliasNames;
    }

    public void setAliasNames(List<String> aliasNames) {
        this.aliasNames = aliasNames;
    }

    public List<String> getAliasSymbols() {
        return aliasSymbols;
    }

    public void setAliasSymbols(List<String> aliasSymbols) {
        this.aliasSymbols = aliasSymbols;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDateApprovedReserved() {
        return dateApprovedReserved;
    }

    public void setDateApprovedReserved(String dateApprovedReserved) {
        this.dateApprovedReserved = dateApprovedReserved;
    }

    public String getDateModified() {
        return dateModified;
    }

    public void setDateModified(String dateModified) {
        this.dateModified = dateModified;
    }

    public String getDateNameChanged() {
        return dateNameChanged;
    }

    public void setDateNameChanged(String dateNameChanged) {
        this.dateNameChanged = dateNameChanged;
    }

    public String getDateSymbolChanged() {
        return dateSymbolChanged;
    }

    public void setDateSymbolChanged(String dateSymbolChanged) {
        this.dateSymbolChanged = dateSymbolChanged;
    }

    public List<String> getEuropeanNucleotideArchive() {
        return europeanNucleotideArchive;
    }

    public void setEuropeanNucleotideArchive(List<String> europeanNucleotideArchive) {
        this.europeanNucleotideArchive = europeanNucleotideArchive;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getEntrezId() {
        return entrezId;
    }

    public void setEntrezId(String entrezId) {
        this.entrezId = entrezId;
    }

    public List<String> getMouseGenomeDatabaseId() {
        return mouseGenomeDatabaseId;
    }

    public void setMouseGenomeDatabaseId(List<String> mouseGenomeDatabaseId) {
        this.mouseGenomeDatabaseId = mouseGenomeDatabaseId;
    }

    public List<Integer> getPubmedId() {
        return pubmedId;
    }

    public void setPubmedId(List<Integer> pubmedId) {
        this.pubmedId = pubmedId;
    }

    public List<String> getRefseqAccessions() {
        return refseqAccessions;
    }

    public void setRefseqAccessions(List<String> refseqAccessions) {
        this.refseqAccessions = refseqAccessions;
    }

    public List<String> getGeneFamily() {
        return geneFamily;
    }

    public void setGeneFamily(List<String> geneFamily) {
        this.geneFamily = geneFamily;
    }

    public List<Integer> getGeneFamilyId() {
        return geneFamilyId;
    }

    public void setGeneFamilyId(List<Integer> geneFamilyId) {
        this.geneFamilyId = geneFamilyId;
    }

    public String getLocationSortable() {
        return locationSortable;
    }

    public void setLocationSortable(String locationSortable) {
        this.locationSortable = locationSortable;
    }

    public String getVegaId() {
        return vegaId;
    }

    public void setVegaId(String vegaId) {
        this.vegaId = vegaId;
    }

    public String getUcscId() {
        return ucscId;
    }

    public void setUcscId(String ucscId) {
        this.ucscId = ucscId;
    }

    public List<Integer> getOmimId() {
        return omimId;
    }

    public void setOmimId(List<Integer> omimId) {
        this.omimId = omimId;
    }

    public List<String> getConcensusCDSId() {
        return concensusCDSId;
    }

    public void setConcensusCDSId(List<String> concensusCDSId) {
        this.concensusCDSId = concensusCDSId;
    }

    public String getIuphar() {
        return iuphar;
    }

    public void setIuphar(String iuphar) {
        this.iuphar = iuphar;
    }

    public String getEnsemblGeneId() {
        return ensemblGeneId;
    }

    public void setEnsemblGeneId(String ensemblGeneId) {
        this.ensemblGeneId = ensemblGeneId;
    }

    public List<String> getUniprotIds() {
        return uniprotIds;
    }

    public void setUniprotIds(List<String> uniprotIds) {
        this.uniprotIds = uniprotIds;
    }

    public List<String> getRatGenomeDatabaseId() {
        return ratGenomeDatabaseId;
    }

    public void setRatGenomeDatabaseId(List<String> ratGenomeDatabaseId) {
        this.ratGenomeDatabaseId = ratGenomeDatabaseId;
    }

    public String getLongNonCodingRNADatabaseId() {
        return longNonCodingRNADatabaseId;
    }

    public void setLongNonCodingRNADatabaseId(String longNonCodingRNADatabaseId) {
        this.longNonCodingRNADatabaseId = longNonCodingRNADatabaseId;
    }

    public String getCellDifferentiation() {
        return cellDifferentiation;
    }

    public void setCellDifferentiation(String cellDifferentiation) {
        this.cellDifferentiation = cellDifferentiation;
    }

    public String getBioParadigmsSLC() {
        return bioParadigmsSLC;
    }

    public void setBioParadigmsSLC(String bioParadigmsSLC) {
        this.bioParadigmsSLC = bioParadigmsSLC;
    }

    public String getCosmic() {
        return cosmic;
    }

    public void setCosmic(String cosmic) {
        this.cosmic = cosmic;
    }

    public List<String> getEnzymeIds() {
        return enzymeIds;
    }

    public void setEnzymeIds(List<String> enzymeIds) {
        this.enzymeIds = enzymeIds;
    }

    public Integer getHomeoDatabaseId() {
        return homeoDatabaseId;
    }

    public void setHomeoDatabaseId(Integer homeoDatabaseId) {
        this.homeoDatabaseId = homeoDatabaseId;
    }

    public String getHordeId() {
        return hordeId;
    }

    public void setHordeId(String hordeId) {
        this.hordeId = hordeId;
    }

    public String getImmunoGenetics() {
        return immunoGenetics;
    }

    public void setImmunoGenetics(String immunoGenetics) {
        this.immunoGenetics = immunoGenetics;
    }

    public String getIntermediateFilamentDatabaseId() {
        return intermediateFilamentDatabaseId;
    }

    public void setIntermediateFilamentDatabaseId(String intermediateFilamentDatabaseId) {
        this.intermediateFilamentDatabaseId = intermediateFilamentDatabaseId;
    }

    public Integer getKznfGeneCatalog() {
        return kznfGeneCatalog;
    }

    public void setKznfGeneCatalog(Integer kznfGeneCatalog) {
        this.kznfGeneCatalog = kznfGeneCatalog;
    }

    public List<String> getLocusSpecificMutationDatabaseId() {
        return locusSpecificMutationDatabaseId;
    }

    public void setLocusSpecificMutationDatabaseId(List<String> locusSpecificMutationDatabaseId) {
        this.locusSpecificMutationDatabaseId = locusSpecificMutationDatabaseId;
    }

    public Integer getMamittRNADatabaseId() {
        return mamittRNADatabaseId;
    }

    public void setMamittRNADatabaseId(Integer mamittRNADatabaseId) {
        this.mamittRNADatabaseId = mamittRNADatabaseId;
    }

    public String getMerops() {
        return merops;
    }

    public void setMerops(String merops) {
        this.merops = merops;
    }

    public String getMiRBaseId() {
        return miRBaseId;
    }

    public void setMiRBaseId(String miRBaseId) {
        this.miRBaseId = miRBaseId;
    }

    public Integer getOrphanetId() {
        return orphanetId;
    }

    public void setOrphanetId(Integer orphanetId) {
        this.orphanetId = orphanetId;
    }

    public String getPseudogeneId() {
        return pseudogeneId;
    }

    public void setPseudogeneId(String pseudogeneId) {
        this.pseudogeneId = pseudogeneId;
    }

    public String getSnoRNABaseId() {
        return snoRNABaseId;
    }

    public void setSnoRNABaseId(String snoRNABaseId) {
        this.snoRNABaseId = snoRNABaseId;
    }

}
