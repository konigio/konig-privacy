package io.konig.privacy.deidentification.repo;

/**
 * An interface for accessing the trust level for a DataSource
 * @author Greg McFall
 *
 */
public interface DatasourceTrustService {

	public static final ThreadLocal<DatasourceTrustService> instance = new ThreadLocal<>();
	double getTrustLevel(String datasourceId);
}
