package org.renci.canvas.primer.esp.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.Test;

public class Scratch {

    @Test
    public void scratch() throws Exception {

        File downloadDestination = new File("/tmp", "ESP6500SI-V2-SSA137.GRCh38-liftover.snps_indels.txt.tar.gz");
        try (FileInputStream fis = new FileInputStream(downloadDestination);
                GZIPInputStream gin = new GZIPInputStream(fis);
                TarArchiveInputStream tar = new TarArchiveInputStream(gin)) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextTarEntry()) != null) {

                try (BufferedReader br = new BufferedReader(new InputStreamReader(tar))) {

                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("#")) {
                            continue;
                        }
                        String[] lineSplit = line.split(" ");
                        String base = lineSplit[0];
                        String rsId = lineSplit[1];
                        String dbSNPVersion = lineSplit[2];
                        String alleles = lineSplit[3];
                        String europeanAmericanAlleleCount = lineSplit[4];
                        String africanAmericanAlleleCount = lineSplit[5];
                        String allAlleleCount = lineSplit[6];
                        String mAFinPercent = lineSplit[7];
                        String europeanAmericanGenotypeCount = lineSplit[8];
                        String africanAmericanGenotypeCount = lineSplit[9];
                        String allGenotypeCount = lineSplit[10];
                        String avgSampleReadDepth = lineSplit[11];
                        String genes = lineSplit[12];
                        String geneAccession = lineSplit[13];
                        String functionGVS = lineSplit[14];
                        String hgvsProteinVariant = lineSplit[15];
                        String hgvsCdnaVariant = lineSplit[16];
                        String codingDnaSize = lineSplit[17];
                        String conservationScorePhastCons = lineSplit[18];
                        String conservationScoreGERP = lineSplit[19];
                        String granthamScore = lineSplit[20];
                        String polyphen2 = lineSplit[21];
                        String refBaseNCBI37 = lineSplit[22];
                        String chimpAllele = lineSplit[23];
                        String clinicalInfo = lineSplit[24];
                        String filterStatus = lineSplit[25];
                        String onIlluminaHumanExomeChip = lineSplit[26];
                        String gwasPubMedInfo = lineSplit[27];
                        String eaEstimatedAge = lineSplit[28];
                        String aaEstimatedAge = lineSplit[29];
                        String gRCh38Position = lineSplit[30];

                    }

                }

                entry = tar.getNextTarEntry();
            }
        }
    }
}
