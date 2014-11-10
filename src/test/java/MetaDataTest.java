import hanwha.meta.Meta;

import org.junit.BeforeClass;
import org.junit.Test;

public class MetaDataTest {
	@BeforeClass
	public static void setup() throws Exception {
		Class.forName(Meta.class.getName());
	}

//	@Ignore
	@Test
	public void test() throws Exception {
		Meta.testOrPrint(0, 0, null);         // 테스트
		Meta.testOrPrint(30, 1, null);        // 31번째 종목부터 1종목 콘솔에 출력
		Meta.testOrPrint(30, 2, "rows.txt");  // 31번째 종목부터 2종목 파일에 출력
	}
}
