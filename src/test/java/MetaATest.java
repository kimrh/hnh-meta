import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;
import hanwha.meta.Meta;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MetaATest {
	@Parameters
	public static Collection<Object[]> cases() {
		return Arrays.asList(new Object[][] {
{3, "LA00369", "CLA00223", 20130501, "보험기간=15 납입기간=5 만기구분코드=02 납입주기코드=00",
 new Object[] {"*", "*", "*", 15, 5, "*", "*", "*", "*", "*", "*", "02", 0, "*", "*", 0, "00", 0}},
{3, "LA00369", "CLA00223", 20140301, "보험기간=15 납입기간=5 만기구분코드=02 납입주기코드=01",
 new Object[] {"*", "*", "*", 15, 5, "*", "*", "*", "*", "*", "*", "02", 0, "*", "*", 0, "*", 0}},
{1, "LA00566", "CLA00029", 20120708, "종구분코드=002 성별코드=01 보험기간=10 납입기간=5 만기구분코드=02 연령=35 납입주기코드=03",
 new Object[] {"002", "01", "*", 10, 5, "*", "*", "*", "*", "*", "*", "02", 0, "*", "*", 35, "03", 0}},
{1, "LA00566", "CLA00029", 20120708, "종구분코드=001 성별코드=01 보험기간=10 납입기간=5 만기구분코드=02 연령=35 납입주기코드=03",
 new Object[] {"001", "01", "*", 10, 5, "*", "*", "*", "*", "*", "*", "02", 0, "*", "*", 0, "03", 0}}
		});
	}

	private int      테이블구분코드;
	private String   종목코드;
	private String   담보코드;
	private int      적용일자;
	private String   계약정보;
	private Object[] 예상_키;

	public MetaATest(int 테이블구분코드, String 종목코드, String 담보코드,
			         int 적용일자, String 계약정보, Object[] 예상_키) {
		this.종목코드 = 종목코드;
		this.담보코드 = 담보코드;
		this.테이블구분코드 = 테이블구분코드;
		this.적용일자 = 적용일자;
		this.계약정보 = 계약정보;
		this.예상_키 = 예상_키;
	}

	@BeforeClass
	public static void setup() throws Exception {
		Class.forName(Meta.class.getName());
	}

	@Test
	public void test() {
		Map<String, Object> map = new HashMap<>();  // 계약사항
		String[] a = 계약정보.split("(\\s|=|,)+");
		if (a.length % 2 != 0) {
			fail("계약 정보에 잘못이 있습니다.");
		}
		for (int i = 0; i < a.length; i += 2) {
			map.put(a[i], a[i + 1]);
		}
		assertArrayEquals(예상_키, Meta.getKeys(테이블구분코드, 종목코드, 담보코드, 적용일자, map));
	}
}
