package io.konig.privacy.deidentification.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class DatasourceDataRowMapper implements RowMapper<DatasourceData>{
	DatasourceData datasourceData = new DatasourceData();
	@Override
	public DatasourceData mapRow(ResultSet row, int rowNum) throws SQLException {
		datasourceData.setId(row.getString("ID"));
		datasourceData.setTrustLevel(row.getString("TRUST_LEVEL"));			
		return datasourceData;
	}

}
