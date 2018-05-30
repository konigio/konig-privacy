package io.konig.privacy.deidentification.model;

/**
 * Metadata about some data record.
 * For instance, Metadata is available from {@link PersonWithMetadata}.
 * @author Greg McFall
 *
 */
public class Metadata {
	
	private DataModel dataModel;
	private Provenance provenance;
	
	/**
	 * Get information about the data model that defines the structure of the record.
	 * @return The data model for the record.
	 */
	public DataModel getDataModel() {
		return dataModel;
	}
	
	/**
	 * Set information about the data model that defines the structure of the record.
	 * @param dataModel The data model for the record.
	 */
	public void setDataModel(DataModel dataModel) {
		this.dataModel = dataModel;
	}
	
	/**
	 * Get provenance information about the record.
	 * @return Provenance information about the record.
	 */
	public Provenance getProvenance() {
		return provenance;
	}
	
	/**
	 * Set provenance information about the record.
	 * @return Provenance information about the record.
	 */
	public void setProvenance(Provenance provenance) {
		this.provenance = provenance;
	}
	

}
