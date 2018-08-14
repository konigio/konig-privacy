package io.konig.privacy.deidentification.repo;

/**
 * An interface for accessing the trust level for a DataSource
 * @author Greg McFall
 *
 */
public interface DatasourceTrustService {

	double getTrustLevel(String datasourceId);
}
