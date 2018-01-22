package uk.ac.ebi.interpro.scan.persistence;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.interpro.scan.model.Protein;
import uk.ac.ebi.interpro.scan.model.Signature;
import uk.ac.ebi.interpro.scan.model.SuperFamilyHmmer3Match;
import uk.ac.ebi.interpro.scan.model.raw.RawProtein;
import uk.ac.ebi.interpro.scan.model.raw.SuperFamilyHmmer3RawMatch;
import uk.ac.ebi.interpro.scan.util.Utilities;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SuperFamily filtered match data access object.
 *
 * @author Matthew Fraser
 * @version $Id$
 */
public class SuperFamilyHmmer3FilteredMatchDAOImpl extends FilteredMatchDAOImpl<SuperFamilyHmmer3RawMatch, SuperFamilyHmmer3Match> implements SuperFamilyHmmer3FilteredMatchDAO {

    private static final Logger LOGGER = Logger.getLogger(SuperFamilyHmmer3FilteredMatchDAOImpl.class.getName());

    private String superFamilyReleaseVersion;

    /**
     * Sets the class of the model that the DAO instance handles.
     * Note that this has been set up to use constructor injection
     * because it makes it easy to sub-class GenericDAOImpl in a robust
     * manner.
     * <p/>
     * Model class specific sub-classes should define a no-argument constructor
     * that calls this constructor with the appropriate class.
     */
    public SuperFamilyHmmer3FilteredMatchDAOImpl(String version) {
        super(SuperFamilyHmmer3Match.class);
        this.superFamilyReleaseVersion = version;
    }

    /**
     * This is the method that should be implemented by specific FilteredMatchDAOImpl's to
     * persist filtered matches.
     *
     * @param filteredProteins      being the Collection of filtered RawProtein objects to persist
     * @param modelIdToSignatureMap a Map of model IDs to Signature objects.
     * @param proteinIdToProteinMap a Map of Protein IDs to Protein objects
     */
    @Transactional
    public void persist(Collection<RawProtein<SuperFamilyHmmer3RawMatch>> filteredProteins, Map<String, Signature> modelIdToSignatureMap, Map<String, Protein> proteinIdToProteinMap) {
        int proteinCount = 0;
        int matchCount = 0;
        int sfBatchSize = 3000;
        Utilities.verboseLog("SuperFamilyHmmer3FilteredMatchDAO: Start persist " + filteredProteins.size() + " filteredProteins,");
        for (RawProtein<SuperFamilyHmmer3RawMatch> rawProtein : filteredProteins) {
            proteinCount++;
            final Map<UUID, SuperFamilyHmmer3Match> splitGroupToMatch = new HashMap<UUID, SuperFamilyHmmer3Match>();

            Protein protein = proteinIdToProteinMap.get(rawProtein.getProteinIdentifier());
            if (protein == null) {
                throw new IllegalStateException("Cannot store match to a protein that is not in database " +
                        "[protein ID= " + rawProtein.getProteinIdentifier() + "]");
            }
            LOGGER.debug("Protein: " + protein);
//            if ((proteinCount == 1) || (proteinCount % sfBatchSize == 0)){
//               	Utilities.verboseLog("SuperFamilyHmmer3FilteredMatchDAO: considering Protein: " + protein.getId());
//            }
            for (SuperFamilyHmmer3RawMatch rawMatch : rawProtein.getMatches()) {
                SuperFamilyHmmer3Match match = splitGroupToMatch.get(rawMatch.getSplitGroup());
//                if ((proteinCount == 1) || (proteinCount % sfBatchSize == 0)) {
//                    if (match != null) {
//                        Utilities.verboseLog("SuperFamilyHmmer3FilteredMatchDAO: considering Protein: " + match);
//                    }
//                }

                if (match == null) {
                    final Signature currentSignature = modelIdToSignatureMap.get(rawMatch.getModelId());
                    if (currentSignature == null) {
                        throw new IllegalStateException("Cannot find model " + rawMatch.getModelId() + " in the database.");
                    }
                    match = new SuperFamilyHmmer3Match(
                            currentSignature,
                            rawMatch.getEvalue(),
                            null);
                    splitGroupToMatch.put(rawMatch.getSplitGroup(), match);
                }
                match.addLocation(new SuperFamilyHmmer3Match.SuperFamilyHmmer3Location(
                        rawMatch.getLocationStart(),
                        rawMatch.getLocationEnd()
                ));
                matchCount++;
//                if ((matchCount == 1) || (matchCount % sfBatchSize == 0)) {
//                    Utilities.verboseLog("SuperFamilyHmmer3FilteredMatchDAO: dealt with  " + matchCount + " matches");
//                }
            }

            for (SuperFamilyHmmer3Match match : splitGroupToMatch.values()) {
                //LOGGER.debug("superfamily match: " + match);
                //LOGGER.debug("Protein with match: " + protein);
                Utilities.verboseLog("SuperFamilyHmmer3FilteredMatchDAO:" + "superfamily match: " + match.getLocations() + " \nProtein with match: " + protein.getId());
                protein.addMatch(match);
                entityManager.persist(match);
            }
//            if ((proteinCount == 1) || (proteinCount % 500 == 0)) {
//                Utilities.verboseLog("SuperFamilyHmmer3FilteredMatchDAO: persisted " + proteinCount + " matches/raw proteins");
//            }
        }
    }
}
