package org.renci.canvas.primer.genenames.commands;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.renci.canvas.primer.genenames.model.fetch.HGNCFetch;
import org.renci.canvas.primer.genenames.model.search.HGNCSearch;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Scratch {

    @Test
    public void scratch() throws Exception {
        Date d = DateUtils.parseDate("2015-12-14T00:00:00Z", "yyyy-MM-dd'T'HH:mm:ss'Z'");
        System.out.println(d.toString());

        String location = "19q13.43";
        Pattern p = Pattern.compile("(?<chr>\\d+)[a-z]\\.?\\d+?");
        Matcher m = p.matcher(location);
        if (m.find()) {
            String chr = m.group("chr");
            System.out.println(chr);
        }
    }

    @Test
    public void fetch() throws Exception {
        Client client = ClientBuilder.newBuilder().newClient();
        // WebTarget target = client.target("http://rest.genenames.org/fetch/symbol/ASIC1");
        WebTarget target = client.target("http://rest.genenames.org/fetch/symbol/ADAM12-OT1");
        Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON);
        Response response = builder.get();
        String result = response.readEntity(String.class);
        FileUtils.write(new File("/tmp", "asdf"), result, "UTF-8");
    }

    @Test
    public void search() throws Exception {
        Client client = ClientBuilder.newBuilder().newClient();
        WebTarget target = client.target("http://rest.genenames.org/search/status:Approved+OR+status:%22Entry%20Withdrawn%22");
        Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON);
        Response response = builder.get();
        String result = response.readEntity(String.class);
        FileUtils.write(new File("/tmp", "qwer"), result, "UTF-8");
    }

    @Test
    public void parseFetch() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HGNCFetch hgnc = mapper.readValue(new File("/tmp", "asdf"), HGNCFetch.class);
        System.out.println(hgnc.getResponseHeader().toString());
        System.out.println(hgnc.getResponse().toString());
        System.out.println(hgnc.getResponse().getDocs().get(0).getAliasSymbols().get(0));
        System.out.println(hgnc.getResponse().getDocs().get(0).getVersion());
    }

    @Test
    public void parseSearch() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HGNCSearch hgnc = mapper.readValue(new File("/tmp", "qwer"), HGNCSearch.class);
        System.out.println(hgnc.getResponseHeader().toString());
        System.out.println(hgnc.getResponse().toString());
    }

    @Test
    public void testSymbols() throws Exception {

        Reader in = new FileReader(new File("/home/jdr0887/Downloads/hgnc_complete_set.txt"));
        Iterable<CSVRecord> records = CSVFormat.TDF.withFirstRecordAsHeader().parse(in);
        for (CSVRecord record : records) {
            
            String hgncId = record.get("hgnc_id");
            String symbol = record.get("symbol");
            String name = record.get("name");
            String locusGroup = record.get("locus_group");
            String locusType = record.get("locus_type");
            String status = record.get("status");
            String location = record.get("location");
            String locationSortable = record.get("location_sortable");
            String aliasSymbol = record.get("alias_symbol");
            String aliasName = record.get("alias_name");
            String prevSymbol = record.get("prev_symbol");
            String prevName = record.get("prev_name");
            String geneFamily = record.get("gene_family");
            String geneFamilyId = record.get("gene_family_id");
            String dateApprovedReserved = record.get("date_approved_reserved");
            String dateSymbolChanged = record.get("date_symbol_changed");
            String dateNameChanged = record.get("date_name_changed");
            String dateModified = record.get("date_modified");
            System.out.println(prevSymbol);
            System.out.println(aliasSymbol);
        }

    }

}
