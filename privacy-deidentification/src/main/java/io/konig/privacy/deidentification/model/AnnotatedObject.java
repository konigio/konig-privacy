package io.konig.privacy.deidentification.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A structure that holds provenance information about an object at the field level.
 * @author Greg McFall
 *
 */
public class AnnotatedObject implements AnnotatedEntity {

	private List<AnnotatedProperty> fields = new ArrayList<>();
	
	public AnnotatedProperty getProperty(String propertyName) {
		for (AnnotatedProperty p : fields) {
			if (propertyName.equals(p.getName())) {
				return p;
			}
		}
		return null;
	}
	
	public void add(AnnotatedProperty property) {
		fields.add(property);
	}
	
}
