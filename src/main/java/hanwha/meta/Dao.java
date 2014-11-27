package hanwha.meta;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
class Dao {
	private static final String QUERY=
			"SELECT DISTINCT\n"+
	"			PRDT_COD 펀드코드\n"+				
	"			, CASE INV_RSK_GRAD_SE_NM\n"+
	"			WHEN	'초고위' THEN 0\n"+
	"			WHEN	'고위험' THEN 1\n"+
	"			WHEN	'중위험' THEN 2\n"+
	"			WHEN	'저위험' THEN 3\n"+
	"                   ELSE 4 END 키인덱스\n"+
	"			, INV_RSK_GRAD_SE_NM 키값항목명\n"+
	"			, RPCH_RQS_SCD_NM	  조건항목명\n"+
	"			, KSD_FND_STD_PRDT_COD	비교연산자\n"+
	"			, PRDT_COD	  조건값\n"+
	"			FROM AM_FND_PRDT_LST\n"+
	" ORDER BY 펀드코드, 키인덱스,키값항목명,조건항목명,비교연산자,조건값\n";
	
	

	static List<Row> getRowList() throws Exception {
		try (Closeable c = new AnnotationConfigApplicationContext(Config.class)) {
			return jdbcTemplate.query(QUERY, new RowMapper<Row>() {
				@Override
				public Row mapRow(ResultSet rs, int rowNum) throws SQLException {
					Row row    = new Row();
					row.imCd   = rs.getInt("펀드코드");
					row.data   = new Data(rs.getInt("키인덱스"),
					                      rs.getString("키값항목명"),
					                      rs.getString("조건항목명"),
					                      rs.getString("비교연산자"),
					                      rs.getInt("조건값"));
					return row;
				}
			});
		}
	}

	@Autowired
	private void setJdbc(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	private static JdbcTemplate jdbcTemplate;
}
