package org.renci.canvas.primer.clinvar.commands;

import java.util.List;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.renci.canvas.dao.commons.LocatedVariantFactory;
import org.renci.canvas.dao.ref.model.GenomeRef;
import org.renci.canvas.dao.ref.model.GenomeRefSeq;
import org.renci.canvas.dao.var.model.LocatedVariant;
import org.renci.canvas.dao.var.model.VariantType;
import org.renci.clinvar.SequenceLocationType;
import org.renci.gerese4j.core.GeReSe4jBuild;
import org.renci.gerese4j.core.GeReSe4jException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocatedVariantUtil {

    private static final Logger logger = LoggerFactory.getLogger(LocatedVariantUtil.class);

    public static LocatedVariant processMutation(String measureType, SequenceLocationType sequenceLocationType, GeReSe4jBuild gerese4jBuild,
            GenomeRef genomeRef, GenomeRefSeq genomeRefSeq, List<VariantType> allVariantTypes) {
        String refBase = null;

        String alt = StringUtils.isNotEmpty(sequenceLocationType.getAlternateAllele())
                && !sequenceLocationType.getAlternateAllele().equals("-") ? sequenceLocationType.getAlternateAllele() : "";

        try {
            switch (measureType) {
                case "Deletion":
                case "Insertion":
                case "Duplication":
                    if (sequenceLocationType.getStart().intValue() == sequenceLocationType.getStop().intValue()) {
                        refBase = gerese4jBuild.getBase(sequenceLocationType.getAccession(), sequenceLocationType.getStart().intValue(),
                                true);
                    } else {
                        refBase = gerese4jBuild.getRegion(sequenceLocationType.getAccession(),
                                Range.between(sequenceLocationType.getStart().intValue(), sequenceLocationType.getStop().intValue() + 1),
                                true);
                    }

                    break;
                case "single nucleotide variant":
                    refBase = gerese4jBuild.getBase(sequenceLocationType.getAccession(), sequenceLocationType.getStart().intValue(), true);
                    break;
            }
        } catch (GeReSe4jException e) {
            logger.warn(e.getMessage());
            return null;
        }

        if (refBase.equals(alt)) {
            return null;
        }

        LocatedVariant locatedVariant = LocatedVariantFactory.create(genomeRef, genomeRefSeq, sequenceLocationType.getStart().intValue(),
                refBase, alt, allVariantTypes);

        return locatedVariant;
    }

}
