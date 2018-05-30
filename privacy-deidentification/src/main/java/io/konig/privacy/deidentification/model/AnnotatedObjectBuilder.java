package io.konig.privacy.deidentification.model;

import com.fasterxml.jackson.databind.JsonNode;

public class AnnotatedObjectBuilder {

	/**
	 * Given a simple JSON document (without provenance annotations) and
	 * Provenance information for that record, return an AnnotatedObject that
	 * represents the same information.
	 * <p>
	 * For example, suppose the method is given the following provenance
	 * information...
	 * </p>
	 * <pre>
	 * {
	 *   "receivedFrom" : "http://example.com/system/mdm",
	 *   "receivedAtTime" : "2018-05-29T18:27:58.826Z"
	 * }
	 * </pre>
	 * 
	 * And it is given the following simpleObject...
	 * 
	 * <pre>
	 * {
	 *   "givenName" : "Alice",
	 *   "email" : ["alice@example.com"]
	 * }
	 * </pre>
	 * 
	 * Then this method would return an AnnotatedObject that has the following
	 * JSON representation...
	 * 
	 * <pre>
	 * {
	 *   "fields" : [{
	 *     "name" : "givenName",
	 *     "value" : "Alice",
	 *     "receivedFrom" : "http://example.com/system/mdm",
	 *     "receivedAtTime" : "2018-05-29T18:27:58.826Z"
	 *   }, {
	 *     "name" : "email",
	 *    "value" : [{
	 *      "value" : "alice@example.com",
	 *       "receivedFrom" : "http://example.com/system/mdm",
	 *       "receivedAtTime" : "2018-05-29T18:27:58.826Z"
	 *     }]
	 *   }
	 * }
	 * </pre>
	 * 
	 * In other words, it distributes the provenance to each field in the simpleObject to construct the annotated object.
	 * 
	 * @param provenance
	 * @param simpleObject
	 * @return
	 */
	public AnnotatedObject toAnnotatedObject(Provenance provenance, JsonNode simpleObject) {
		// TODO: implement this method
		return null;
	}

}
