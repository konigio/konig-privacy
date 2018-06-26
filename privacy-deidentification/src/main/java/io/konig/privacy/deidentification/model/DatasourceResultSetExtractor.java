/**
 * This class is used to extract the result into Custom Datasource object
 */
package io.konig.privacy.deidentification.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.ResultSetExtractor;


public class DatasourceResultSetExtractor implements ResultSetExtractor<Datasource> {
	List<LanguageString> nameList=new ArrayList<LanguageString>();
	List<LanguageString> descriptionList=new ArrayList<LanguageString>();
	
	@Override
	  public Datasource extractData(ResultSet rs) throws SQLException {
		Datasource datasource = new Datasource();
		while(rs.next()){
			datasource.setUuid(rs.getString("UID"));
			datasource.setId(rs.getString("ID"));
			LanguageString language= new LanguageString();
			language.setLanguage(rs.getString("Language"));
			language.setValue(rs.getString("value"));
			nameList.add(language);
			datasource.setName(nameList);
			LanguageString description= new LanguageString();
			description.setLanguage(rs.getString("Description_Language"));
			description.setValue(rs.getString("Description_Value"));
			descriptionList.add(description);
			datasource.setDescription(descriptionList);
			datasource.setTrustLevel(rs.getDouble("TRUST_LEVEL"));
		}
		return datasource;
	  }

}
