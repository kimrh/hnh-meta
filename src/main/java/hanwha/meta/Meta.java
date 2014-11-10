package hanwha.meta;

import static hanwha.util.Extend.extend;
import static org.junit.Assert.assertEquals;
import hanwha.util.IntList;
import hanwha.util.IntSet;
import hanwha.util.PairSet;
import hanwha.util.Set;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Meta {
	private static boolean     refreshing = false;
	private static AtomicInteger useCount = new AtomicInteger();

	static {
		try {
			refresh();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static   int[]    intSet; // 구분-종료일 집합
	private static  Data[]   dataSet; // 데이터 집합
	private static short[][] pairSet; // 정수 짝 집합
	private static short[]  dataList; // 데이터 열
	private static short[] data1List; // 담보 데이터 열
	private static short[] data2List; // 종목 데이터 열
	private static short[]   cvrList; // 담보 코드 인덱스 열
	private static short[][]     cvr; // 담보(코드 인덱스 열 시작, 끝, 데이터 열 시작) 열
	private static   int[]        im; // 종목 코드 열

	/**
	 * 보험료/준비금/사업비 찾기 키 구성 정보를 갱신한다.
	 * @throws Exception 테이블 읽기 오류, 데이터 배열 원소 크기 오류.
	 */
	public static synchronized void refresh() throws Exception {
		IntSet     intSet0 = new  IntSet(3000);
		Set<Data> dataSet0 = new   Set<>(300);
		PairSet   pairSet0 = new PairSet(10000);
		IntList  dataList0 = new IntList(10000, 2000);
		IntList data1List0 = new IntList(10000, 3000);
		IntList data2List0 = new IntList(40000, 2000);
		IntList   cvrList0 = new IntList(30000, 1000);
		ImCvr       imCvr0 = new   ImCvr(3000);

		time(null);
		List<Row> rows = Dao.getRowList();    // 키 구성 정보를 모두 읽는다
		time("Read");

		int rowCount = rows.size();           // 키 구성 정보 수
		rows.add(new Row());                  // 키 구성 정보의 끝을 표시한다

		for (int i = 0; i < rowCount; i++) {
			Row thisRow = rows.get(i);
			Row nextRow = rows.get(i + 1);
			dataList0.append(dataSet0.find(thisRow.data));     // 데이터

			if (!thisRow.sameCdNddt(nextRow)) {
				int cdNddt = intSet0.find(thisRow.cdNddt);     // 구분-종료일
				int   data = pairSet0.find(dataList0.find());  // 데이터 리스트
				data1List0.append(pairSet0.find(cdNddt, data));

				if (!thisRow.sameCvrcd(nextRow)) {
					cvrList0.append(intSet0.find(thisRow.cvrCd));  // 담보 코드
					data2List0.append(pairSet0.find(data1List0.find()));

					if (!thisRow.sameImcd(nextRow)) {
						imCvr0.append(thisRow.imCd,        // 종목 코드
						              cvrList0.find(),     // 담보 코드 리스트 
						              data2List0.find());  // 담보  데이터 리스트
					}
				}
			}
		}
		time("Index");
		
		refreshing = true;
		while (0 < useCount.get()) {
			Thread.sleep(1);
		}
		intSet     =    intSet0.copy();
		dataSet    =   dataSet0.copy(); 
		pairSet    =   pairSet0.copyToShorts();
		dataList   =  dataList0.copyToShorts();
		data1List  = data1List0.copyToShorts();
		data2List  = data2List0.copyToShorts();
		cvrList    =   cvrList0.copyToShorts();
		cvr        =     imCvr0.copyCvr();
		im         =     imCvr0.copyIm();
		refreshing = false;
		time("Refresh");
		
		int totalSize = 100 * dataSet.length + 4 * (im.length + intSet.length) +
		          2 * (data2List.length +  data1List.length + dataList.length +
		          2 * pairSet[0].length + 3 * cvr[0].length +  cvrList.length);

		System.out.format("\n(%d rows --> %d KBytes)\n" +
		           "im[%d] cvr[3][%d] cvrList[%d](%d)\n" + 
		           "intSet[%d] pairSet[2][%d] dataSet[%d]\n" +
		           "dataList[%d](%d) data1List[%d](%d) data2List[%d](%d)\n",
		                   rowCount, (totalSize + 1023) / 1024, 
		   im.length, cvr[0].length, cvrList.length, cvrList0.getIndexSize(),
		              intSet.length, pairSet[0].length, dataSet.length,
		            dataList.length,  dataList0.getIndexSize(),
		           data1List.length, data1List0.getIndexSize(),
		           data2List.length, data2List0.getIndexSize());
	}

	/**
	 * 경과 시간을 출력한다.
	 * @param title 제목. null이면 경과 시간 측정 시작.
	 */
	private static void time(String title) {
		nano = System.nanoTime();
		if (title != null) {
			System.out.format("%s(%.3f초) ", title, (nano - nano0)/1000000000.);
		}
		nano0 = nano;
	}

	private static long nano0, nano;  // 나노 초 단위 경과 구간: 시작, 끝

	/**
	 * 종목 데이터 열 -- (종목 코드, 담보 코드 인덱스 열, 담보 데이터 열)의 열
	 */
	private static class ImCvr {
		private int[]      im;  // 종목 코드 배열
		private short[][] cvr;  // 담보(코드 인덱스 열 시작, 끝, 데이터 열 시작) 배열
		private int      size;  // 종목 수

		/**
		 * @param initialCapacity 처음 배열 용량(= 종목 수 최대값)
		 */
		ImCvr(int initialCapacity) {
			im  = new int[initialCapacity];
			cvr = new short[3][initialCapacity];
		}

		/**
		 * 테이블에 한 종목의 데이터를 넣는다
		 * @param imCd    종목 코드
		 * @param cvrCds  담보 코드 인덱스 배열의 {시작 인덱스, 끝 인덱스}
		 * @param cvrData 담보 데이터 배열의 {시작 인덱스, 끝 인덱스}
		 */
		void append(int imCd, int[] cvrCds, int[] cvrData) throws Exception {
			int a = cvrCds[0], b = cvrCds[1], c = cvrData[0];
			if (a < Short.MIN_VALUE || Short.MAX_VALUE < a ||
			    b < Short.MIN_VALUE || Short.MAX_VALUE < b ||
			    c < Short.MIN_VALUE || Short.MAX_VALUE < c) {
				throw new Exception("short형으로 받을 수 없습니다.");
			}
			if (im.length <= size) {
				im  = Arrays.copyOf(im, extend(size));
				cvr = copyCvr(extend(size));
			}
			im[size]     = imCd;       // 종목 코드
			cvr[0][size] = (short) a;  // 담보 코드 인덱스 배열 시작 인덱스
			cvr[1][size] = (short) b;  // 담보 코드 인덱스 배열 끝 인덱스
			cvr[2][size] = (short) c;  // 담보 데이터 배열 시작 인덱스
			size++;
		}

		int[] copyIm() {
			return Arrays.copyOf(im, size);
		}

		short[][] copyCvr() {
			return copyCvr(size);
		}

		private short[][] copyCvr(int newSize) {
			return new short[][] { 
				Arrays.copyOf(cvr[0], newSize),
				Arrays.copyOf(cvr[1], newSize),
				Arrays.copyOf(cvr[2], newSize)
			};
		}
	}

	private static final int       D5 = 100000, D8 = 100000000;

	private static final String[]  PREFIX = {"IA", "LA"};

	/**
	 * 보험료/준비금/사업비 찾기 키 배열을 만든다.
	 *
	 * @param 테이블구분코드  1=보험료, 2=준비금, 3=사업비
	 * @param 종목코드           종목코드 글자열
	 * @param 담보코드           담보코드 글자열
	 * @param 적용일자           8자리 정수 (년 4자리, 월 2자리, 일 2자리)
	 * @param 계약정보           Map(계약정보 이름, 계약정보 값)
	 * @return 18개 키 배열
	 */
	public static Object[] getKeys(int 테이블구분코드,
	                               String 종목코드, String 담보코드,
	                               int 적용일자, Map<String, Object> 계약정보) {

		int imCd = Integer.parseInt(종목코드.substring(2));   // 종목 코드
		if (종목코드.charAt(0) == 'L') {
			imCd += D5;    // "LA"로 시작하는 종목 코드
		}
		int cvrCd = Integer.parseInt(담보코드.substring(3));  // 담보 코드

		Object[] keys = Data.DEFAULT_KEYS.clone();

		useCount.incrementAndGet();
		if (refreshing) {
			useCount.decrementAndGet();
			while (refreshing) {
				try { Thread.sleep(1); } catch (Exception e) {}
			}
			useCount.incrementAndGet();
		}

		do {
			int m = Arrays.binarySearch(im, imCd);  // 종목 인덱스
			if (m < 0) break;  // 종목 코드가 없다

			int c = IntSet.binarySearch(intSet, cvrList,
			                            cvr[0][m], cvr[1][m], cvrCd);
			if (c < 0) break;  // 종목에 담보 코드가 없다

			int data2 = data2List[cvr[2][m] + (c - cvr[0][m])];  // 담보 데이터
			int i     = pairSet[0][data2];         // 담보 데이터 시작
			int iEnd  = pairSet[1][data2];         // 담보 데이터 끝
			while (i < iEnd) {
				int  data1 = data1List[i++];       // (구분-종료일, 데이터) 짝
				int cdNddt = intSet[pairSet[0][data1]];  // 구분-종료일
				int     cd = cdNddt / D8;                // 구분
				if (테이블구분코드 == cd && 적용일자 <= cdNddt % D8) {
					int data = pairSet[1][data1];  // 데이터
					int j    = pairSet[0][data];   // 데이터 시작
					int jEnd = pairSet[1][data];   // 데이터 끝
					while (j < jEnd) {
						dataSet[dataList[j++]].getKey(keys, 계약정보);
					}
					break;
				}
				if (테이블구분코드 < cd) break;
			}
		} while (false);

		useCount.decrementAndGet();
		return keys;
	}

	/**
	 * 테이블에서 읽은 행과 맞추어 데이터를 검사하거나, 데이터를 출력한다.
	 *
	 * @param from  출력 시작 종목 인덱스  -- 0, 1, 2, ...
	 * @param count 출력 종목 수 -- 0이면 데이터를 모두 검사한다.
	 * @param file  출력 파일 이름 -- null이거나 잘못된 이름이면 표준 출력으로 출력한다.
	 * @throws Exception 데이터베이스 테이블 읽기 오류. 
	 */
	public static void testOrPrint(int from, int count, String file)
		                    throws Exception {
		boolean testing = count == 0;
		int          to = Math.min(from + count, im.length);
		PrintStream out = System.out;
		List<Row>  rows = null;

		if (testing) {
			from = 0;                 // 처음부터
			to   = im.length;         // 끝까지
			rows = Dao.getRowList();  // 테이블에서 읽은 데이터 행 리스트
		} else {
			if (file != null) {
				try {
					out = new PrintStream(file);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		int r = 0;                                  // 행 인덱스
		for (int m = from; m < to; m++) {           // 종목 인덱스
			for (int c0 = cvr[0][m], c = c0; c < cvr[1][m]; c++) { // 담보 인덱스
				int data2 = data2List[cvr[2][m] + (c - c0)];       // 담보 데이터
				int i     = pairSet[0][data2];      // 담보 데이터 시작
				int iEnd  = pairSet[1][data2];      // 담보 데이터 끝
				while (i < iEnd) {
					int  data1 = data1List[i++];    // (구분-종료일, 데이터) 짝
					int cdNddt = intSet[pairSet[0][data1]];  // 구분-종료일
					int   data =        pairSet[1][data1];   // 데이터
					int j      = pairSet[0][data];           // 데이터 시작
					int jEnd   = pairSet[1][data];           // 데이터 끝 
					while (j < jEnd) {
						if (testing) {
							String msg = "행 인덱스 = " + r;
							Row    row = rows.get(r++);
							assertEquals(msg, row.imCd, im[m]);
							assertEquals(msg, row.cvrCd, intSet[cvrList[c]]);
							assertEquals(msg, row.cdNddt, cdNddt);
							assertEquals(msg, row.data, dataSet[dataList[j++]]);
						} else {
							out.format("%s%05d CLA%05d %d %d %s\n",
									PREFIX[im[m] / D5], im[m] % D5, // 종목 코드
									intSet[cvrList[c]],       // 담보 코드
									cdNddt / D8, cdNddt % D8, // 구분, 종료일
									dataSet[dataList[j++]]);  // 데이터
						}
					}
				}
			}
		}

		if (testing) {
			assertEquals("행 수", rows.size(), r);  // 행 수가 맞는지 검사한다
		} else {
			if (out != System.out) {
				out.close();
			}
		}
	}

	private Meta() {}
}
