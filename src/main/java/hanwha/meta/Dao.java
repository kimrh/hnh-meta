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
	private static final String QUERY =
"SELECT DISTINCT\n"+
"       CASE SUBSTR(A.INS_IMCD,1,2) WHEN 'IA' THEN '0' ELSE '1' END ||\n"+
"       SUBSTR(A.INS_IMCD,3) 종목코드\n"+
"     , SUBSTR(A.CVRCD,4)    담보코드\n"+
"     , A.LTPRM_BA_TABL_FLGCD||TO_CHAR(A.AP_NDDT,'YYYYMMDD') 테이블구분_종료일\n"+
"     , CASE A.BA_TABL_IDNFR_CLMNM\n"+
"       WHEN           '종구분코드' THEN 0\n"+
"       WHEN             '성별코드' THEN 1\n"+
"       WHEN         '보상한도코드' THEN 2\n"+
"       WHEN             '보험기간' THEN 3  -- Number\n"+
"       WHEN             '납입기간' THEN 4  -- Number\n"+
"       WHEN         '재물구분코드' THEN 5\n"+
"       WHEN         '운전형태코드' THEN 6\n"+
"       WHEN     '신규갱신구분코드' THEN 7\n"+
"       WHEN         '가입구분코드' THEN 8\n"+
"       WHEN             '업종코드' THEN 9\n"+
"       WHEN             '급수코드' THEN 10\n"+
"       WHEN         '만기구분코드' THEN 11\n"+
"       WHEN               '세만기' THEN 12 -- Number\n"+
"       WHEN         '요율구분코드' THEN 13\n"+
"       WHEN '피보험자관계구분코드' THEN 14\n"+
"       WHEN             '가입연령' THEN 15 -- Number\n"+
"       WHEN         '납입주기코드' THEN 16\n"+
"       WHEN             '경과년수' THEN 17 -- Number\n"+
"                                   ELSE 18 END 키인덱스\n"+
"     , A.IDNFR_ADM_ITNM                        키값항목명\n"+
"     , B.CND_IT_IDNFR_CLMNM                    조건항목명\n"+
"     , B.ADMIT_AP_CND_FLGCD                    비교연산자\n"+
"     , CASE B.CND_IT_CNDVL WHEN '*' THEN '0'\n"+
"       ELSE B.CND_IT_CNDVL END                 조건값\n"+
"  FROM IGD_LTPRM_BA_ATR_META A LEFT JOIN\n"+
"       IGD_ADM_IT_AP_CND B\n"+
"    ON A.LTPRM_BA_TABL_FLGCD = B.LTPRM_BA_TABL_FLGCD -- 테이블구분코드\n"+
"   AND A.INS_IMCD            = B.INS_IMCD            -- 보험종목코드\n"+
"   AND A.CVRCD               = B.CVRCD               -- 담보코드\n"+
"   AND A.BA_TABL_IDNFR_CLMNM = B.ST_IT_IDNFR_CLMNM   -- 키인덱스\n"+
"   AND A.AP_NDDT             = B.AP_NDDT             -- 적용종료일자\n"+
"   AND A.AP_STRDT            = B.AP_STRDT            -- 적용시작일자\n"+
" WHERE A.LTPRM_BA_TABL_FLGCD BETWEEN '01' AND '03'\n"+
"   AND A.BA_TABL_IDNFR_CLMNM <> '키없음'\n"+
"   AND A.BA_TABL_IDNFR_CLMNM <> '경과기간'\n"+
"   AND A.BA_TABL_IDNFR_CLMNM NOT LIKE '% '\n"+
"   AND (B.ADMIT_AP_CND_FLGCD is null OR\n"+
"        B.ADMIT_AP_CND_FLGCD BETWEEN '01' AND '06')\n"+
" ORDER BY 종목코드,담보코드,테이블구분_종료일, 키인덱스,키값항목명,조건항목명,비교연산자,조건값\n";

	static List<Row> getRowList() throws Exception {
		try (Closeable c = new AnnotationConfigApplicationContext(Config.class)) {
			return jdbcTemplate.query(QUERY, new RowMapper<Row>() {
				@Override
				public Row mapRow(ResultSet rs, int rowNum) throws SQLException {
					Row row    = new Row();
					row.imCd   = rs.getInt("종목코드");
					row.cvrCd  = rs.getInt("담보코드");
					row.cdNddt = rs.getInt("테이블구분_종료일");
					row.data   = new Data(rs.getInt("키인덱스"),
					                      rs.getString("키값항목명"),
					                      rs.getString("조건항목명"),
					                      rs.getInt("비교연산자"),
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
