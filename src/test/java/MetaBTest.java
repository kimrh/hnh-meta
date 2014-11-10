import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import hanwha.meta.Meta;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MetaBTest {
	@Parameters
	public static Collection<Object[]> cases() {
		return Arrays.asList(new Object[][] {
{"3 LA00369 CLA00223 20130501 보험기간=15 납입기간=5 만기구분코드=02 납입주기코드=00",
 "* * * 15 5 * * * * * * 02 0 * * 0 00 0"},
{"3 LA00369 CLA00223 20140301 보험기간=15 납입기간=5 만기구분코드=02 납입주기코드=01",
 "* * * 15 5 * * * * * * 02 0 * * 0 * 0"},
{"1 LA00566 CLA00029 20120708 종구분코드=002 성별코드=01 보험기간=10 납입기간=5 만기구분코드=02 연령=35 납입주기코드=03",
 "002 01 * 10 5 * * * * * * 02 0 * * 35 03 0"},
{"1 LA00566 CLA00029 20120708 종구분코드=001 성별코드=01 보험기간=10 납입기간=5 만기구분코드=02 연령=35 납입주기코드=03",
 "001 01 * 10 5 * * * * * * 02 0 * * 0 03 0"}
		});
	}

	private String 문제, 답;

	public MetaBTest(String 문제, String 답) {
		this.문제 = 문제;  // 테이블구분코드   종목코드  담보코드  적용일자  [계약사항]... 
		this.답 = 답;
	}

	@BeforeClass
	public static void setup() throws Exception {
		Class.forName(Meta.class.getName());
	}

	@Test
	public void test() {
		String[] a = 문제.split("(\\s|,|=)+");
		if (a.length < 4 || a.length % 2 != 0) {
			fail("데이터가 부족하거나 계약 사항에 잘못이 있습니다.");
		}
		Map<String, Object> map = new HashMap<>();  // 계약사항
		for (int i = 4; i < a.length; i += 2) {
			map.put(a[i], a[i + 1]);
		}
		Object[] keys = Meta.getKeys(Integer.parseInt(a[0]), a[1], a[2], 
		                             Integer.parseInt(a[3]), map);
		assertEquals(답, StringUtils.join(keys, " "));
	}
}
